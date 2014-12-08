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

import java.util.*;

/**
 * A builder class for graph instance creation. Can be used concurrently by different threads which create together the
 * graph.
 * <p/>
 * Thread-safe.
 *
 * @param <T>
 * 		The type of the ID of the tasks in the graph. Something with a meaningful {@link Object#equals(Object)} and {@link
 * 		Object#hashCode()} implementation like {@link String}, {@link Long} or a class of your domain model which is fine
 * 		to use as a key e.g. in a {@link java.util.HashMap}. If T implements Comparable, then you get sorted graphs.
 */
public class GraphBuilder<T> {

	private final Map<T, Task<T>> taskMap = new LinkedHashMap<>();

	private final Object internalLock = new Object();

	/**
	 * Adds a task in the graph, if not yet present.
	 *
	 * @param taskId
	 * 		not null
	 *
	 * @return the GraphBuilder instance itself
	 *
	 * @throws java.lang.IllegalArgumentException
	 * 		in case of taskId is null
	 */
	public GraphBuilder<T> addTask(T taskId) {
		synchronized (internalLock) {
			getOrAddTaskRepresentator(taskId);
		}
		return this;
	}

	/**
	 * Checks, it there exists a task with the givenTaskId
	 *
	 * @param taskId
	 * 		not null
	 *
	 * @return boolean
	 *
	 * @throws java.lang.IllegalArgumentException
	 * 		in case of taskId is null
	 */
	public boolean hasTask(T taskId) {
		Preconditions.checkArgumentNotNull(taskId, "taskId must not be null");
		boolean result;
		synchronized (internalLock) {
			result = taskMap.containsKey(taskId);
		}
		return result;
	}

	/**
	 * Removes a task from the graph. Removes also of course all "incoming" "wait for" dependencies from other tasks
	 * referencing this one.
	 *
	 * @param taskId
	 * 		not null
	 *
	 * @return the GraphBuilder instance itself
	 *
	 * @throws java.lang.IllegalArgumentException
	 * 		in case of taskId was not added before
	 */
	public GraphBuilder<T> removeTask(T taskId) {
		Preconditions.checkArgumentNotNull(taskId, "taskId must not be null");
		synchronized (internalLock) {
			Task<T> toRemove = taskMap.get(taskId);
			if (toRemove != null) {
				taskMap.remove(taskId);
				for (Task<T> otherTasks : taskMap.values()) {
					otherTasks.removeWaitFor(toRemove);
				}
			} else {
				throw new IllegalArgumentException("taskId " + taskId + " is unknown and can't be removed");
			}
		}
		return this;
	}

	/**
	 * @param taskIds
	 * 		may be null or empty
	 *
	 * @return the GraphBuilder instance itself
	 *
	 * @throws java.lang.IllegalArgumentException
	 * 		in case of taskId was not added before
	 */
	public GraphBuilder<T> removeTasks(Iterable<T> taskIds) {
		if (null != taskIds) {
			synchronized (internalLock) {
				for (T taskId : taskIds) {
					if (!taskMap.containsKey(taskId)) {
						throw new IllegalArgumentException("taskId " + taskId + " is unknown and can't be removed. None of the given tasks " + taskIds + " were removed");
					}
				}
				for (T taskId : taskIds) {
					removeTask(taskId);
				}
			}
		}
		return this;
	}

	/**
	 * Removes the "wait for" dependency (and only the "wait for", not the tasks itself).
	 *
	 * @param taskId
	 * 		not null
	 * @param waitingOnTaskId
	 * 		not null
	 *
	 * @return the GraphBuilder instance itself
	 */
	public GraphBuilder<T> removeTaskWaitForDependency(T taskId, T waitingOnTaskId) {
		synchronized (internalLock) {
			Task<T> task = taskMap.get(taskId);
			Preconditions.checkArgumentNotNull(task, "taskId " + taskId + " is unknown");
			Task<T> waitingOnTask = taskMap.get(waitingOnTaskId);
			Preconditions.checkArgumentNotNull(waitingOnTask, "taskId " + waitingOnTaskId + " is unknown");
			boolean removed = task.removeWaitFor(waitingOnTask);
			if (!removed) {
				throw new IllegalArgumentException(taskId + " is existing but was not waiting on " + waitingOnTaskId);
			}
		}
		return this;
	}

	/**
	 * Adds a couple of taskIds in the graph, if not yet present.
	 *
	 * @param taskIds
	 * 		not null, may be empty
	 *
	 * @return the GraphBuilder instance itself
	 */
	public GraphBuilder<T> addTasks(Iterable<T> taskIds) {
		synchronized (internalLock) {
			for (T task : taskIds) {
				getOrAddTaskRepresentator(task);
			}
		}
		return this;
	}

	/**
	 * Adds an edge between two tasks in the graph, if not yet present.
	 *
	 * @param taskId
	 * 		not null
	 * @param waitingOnTaskId
	 * 		not null
	 *
	 * @return the GraphBuilder instance itself
	 */
	public GraphBuilder<T> addTaskWaitsFor(T taskId, T waitingOnTaskId) {
		synchronized (internalLock) {
			Task<T> task = getOrAddTaskRepresentator(taskId);
			Task<T> waitingOnTask = getOrAddTaskRepresentator(waitingOnTaskId);
			task.addWaitFor(waitingOnTask);
		}
		return this;
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
		final TreeSet<Task<T>> tasksSnapshotCopy;
		synchronized (internalLock) {
			final Collection<Task<T>> tasks = taskMap.values();
			tasksSnapshotCopy = copy(tasks);
		}
		return new Graph<>(tasksSnapshotCopy);
	}

	private Task<T> getOrAddTaskRepresentator(T taskId) {
		Preconditions.checkArgumentNotNull(taskId, "taskId must not be null");

		Task<T> taskRepresentator = taskMap.get(taskId);
		if (taskRepresentator == null) {
			taskRepresentator = new Task<>(taskId);
			taskMap.put(taskId, taskRepresentator);
		}

		return taskRepresentator;
	}

	private static <T> TreeSet<Task<T>> copy(final Collection<Task<T>> originalTasks) {
		Map<Task<T>, Task<T>> result = new HashMap<>(originalTasks.size());
		recursiveCopy(originalTasks, result);
		return new TreeSet<>(result.values());
	}

	private static <T> void recursiveCopy(Collection<Task<T>> originalTasks, Map<Task<T>, Task<T>> result) {
		for (Task<T> originalTask : originalTasks) {
			Task<T> copiedTask = result.get(originalTask);
			if (null == copiedTask) {
				copiedTask = new Task<>(originalTask.getId());
				result.put(originalTask, copiedTask);
				recursiveCopy(originalTask.getWaitsForTasks(), result);
				for (Task<T> originalWaitForTask : originalTask.getWaitsForTasks()) {
					copiedTask.addWaitFor(result.get(originalWaitForTask));
				}
			}
		}
	}
}
