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


import java.util.Collections;
import java.util.List;

public final class DeadlockCycle {

    private final List<Task> cycle;

    public DeadlockCycle(final List<Task> cycle) {
        if (cycle == null || cycle.isEmpty()) {
            throw new IllegalArgumentException("cycle must not be null or empty");
        }
        this.cycle = Collections.unmodifiableList(cycle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeadlockCycle that = (DeadlockCycle) o;

        if (!cycle.equals(that.cycle)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return cycle.hashCode();
    }

    @Override
    public String toString() {
        return "DeadlockCycle{" +
                "cycle=" + cycle +
                '}';
    }
}
