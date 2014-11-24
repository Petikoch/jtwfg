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

package ch.petikoch.jtwfg;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Task {

    private final String id;
    private final Set<Task> waitForTasks = new LinkedHashSet<>();

    public Task(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or blank");
        }
        this.id = id;
    }

    public Set<Task> getWaitForTasks() {
        return Collections.unmodifiableSet(waitForTasks);
    }

    public void addWaitFor(Task task) {
        waitForTasks.add(task);
    }

    public void addWaitForAll(Collection<Task> tasks) {
        waitForTasks.addAll(tasks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        if (!id.equals(task.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                '}';
    }
}
