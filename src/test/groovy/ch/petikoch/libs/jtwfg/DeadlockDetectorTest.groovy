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

	def graphBuilder = new GraphBuilder<String>()
	def testee = new DeadlockDetector<String>()

	//TODO Test threadsafty
	//TODO Check coverage

	def 'findDeadlock: Simple direct cycle'() {
		given:
		graphBuilder.addTaskWaitFor('t1', 't2')
		graphBuilder.addTaskWaitFor('t2', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles == [new DeadlockCycle<>(['t1', 't2', 't1'])] as Set
	}

	def 'findDeadlock: task referencing itself'() {
		given:
		graphBuilder.addTaskWaitFor('t1', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles == [new DeadlockCycle<>(['t1', 't1'])] as Set
	}

	def 'findDeadlock: 4 tasks no dependencies'() {
		given:
		def graph = graphBuilder.addTasks(['t1', 't2', 't3', 't4']).build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		!result.hasDeadlock()
		result.deadlockCycles == [] as Set
	}

	def 'findDeadlock: triangle cycle'() {
		given:
		graphBuilder.addTaskWaitFor('t1', 't2')
		graphBuilder.addTaskWaitFor('t2', 't3')
		graphBuilder.addTaskWaitFor('t3', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles == [new DeadlockCycle<>(['t1', 't2', 't3', 't1'])] as Set
	}

	def 'findDeadlock: square cycle'() {
		given:
		graphBuilder.addTaskWaitFor('t1', 't2')
		graphBuilder.addTaskWaitFor('t2', 't3')
		graphBuilder.addTaskWaitFor('t3', 't4')
		graphBuilder.addTaskWaitFor('t4', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles == [new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'])] as Set
	}

	def 'findDeadlock: 4 tasks no locking dependencies'() {
		given:
		graphBuilder.addTaskWaitFor('t1', 't2')
		graphBuilder.addTaskWaitFor('t1', 't3')
		graphBuilder.addTaskWaitFor('t1', 't4')
		graphBuilder.addTaskWaitFor('t2', 't3')
		graphBuilder.addTaskWaitFor('t2', 't4')
		graphBuilder.addTaskWaitFor('t3', 't4')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		!result.hasDeadlock()
		result.deadlockCycles == [] as Set
	}

	def 'findDeadlock: 4 tasks with simple deadlock'() {
		given:
		graphBuilder.addTasks(['t1', 't2'])
		graphBuilder.addTaskWaitFor('t3', 't4')
		graphBuilder.addTaskWaitFor('t4', 't3')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles == [new DeadlockCycle<>(['t3', 't4', 't3'])] as Set
	}

}
