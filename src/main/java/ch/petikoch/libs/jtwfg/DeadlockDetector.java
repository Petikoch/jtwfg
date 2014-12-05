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

public class DeadlockDetector<T> {

	private static <T> void findDeadlocksDepthFirst(Task<T> startTask,
	                                                Set<Task<T>> waitForTasks,
	                                                List<Task<T>> hops,
	                                                Set<DeadlockCycle<T>> cycleCollector,
	                                                Set<Task<T>> visitedTasks) {
		for (Task<T> otherTask : waitForTasks) {
			List<Task<T>> hopsCopy = new LinkedList<>(hops);
			hopsCopy.add(otherTask);
			for (Task<T> otherOfOtherTask : otherTask.getWaitForTasks()) {
				if (!visitedTasks.contains(otherOfOtherTask)) {
					visitedTasks.add(otherOfOtherTask);
					if (startTask.equals(otherOfOtherTask)) {
						List<Task<T>> cycleList = new LinkedList<>();
						cycleList.add(startTask);
						cycleList.addAll(hopsCopy);
						Set<T> cycleIdList = convert(cycleList);
						cycleCollector.add(new DeadlockCycle<>(cycleIdList));
					} else {
						List<Task<T>> hopsCopy2 = new LinkedList<>(hopsCopy);
						hopsCopy2.add(otherOfOtherTask);
						findDeadlocksDepthFirst(startTask, otherTask.getWaitForTasks(), hopsCopy2, cycleCollector, visitedTasks);
					}
				}
			}
		}
	}

	private static <T> TreeSet<T> convert(List<Task<T>> tasks) {
		TreeSet<T> result = new TreeSet<>();
		for (Task<T> task : tasks) {
			result.add(task.getId());
		}
		return result;
	}

	public DeadlockAnalysisResult<T> analyze(Graph<T> graph) {
		Set<DeadlockCycle<T>> cycleCollector = new LinkedHashSet<>();
		Set<Task<T>> visitedTasks = new HashSet<>();
		for (Task<T> startTask : graph.getTasks()) {
			findDeadlocksDepthFirst(startTask, startTask.getWaitForTasks(), new LinkedList<Task<T>>(), cycleCollector, visitedTasks);
		}
		return new DeadlockAnalysisResult<>(cycleCollector);
	}
}
