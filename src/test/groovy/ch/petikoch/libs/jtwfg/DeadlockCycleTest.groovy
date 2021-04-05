/*
 * Copyright 2014-2021 Peti Koch und Adrian Elsener
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

class DeadlockCycleTest extends Specification {

	def 'equals specs'() {
		expect:
		deadLockCycle1.equals(deadLockCycle2) == result

		where:
		deadLockCycle1                                                                                           | deadLockCycle2                                                                                           | result
		new DeadlockCycle<>(['t1', 't1'], null)                                                                  | new DeadlockCycle<>(['t1', 't1'], null)                                                                  | true
		new DeadlockCycle<>(['t1', 't1'], [:])                                                                   | new DeadlockCycle<>(['t1', 't1'], [:])                                                                   | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                                 | new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                                 | true
		new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                            | new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                            | true
		new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                            | new DeadlockCycle<>(['t1', 't2', 't1'], [:])                                                             | true
		new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                            | new DeadlockCycle<>(['t2', 't1', 't2'], null)                                                            | true
		new DeadlockCycle<>(['t1', 't2', 't1'], ['t3': ['t2'] as Set, 't4': ['t2'] as Set, 't5': ['t1'] as Set]) | new DeadlockCycle<>(['t2', 't1', 't2'], ['t5': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t2'] as Set]) | true
		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'], null)                                                | new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'], null)                                                | true
		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'], null)                                                | new DeadlockCycle<>(['t2', 't3', 't4', 't1', 't2'], null)                                                | true
		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'], null)                                                | new DeadlockCycle<>(['t3', 't4', 't1', 't2', 't3'], null)                                                | true

		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'], null)                                                | new DeadlockCycle<>(['t1', 't3', 't2', 't4', 't1'], null)                                                | false
		new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                            | new DeadlockCycle<>(['t1', 't42', 't1'], null)                                                           | false
		new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                            | new DeadlockCycle<>(['t1', 't2', 't1'], ['t3': ['t1'] as Set])                                           | false
		new DeadlockCycle<>(['t1', 't2', 't1'], ['t3': ['t1'] as Set])                                           | new DeadlockCycle<>(['t1', 't2', 't1'], ['t3': ['t1', 't2'] as Set])                                     | false
	}

	def 'equals standard behaviour'() {
		given:
		def cycle = new DeadlockCycle<>(['t1', 't1'], null)

		when:
		def isEqual = cycle.equals(cycle)

		then:
		isEqual

		when:
		def isNotEqual = !cycle.equals('egg')

		then:
		isNotEqual
	}

	def 'toString: nice String representation'() {
		expect:
		deadLockCycle.toString() == result

		where:
		deadLockCycle                                                                                                        | result
		new DeadlockCycle<>(['t1', 't1'], null)                                                                              | 'DeadlockCycle: t1 -> t1'
		new DeadlockCycle<>(['t1', 't1'], [:])                                                                               | 'DeadlockCycle: t1 -> t1'
		new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                                        | 'DeadlockCycle: t1 -> t2 -> t1'
		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'], null)                                                            | 'DeadlockCycle: t1 -> t2 -> t3 -> t4 -> t1'

		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                                             | 'DeadlockCycle: t1 -> t1. The following tasks are also deadlocked, because they are direct or indirect dependent on at least one of the tasks in the deadlock cycle: t2->t1.'
		new DeadlockCycle<>(['t1', 't2', 't1'], ['t3': ['t2'] as Set, 't4': ['t3'] as Set, 't5': ['t1'] as Set])             | 'DeadlockCycle: t1 -> t2 -> t1. The following tasks are also deadlocked, because they are direct or indirect dependent on at least one of the tasks in the deadlock cycle: t3->t2 t4->t3 t5->t1.'
		new DeadlockCycle<>(['t1', 't2', 't1'], ['t3': ['t1', 't2'] as Set, 't4': ['t1', 't3'] as Set, 't5': ['t1'] as Set]) | 'DeadlockCycle: t1 -> t2 -> t1. The following tasks are also deadlocked, because they are direct or indirect dependent on at least one of the tasks in the deadlock cycle: t3->t1 t3->t2 t4->t1 t4->t3 t5->t1.'
	}

	def 'getAlsoDeadlockedTasks specs'() {
		expect:
		deadLockCycle.getAlsoDeadlockedTasks() == result

		where:
		deadLockCycle                                                                                      | result
		new DeadlockCycle<>(['t1', 't1'], null)                                                            | [:]
		new DeadlockCycle<>(['t1', 't1'], [:])                                                             | [:]
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                           | ['t2': ['t1'] as Set]
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]
	}

	def 'getAlsoDeadlockedTasks returns unmodifiable map'() {
		given:
		def deadlockCycle = new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])
		def alsoDeadlockedTasksMap = deadlockCycle.getAlsoDeadlockedTasks()

		when:
		alsoDeadlockedTasksMap.clear()

		then:
		thrown(UnsupportedOperationException)

		when:
		alsoDeadlockedTasksMap.entrySet().iterator().next().getValue().clear()

		then:
		thrown(UnsupportedOperationException)
	}

	def 'getAllDeadlockedTasks specs'() {
		expect:
		deadLockCycle.getAllDeadlockedTasks() == result

		where:
		deadLockCycle                                                                                      | result
		new DeadlockCycle<>(['t1', 't1'], null)                                                            | ['t1'] as Set
		new DeadlockCycle<>(['t1', 't1'], [:])                                                             | ['t1'] as Set
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                           | ['t1', 't2'] as Set
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | ['t1', 't2', 't3', 't4'] as Set
	}

	def 'getAllDeadlockedTasks returns unmodifiable set'() {
		given:
		def deadlockCycle = new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set])
		def allDeadlockedTasks = deadlockCycle.getAllDeadlockedTasks()

		when:
		allDeadlockedTasks.clear()

		then:
		thrown(UnsupportedOperationException)
	}

	def 'isDeadlocked specs'() {
		expect:
		deadLockCycle.isDeadlocked(task) == result

		where:
		deadLockCycle                                                                                      | task  | result
		new DeadlockCycle<>(['t1', 't1'], null)                                                            | 't1'  | true
		new DeadlockCycle<>(['t1', 't1'], [:])                                                             | 't1'  | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                           | 't1'  | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                           | 't2'  | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | 't1'  | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | 't2'  | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | 't3'  | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | 't4'  | true

		new DeadlockCycle<>(['t1', 't1'], null)                                                            | 't42' | false
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | 't99' | false
	}

	def 'areAllDeadlocked specs'() {
		expect:
		deadLockCycle.areAllDeadlocked(tasks) == result

		where:
		deadLockCycle                                                                                      | tasks                    | result
		new DeadlockCycle<>(['t1', 't1'], null)                                                            | ['t1']                   | true
		new DeadlockCycle<>(['t1', 't1'], [:])                                                             | ['t1']                   | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set])                                           | ['t1', 't2']             | true
		new DeadlockCycle<>(['t1', 't1'], ['t2': ['t1'] as Set, 't3': ['t2'] as Set, 't4': ['t1'] as Set]) | ['t1', 't2', 't3', 't4'] | true

		new DeadlockCycle<>(['t1', 't1'], null)                                                            | ['t42']                  | false
		new DeadlockCycle<>(['t1', 't1'], null)                                                            | ['t1', 't42']            | false
		new DeadlockCycle<>(['t1', 't2', 't1'], null)                                                      | ['t1', 't2', 't42']      | false
	}
}