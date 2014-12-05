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



package ch.petikoch.libs.jtwfg

import spock.lang.Specification

class DeadlockDetectorTest extends Specification {

    def testee = new DeadlockDetector()

    def 'findDeadlock: Simple direct cycle'() {
        given:
        Task t1 = new Task('t1')
        Task t2 = new Task('t2')
        t1.addWaitFor(t2)
        t2.addWaitFor(t1)
        List<Task> tasks = [t1, t2]

        when:
        DeadlockCycle result = testee.findDeadlock(tasks)

        then:
        result == new DeadlockCycle(tasks)
    }

    def 'findDeadlock: 4 tasks no dependencies'() {
        given:
        Task t1 = new Task('t1')
        Task t2 = new Task('t2')
        Task t3 = new Task('t3')
        Task t4 = new Task('t4')
        List<Task> tasks = [t1, t2, t3, t4]
        when:
        DeadlockCycle result = testee.findDeadlock(tasks)
        then:
        result == null
    }

    def 'findDeadlock: triangle cycle'() {
        given:
        Task t1 = new Task('t1')
        Task t2 = new Task('t2')
        Task t3 = new Task('t3')
        t1.addWaitFor(t2)
        t2.addWaitFor(t3)
        t3.addWaitFor(t1)
        List<Task> tasks = [t1, t2, t3]

        when:
        DeadlockCycle result = testee.findDeadlock(tasks)

        then:
        result == new DeadlockCycle(tasks)
    }

    def 'findDeadlock: square cycle'() {
        given:
        Task t1 = new Task('t1')
        Task t2 = new Task('t2')
        Task t3 = new Task('t3')
        Task t4 = new Task('t4')
        t1.addWaitFor(t2)
        t2.addWaitFor(t3)
        t3.addWaitFor(t4)
        t4.addWaitFor(t1)
        List<Task> tasks = [t1, t2, t3, t4]

        when:
        DeadlockCycle result = testee.findDeadlock(tasks)

        then:
        result == new DeadlockCycle(tasks)
    }

    def 'findDeadlock: 4 tasks no locking dependencies'() {
        given:
        Task t1 = new Task('t1')
        Task t2 = new Task('t2')
        Task t3 = new Task('t3')
        Task t4 = new Task('t4')
        t1.addWaitForAll([t2, t3, t4])
        t2.addWaitForAll([t3, t4])
        t3.addWaitForAll([t4])
        List<Task> tasks = [t1, t2, t3, t4]
        when:
        DeadlockCycle result = testee.findDeadlock(tasks)
        then:
        result == null
    }

    def 'findDeadlock: 4 tasks with simple deadlock'() {
        given:
        Task t1 = new Task('t1')
        Task t2 = new Task('t2')
        Task t3 = new Task('t3')
        Task t4 = new Task('t4')
        t3.addWaitFor(t4)
        t4.addWaitFor(t3)
        List<Task> tasks = [t1, t2, t3, t4]
        when:
        DeadlockCycle result = testee.findDeadlock(tasks)
        then:
        result == new DeadlockCycle([t3, t4])
    }

}
