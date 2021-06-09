/**
 * Copyright 2014-2021 Peti Koch und Adrian Elsener
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.petikoch.libs.jtwfg;

import java.util.*;

/**
 * An implementation of an algorithm to look for deadlocks in a "task wait for model" graph. The algorithm looks for
 * circular dependencies between tasks.
 * <p>
 * Immutable / thread-safe.
 *
 * <T> refers to the type of the ID of the tasks. Something with a meaningful {@link Object#equals(Object)} and {@link
 *            Object#hashCode()} implementation like {@link String}, {@link Long} or a class of your domain model which is fine
 *            to use as a key e.g. in a {@link java.util.HashMap}. If T implements Comparable, then you get sorted collections.
 */
public class DeadlockDetector {

    /**
     * Checks very fast whether there is a deadlock involving a particular task.
     * @param task the task
     * @param <T> the type of the ID of the tasks (see class javadoc).
     * @return true if there is a deadlock involving task.
     */
    public static <T> boolean hasDeadlockOn(Task<T> task) {
        //Very simple, since we are not interested in detailed information.
        //If we encounter the same node twice when traversing the graph.
        Stack<Task<T>> workstack = new Stack<>();
        Set<Task<T>> seen = new HashSet<>();
        seen.add(task);
        workstack.push(task);
        while (!workstack.isEmpty()) {
            Task<T> element = workstack.pop();
            for (Task<T> wtask : element.getWaitsForTasks()) {
                if (!seen.add(wtask)) {
                    return true;
                }
                workstack.push(wtask);
            }
        }
        return false;
    }

    public static <T> DeadlockAnalysisResult analyze(final Graph<T> graph) {
        Set<DeadlockCycle<T>> cycleCollector = new LinkedHashSet<>();
        findCycles(graph, cycleCollector);
        Set<DeadlockCycle<T>> cyclesWithAlsoDeadlocked = findAlsoDeadlocked(graph, Collections.unmodifiableSet(cycleCollector));
        return new DeadlockAnalysisResult<>(cyclesWithAlsoDeadlocked);
    }

    private static <T> void findCycles(Graph<T> graph,
                            Set<DeadlockCycle<T>> cycleCollector) {
        for (Task<T> startTask : graph.getTasks()) {
            Set<Task<T>> visitedTasks = new HashSet<>();
            findDeadlocksDepthFirst(startTask, startTask.getWaitsForTasks(), new LinkedList<>(), cycleCollector, visitedTasks);
        }
    }

    private static <T> Set<DeadlockCycle<T>> findAlsoDeadlocked(final Graph<T> graph,
                                                     final Set<DeadlockCycle<T>> deadlockCycles) {
        Set<DeadlockCycle<T>> enrichedDeadlockCycles = deadlockCycles;
        boolean moreDeadlockedFound = true;
        while (moreDeadlockedFound) {
            Set<DeadlockCycle<T>> againEnrichedDeadlockCycles = findSomeMoreDeadlocked(graph, Collections.unmodifiableSet(enrichedDeadlockCycles));
            if (enrichedDeadlockCycles.equals(againEnrichedDeadlockCycles)) {
                moreDeadlockedFound = false;
            } else {
                enrichedDeadlockCycles = againEnrichedDeadlockCycles;
            }
        }
        return enrichedDeadlockCycles;
    }

    private static <T> Set<DeadlockCycle<T>> findSomeMoreDeadlocked(final Graph<T> graph,
                                                         final Set<DeadlockCycle<T>> originalDeadlockCycles) {
        Set<DeadlockCycle<T>> enrichedDeadLockCyclesCollector = new LinkedHashSet<>();
        for (DeadlockCycle<T> originalDeadlockCycle : originalDeadlockCycles) {
            Map<T, Set<T>> enrichedAlsoDeadlocked = new LinkedHashMap<>(originalDeadlockCycle.getAlsoDeadlockedTasks());
            for (Task<T> startTask : graph.getTasks()) {
                for (Task<T> waitsForTask : startTask.getWaitsForTasks()) {
                    if (!originalDeadlockCycle.isDeadlocked(startTask.getId()) && originalDeadlockCycle.isDeadlocked(waitsForTask.getId())) {
                        if (!enrichedAlsoDeadlocked.containsKey(startTask.getId())) {
                            enrichedAlsoDeadlocked.put(startTask.getId(), new LinkedHashSet<>());
                        }
                        Collection<T> values = enrichedAlsoDeadlocked.get(startTask.getId());
                        values.add(waitsForTask.getId());
                    }
                    for (Task<T> otherWaitsForTask : waitsForTask.getWaitsForTasks()) {
                        if (!originalDeadlockCycle.isDeadlocked(waitsForTask.getId()) && originalDeadlockCycle.isDeadlocked(otherWaitsForTask.getId())) {
                            if (!enrichedAlsoDeadlocked.containsKey(waitsForTask.getId())) {
                                enrichedAlsoDeadlocked.put(waitsForTask.getId(), new LinkedHashSet<>());
                            }
                            Collection<T> values = enrichedAlsoDeadlocked.get(waitsForTask.getId());
                            values.add(otherWaitsForTask.getId());
                        }
                    }
                }
            }
            enrichedDeadLockCyclesCollector.add(new DeadlockCycle<>(originalDeadlockCycle.getCycleTasks(), enrichedAlsoDeadlocked));
        }
        return enrichedDeadLockCyclesCollector;
    }

    private static <T> void findDeadlocksDepthFirst(Task<T> startTask,
                                                    Set<Task<T>> waitForTasks,
                                                    List<Task<T>> hops,
                                                    Set<DeadlockCycle<T>> cycleCollector,
                                                    Set<Task<T>> visitedTasks) {
        for (Task<T> otherTask : waitForTasks) {
            List<Task<T>> hopsCopy = new LinkedList<>(hops);
            if (!startTask.equals(otherTask)) { // self-reference
                hopsCopy.add(otherTask);
            }
            for (Task<T> otherOfOtherTask : otherTask.getWaitsForTasks()) {
                if (!visitedTasks.contains(otherOfOtherTask)) {
                    visitedTasks.add(otherOfOtherTask);
                    if (startTask.equals(otherOfOtherTask)) {
                        List<Task<T>> cycleList = new ArrayList<>(hopsCopy.size() + 2);
                        cycleList.add(startTask);
                        cycleList.addAll(hopsCopy);
                        cycleList.add(otherOfOtherTask);
                        final List<T> cycleIdList = convert(cycleList);
                        cycleCollector.add(new DeadlockCycle<>(cycleIdList, null /* is populated afterwards */));
                    } else {
                        findDeadlocksDepthFirst(startTask, otherTask.getWaitsForTasks(), hopsCopy, cycleCollector, visitedTasks);
                    }
                }
            }
        }
    }

    private static <T> List<T> convert(List<Task<T>> tasks) {
        List<T> result = new ArrayList<>(tasks.size());
        for (Task<T> task : tasks) {
            result.add(task.getId());
        }
        return result;
    }
}
