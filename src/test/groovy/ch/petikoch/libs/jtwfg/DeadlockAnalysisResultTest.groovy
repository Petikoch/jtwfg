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

class DeadlockAnalysisResultTest extends Specification {

	def 'hasDeadlock specs'() {
		expect:
		deadlockAnalysisResult.hasDeadlock() == result

		where:
		deadlockAnalysisResult                                                             | result
		new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null)] as Set) | true
		new DeadlockAnalysisResult([] as Set)                                              | false
	}

	def 'getDeadlockCycles specs'() {
		expect:
		deadlockAnalysisResult.getDeadlockCycles() == result

		where:
		deadlockAnalysisResult                                                             | result
		new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null)] as Set) | [new DeadlockCycle<String>(['t1', 't1'], null)] as Set
	}

	def 'getDeadlockCycles returns unmodifiable set'() {
		given:
		def deadlockAnalysisResult = new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null)] as Set)

		when:
		deadlockAnalysisResult.getDeadlockCycles().clear()

		then:
		thrown(UnsupportedOperationException)
	}

	def 'isDeadlocked specs'() {
		expect:
		deadlockAnalysisResult.isDeadlocked(task) == result

		where:
		deadlockAnalysisResult                                                                                                            | task  | result
		new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null)] as Set)                                                | 't1'  | true
		new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null), new DeadlockCycle<String>(['t2', 't2'], null)] as Set) | 't1'  | true
		new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null), new DeadlockCycle<String>(['t2', 't2'], null)] as Set) | 't2'  | true

		new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null), new DeadlockCycle<String>(['t2', 't2'], null)] as Set) | 't42' | false
	}

	def 'toString: nice String representation'() {
		when:
		def result = new DeadlockAnalysisResult([] as Set).toString()

		then:
		result == '''\
DeadlockAnalysisResult:

hasDeadlock=false'''

		when:
		result = new DeadlockAnalysisResult([new DeadlockCycle<String>(['t1', 't1'], null), new DeadlockCycle<>(['t1', 't2', 't1'], ['t3': ['t1', 't2'] as Set, 't4': ['t1', 't3'] as Set, 't5': ['t1'] as Set])] as Set).toString()

		then:
		result == '''\
DeadlockAnalysisResult:

hasDeadlock=true

Cycles:
- DeadlockCycle: t1 -> t1
- DeadlockCycle: t1 -> t2 -> t1. The following tasks are also deadlocked, because they are direct or indirect dependent on at least one of the tasks in the deadlock cycle: t3->t1 t3->t2 t4->t1 t4->t3 t5->t1.'''
	}
}
