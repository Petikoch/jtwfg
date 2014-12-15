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

	//TODO Test thread-safety
	//TODO Check coverage

	def 'findDeadlock: Simple direct cycle'() {
		given:
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t2', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't2', 't1'], null)
	}

	def 'findDeadlock: task referencing itself'() {
		given:
		graphBuilder.addTaskWaitsFor('t1', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't1'], null)
	}

	def 'findDeadlock: 4 tasks no dependencies'() {
		given:
		def graph = graphBuilder.addTasks(['t1', 't2', 't3', 't4']).build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		!result.hasDeadlock()
		result.deadlockCycles.size() == 0
	}

	def 'findDeadlock: triangle cycle'() {
		given:
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t2', 't3')
		graphBuilder.addTaskWaitsFor('t3', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't2', 't3', 't1'], null)
	}

	def 'findDeadlock: square cycle'() {
		given:
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t2', 't3')
		graphBuilder.addTaskWaitsFor('t3', 't4')
		graphBuilder.addTaskWaitsFor('t4', 't1')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't2', 't3', 't4', 't1'], null)
	}

	def 'findDeadlock: 4 tasks no locking dependencies'() {
		given:
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t1', 't3')
		graphBuilder.addTaskWaitsFor('t1', 't4')
		graphBuilder.addTaskWaitsFor('t2', 't3')
		graphBuilder.addTaskWaitsFor('t2', 't4')
		graphBuilder.addTaskWaitsFor('t3', 't4')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		!result.hasDeadlock()
		result.deadlockCycles.size() == 0
	}

	def 'findDeadlock: 4 tasks with simple deadlock'() {
		given:
		graphBuilder.addTasks(['t1', 't2'])
		graphBuilder.addTaskWaitsFor('t3', 't4')
		graphBuilder.addTaskWaitsFor('t4', 't3')
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t3', 't4', 't3'], null)
	}

	def 'findDeadlock: triangle cycle with additional deadlocked tasks outside cycle'() {

		given: 'some tasks in a deadlock cycle'
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t2', 't3')
		graphBuilder.addTaskWaitsFor('t3', 't1')

		and: 'some tasks depending directly or indirectly on a task of the deadlock cycle'
		graphBuilder.addTaskWaitsFor('t4', 't1')
		graphBuilder.addTaskWaitsFor('t4', 't2')
		graphBuilder.addTaskWaitsFor('t4', 't3')
		graphBuilder.addTaskWaitsFor('t6', 't3')
		graphBuilder.addTaskWaitsFor('t7', 't6')

		and: 'a not locked task'
		graphBuilder.addTask('t5')

		when:
		def graph = graphBuilder.build()
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't2', 't3', 't1'], ['t4': ['t1', 't2', 't3'] as Set, 't6': ['t3'] as Set, 't7': ['t6'] as Set])
		result.deadlockCycles.getAt(0).areAllDeadlocked(['t1', 't2', 't3', 't4', 't6', 't7'])
		!result.deadlockCycles.getAt(0).isDeadlocked('t5')
	}
}
