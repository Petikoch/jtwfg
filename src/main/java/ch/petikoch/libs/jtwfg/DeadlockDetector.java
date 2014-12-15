/*
 * Copyright 2014 Peti Koch und Adrian Elsener
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
 * <p/>
 * Immutable / thread-safe.
 *
 * @param <T>
 * 		The type of the ID of the tasks. Something with a meaningful {@link Object#equals(Object)} and {@link
 * 		Object#hashCode()} implementation like {@link String}, {@link Long} or a class of your domain model which is fine
 * 		to use as a key e.g. in a {@link java.util.HashMap}. If T implements Comparable, then you get sorted collections.
 */
public class DeadlockDetector<T> {

	public DeadlockAnalysisResult<T> analyze(Graph<T> graph) {
		Set<DeadlockCycle<T>> cycleCollector = new LinkedHashSet<>();
		Set<Task<T>> visitedTasks = new HashSet<>();
		for (Task<T> startTask : graph.getTasks()) {
			findDeadlocksDepthFirst(startTask, startTask.getWaitsForTasks(), new LinkedList<Task<T>>(), cycleCollector, visitedTasks);
		}
		return new DeadlockAnalysisResult<>(cycleCollector);
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
						cycleCollector.add(new DeadlockCycle<>(cycleIdList));
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
