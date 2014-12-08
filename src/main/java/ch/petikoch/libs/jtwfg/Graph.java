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

import java.util.Collections;
import java.util.Set;

/**
 * Represents a "task wait for model" graph.
 * <p/>
 * Immutable / thread-safe.
 *
 * @param <T>
 * 		The type of the ID of the tasks. Something with a meaningful {@link Object#equals(Object)} and {@link
 * 		Object#hashCode()} implementation like {@link String}, {@link Long} or a class of your domain model which is fine
 * 		to use as a key e.g. in a {@link java.util.HashMap}. If T implements Comparable, then you get sorted graphs.
 */
public class Graph<T> {

	private final Set<Task<T>> tasks;

	Graph(final Set<Task<T>> tasks) {
		this.tasks = Collections.unmodifiableSet(tasks);
	}

	public Set<Task<T>> getTasks() {
		return tasks;
	}

	// generated by IntelliJ IDEA
	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final Graph graph = (Graph) o;

		if (!tasks.equals(graph.tasks)) return false;

		return true;
	}

	// generated by IntelliJ IDEA
	@Override
	public int hashCode() {
		return tasks.hashCode();
	}

	// generated by IntelliJ IDEA
	@Override
	public String toString() {
		return "Graph{" +
				"tasks=" + tasks +
				'}';
	}
}
