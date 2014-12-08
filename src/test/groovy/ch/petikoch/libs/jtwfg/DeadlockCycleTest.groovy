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

class DeadlockCycleTest extends Specification {

	def 'equals specs'() {
		expect:
		deadLockCycle1.equals(deadLockCycle2) == result

		where:
		deadLockCycle1                                      | deadLockCycle2                                      | result
		new DeadlockCycle<>([])                             | new DeadlockCycle<>([])                             | true
		new DeadlockCycle<>(['t1', 't1'])                   | new DeadlockCycle<>(['t1', 't1'])                   | true
		new DeadlockCycle<>(['t1', 't2', 't1'])             | new DeadlockCycle<>(['t1', 't2', 't1'])             | true
		new DeadlockCycle<>(['t1', 't2', 't1'])             | new DeadlockCycle<>(['t2', 't1', 't2'])             | true
		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1']) | new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1']) | true
		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1']) | new DeadlockCycle<>(['t2', 't3', 't4', 't1', 't2']) | true
		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1']) | new DeadlockCycle<>(['t3', 't4', 't1', 't2', 't3']) | true

		new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1']) | new DeadlockCycle<>(['t1', 't3', 't2', 't4', 't1']) | false
		new DeadlockCycle<>([])                             | new DeadlockCycle<>(['t1'])                         | false
		new DeadlockCycle<>(['t1', 't2', 't1'])             | new DeadlockCycle<>(['t1', 't42', 't1'])            | false
	}

	def 'equals standard behaviour'() {
		given:
		def cycle = new DeadlockCycle<>(['t1', 't1'])

		when:
		def isEqual = cycle.equals(cycle)

		then:
		isEqual

		when:
		def isNotEqual = !cycle.equals('egg')

		then:
		isNotEqual
	}
}

