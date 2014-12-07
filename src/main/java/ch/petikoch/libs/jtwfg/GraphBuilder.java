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

/**
 * A builder class for graph creation. Can be used concurrently by different threads which create together the graph.
 * <p/>
 * Thread-safe.
 *
 * @param <T>
 * 		The type of the ID of the tasks in the graph. Something with a meaningful {@link Object#equals(Object)} and {@link
 * 		Object#hashCode()} implementation like {@link String}, {@link Long} or a class of your domain model which is fine
 * 		to use as a key e.g. in a {@link java.util.HashMap}
 */
public class GraphBuilder<T> {

	private final Map<T, Task<T>> taskMap = new LinkedHashMap<>();

	private final Object internalLock = new Object();

	/**
	 * Adds a task in the graph, if not yet present.
	 *
	 * @param task
	 * 		not null
	 *
	 * @return the GraphBuilder instance itself
	 */
	public GraphBuilder<T> addTask(T task) {
		synchronized (internalLock) {
			getOrAddTaskRepresentator(task);
			return this;
		}
	}

	/**
	 * Adds a couple of tasks in the graph, if not yet present.
	 *
	 * @param tasks
	 * 		not null, may be empty
	 *
	 * @return the GraphBuilder instance itself
	 */
	public GraphBuilder<T> addTasks(Iterable<T> tasks) {
		synchronized (internalLock) {
			for (T task : tasks) {
				getOrAddTaskRepresentator(task);
			}
			return this;
		}
	}

	/**
	 * Adds an edge between two tasks in the graph, if not yet present.
	 *
	 * @param task1
	 * 		not null
	 * @param task2
	 * 		not null
	 *
	 * @return the GraphBuilder instance itself
	 */
	public GraphBuilder<T> addTaskWaitFor(T task1, T task2) {
		synchronized (internalLock) {
			Task<T> task1Representator = getOrAddTaskRepresentator(task1);
			Task<T> task2Representator = getOrAddTaskRepresentator(task2);
			task1Representator.addWaitFor(task2Representator);
			return this;
		}
	}

	/**
	 * Creates a graph instance. It creates an unmodifiable "snapshot" of the current graph situation. The
	 * graph-snapshot wont change, even if you continue to populate the graph with the same builder instance with the
	 * current or another thread.
	 * <p/>
	 * Can be called as many times you want to create as many graph snapshots you want.
	 *
	 * @return Graph
	 */
	public Graph<T> build() {
		synchronized (internalLock) {
			final Collection<Task<T>> tasks = taskMap.values();
			final LinkedHashSet<Task<T>> tasksSnapshotCopy = copy(tasks);
			return new Graph<>(tasksSnapshotCopy);
		}
	}

	private Task<T> getOrAddTaskRepresentator(T task) {
		Preconditions.checkNotNull(task, "task must not be null");

		Task<T> taskRepresentator = taskMap.get(task);
		if (taskRepresentator == null) {
			taskRepresentator = new Task<>(task);
			taskMap.put(task, taskRepresentator);
		}

		return taskRepresentator;
	}

	private static <T> LinkedHashSet<Task<T>> copy(final Collection<Task<T>> originalTasks) {
		Map<Task<T>, Task<T>> result = new LinkedHashMap<>(originalTasks.size());
		recursiveCopy(originalTasks, result);
		return new LinkedHashSet<>(result.values());
	}

	private static <T> void recursiveCopy(Collection<Task<T>> originalTasks, Map<Task<T>, Task<T>> result) {
		for (Task<T> originalTask : originalTasks) {
			Task<T> copiedTask = result.get(originalTask);
			if (null == copiedTask) {
				copiedTask = new Task<>(originalTask.getId());
				result.put(originalTask, copiedTask);
				recursiveCopy(originalTask.getWaitForTasks(), result);
				for (Task<T> originalWaitForTask : originalTask.getWaitForTasks()) {
					copiedTask.addWaitFor(result.get(originalWaitForTask));
				}
			}
		}
	}
}
