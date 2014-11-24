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


import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DeadlockDetector {

    private static DeadlockCycle findDeadlockRecursively(Task startTask, Set<Task> toCompareList, List<Task> hops) {
        for (Task other : toCompareList) {
            for (Task otherOfOther : other.getWaitForTasks()) {
                if (startTask.equals(otherOfOther)) {
                    List<Task> cycleList = new LinkedList<>();
                    cycleList.add(startTask);
                    cycleList.addAll(hops);
                    cycleList.add(other);
                    return new DeadlockCycle(cycleList);
                }
            }
            LinkedList<Task> hopsCopy = new LinkedList<>(hops);
            hopsCopy.add(other);
            return findDeadlockRecursively(startTask, other.getWaitForTasks(), hopsCopy);
        }
        return null;
    }

    public DeadlockCycle findDeadlock(List<Task> tasks) {
        for (Task startTask : tasks) {
            DeadlockCycle found = findDeadlockRecursively(startTask, startTask.getWaitForTasks(), new LinkedList<Task>());
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
