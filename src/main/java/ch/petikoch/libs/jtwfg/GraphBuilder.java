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

import ch.petikoch.libs.jtwfg.assertion.Preconditions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class GraphBuilder<T> {

	private final Map<T, Task<T>> taskMap = new LinkedHashMap<>();

	private final Object internalLock = new Object();

	private static <T> LinkedHashSet<Task<T>> copy(final Collection<Task<T>> tasks) {
		LinkedHashSet<Task<T>> result = new LinkedHashSet<>(tasks.size());
		for (Task<T> task : tasks) {
			result.add(task.deepCopy());
		}
		return result;
	}

	public GraphBuilder<T> addTask(T task) {
		synchronized (internalLock) {
			addTaskRepresentator(task);
			return this;
		}
	}

	public GraphBuilder<T> addTasks(Iterable<T> tasks) {
		synchronized (internalLock) {
			for (T task : tasks) {
				addTaskRepresentator(task);
			}
			return this;
		}
	}

	public GraphBuilder<T> addTaskWaitFor(T task1, T task2) {
		synchronized (internalLock) {
			Task<T> task1Representator = addTaskRepresentator(task1);
			Task<T> task2Representator = addTaskRepresentator(task2);
			task1Representator.addWaitFor(task2Representator);
			return this;
		}
	}

	public Graph<T> build() {
		synchronized (internalLock) {
			final Collection<Task<T>> tasks = taskMap.values();
			final LinkedHashSet<Task<T>> tasksSnapshotCopy = copy(tasks);
			return new Graph<>(tasksSnapshotCopy);
		}
	}

	private Task<T> addTaskRepresentator(T task) {
		Preconditions.checkNotNull(task, "task must not be null");

		Task<T> taskRepresentator = taskMap.get(task);
		if (taskRepresentator == null) {
			taskRepresentator = new Task<>(task);
			taskMap.put(task, taskRepresentator);
		}

		return taskRepresentator;
	}
}
