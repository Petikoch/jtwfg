/*
 * Copyright 2014-2015 Peti Koch und Adrian Elsener
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
 * The representation of a cycle between tasks in a "task wait for model" graph.
 * <p/>
 * Immutable / thread-safe.
 *
 * @param <T>
 * 		The type of the ID of the tasks. Something with a meaningful {@link Object#equals(Object)} and {@link
 * 		Object#hashCode()} implementation like {@link String}, {@link Long} or a class of your domain model which is fine
 * 		to use as a key e.g. in a {@link java.util.HashMap}. If T implements Comparable, then you get sorted collections.
 */
public class DeadlockCycle<T> {

	private final List<T> cycleTasks;
	private final Map<T, Set<T>> alsoDeadlockedTasks;
	private final Set<T> allDeadlockedTasks;

	DeadlockCycle(final List<T> cycleTasks, /* Nullable */ final Map<T, Set<T>> alsoDeadlockedTasks) {
		Preconditions.checkArgument(cycleTasks != null && !cycleTasks.isEmpty(), "There are no cycle tasks: " + cycleTasks);
		this.cycleTasks = Collections.unmodifiableList(cycleTasks);
		if (null != alsoDeadlockedTasks) {
			final LinkedHashMap<T, Set<T>> alsoDeadlockedTasksCopyMap = new LinkedHashMap<>(alsoDeadlockedTasks.size());
			for (Map.Entry<T, Set<T>> mapEntry : alsoDeadlockedTasks.entrySet()) {
				alsoDeadlockedTasksCopyMap.put(mapEntry.getKey(), Collections.unmodifiableSet(mapEntry.getValue()));
			}
			this.alsoDeadlockedTasks = Collections.unmodifiableMap(alsoDeadlockedTasksCopyMap);
		} else {
			this.alsoDeadlockedTasks = Collections.emptyMap();
		}
		Set<T> allInvolved = new LinkedHashSet<>(this.cycleTasks);
		allInvolved.addAll(this.alsoDeadlockedTasks.keySet());
		this.allDeadlockedTasks = Collections.unmodifiableSet(allInvolved);
	}

	/**
	 * @return an unmodifiable list of the tasks which build the cycle
	 */
	@SuppressWarnings("UnusedDeclaration")
	public List<T> getCycleTasks() {
		return cycleTasks;
	}

	/**
	 * @return an unmodifiable map of other tasks outside of the cycle, which are direct or indirect dependent on a task
	 * of the cycle
	 */
	public Map<T, Set<T>> getAlsoDeadlockedTasks() {
		return alsoDeadlockedTasks;
	}

	/**
	 * @return an unmodifiable set of all deadlocked tasks because of this deadlock cycle
	 */
	public Set<T> getAllDeadlockedTasks() {
		return allDeadlockedTasks;
	}

	/**
	 * @param task
	 * 		not null
	 *
	 * @return true if the given task is deadlocked because of this deadlock cycle
	 */
	public boolean isDeadlocked(T task) {
		Preconditions.checkArgumentNotNull(task, "task must not be null");
		return allDeadlockedTasks.contains(task);
	}

	/**
	 * @param tasks
	 * 		not null
	 *
	 * @return true if all the given tasks are deadlocked because of this deadlock cycle
	 */
	public boolean areAllDeadlocked(Iterable<T> tasks) {
		Preconditions.checkArgumentNotNull(tasks, "tasks must not be null");
		for (T task : tasks) {
			if (!isDeadlocked(task)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DeadlockCycle<T> that = (DeadlockCycle<T>) o;

		if (alsoDeadlockedTasks.equals(that.alsoDeadlockedTasks)) {
			if (cycleTasks.size() == that.cycleTasks.size()) {

				Set<T> thisTaskSet = new HashSet<>(cycleTasks);
				Set<T> thatTaskSet = new HashSet<>(that.cycleTasks);
				if (thisTaskSet.equals(thatTaskSet)) {
					// the order is important
					T firstElement = cycleTasks.iterator().next();
					int indexOfFirstElementInThat = calculateIndex(that.cycleTasks, firstElement);
					if (indexOfFirstElementInThat > 0) {
						List<T> reorderedCopyOfThatInvolvedTasks = new ArrayList<>(that.cycleTasks.size());
						reorderedCopyOfThatInvolvedTasks.addAll(that.cycleTasks.subList(indexOfFirstElementInThat, that.cycleTasks.size()));
						if (indexOfFirstElementInThat > 1) {
							reorderedCopyOfThatInvolvedTasks.addAll(that.cycleTasks.subList(1, indexOfFirstElementInThat));
						}
						reorderedCopyOfThatInvolvedTasks.add(that.cycleTasks.get(indexOfFirstElementInThat));
						return cycleTasks.equals(reorderedCopyOfThatInvolvedTasks);
					} else {
						return cycleTasks.equals(that.cycleTasks);
					}
				}
			}
		}

		return false;
	}

	private int calculateIndex(final List<T> tasks, final T element) {
		int index = 0;
		for (T t : tasks) {
			if (t.equals(element)) {
				return index;
			}
			index++;
		}
		throw new IllegalStateException("Element " + element + " not found in " + tasks);
	}

	@Override
	public int hashCode() {
		return allDeadlockedTasks.hashCode();
	}

	@Override
	public String toString() {
		String result = DeadlockCycle.class.getSimpleName();
		result += ": ";
		for (int i = 0; i < cycleTasks.size() - 1; i++) {
			result += cycleTasks.get(i) + " -> ";
		}
		result += cycleTasks.get(cycleTasks.size() - 1);
		if (!alsoDeadlockedTasks.isEmpty()) {
			result += ". The following tasks are also deadlocked, because they are direct or indirect dependent on at least one of the tasks in the deadlock cycle: ";
			for (Map.Entry<T, Set<T>> mapEntry : alsoDeadlockedTasks.entrySet()) {
				for (T waitForTask : mapEntry.getValue()) {
					result += mapEntry.getKey();
					result += "->";
					result += waitForTask;
					result += " ";
				}
			}
			result = result.trim();
			result += ".";
		}
		return result;
	}
}
