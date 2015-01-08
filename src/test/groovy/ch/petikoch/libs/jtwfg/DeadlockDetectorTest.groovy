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

	def 'findDeadlock: 2 distinct triangle cycles'() {
		given: "triangle cycle 1"
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t2', 't3')
		graphBuilder.addTaskWaitsFor('t3', 't1')

		and: "triangle cycle 2"
		graphBuilder.addTaskWaitsFor('t11', 't12')
		graphBuilder.addTaskWaitsFor('t12', 't13')
		graphBuilder.addTaskWaitsFor('t13', 't11')

		and:
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 2
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't2', 't3', 't1'], null)
		result.deadlockCycles.getAt(1) == new DeadlockCycle<>(['t11', 't12', 't13', 't11'], null)
	}

	def 'findDeadlock: 3 distinct triangle cycles'() {
		given: "triangle cycle 1"
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t2', 't3')
		graphBuilder.addTaskWaitsFor('t3', 't1')

		and: "triangle cycle 2"
		graphBuilder.addTaskWaitsFor('t11', 't12')
		graphBuilder.addTaskWaitsFor('t12', 't13')
		graphBuilder.addTaskWaitsFor('t13', 't11')

		and: "triangle cycle 3"
		graphBuilder.addTaskWaitsFor('t21', 't22')
		graphBuilder.addTaskWaitsFor('t22', 't23')
		graphBuilder.addTaskWaitsFor('t23', 't21')

		and:
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 3
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't2', 't3', 't1'], null)
		result.deadlockCycles.getAt(1) == new DeadlockCycle<>(['t11', 't12', 't13', 't11'], null)
		result.deadlockCycles.getAt(2) == new DeadlockCycle<>(['t21', 't22', 't23', 't21'], null)
	}

	def 'findDeadlock: 2 overlapping triangle cycles are considered as one cycle with additional deadlocked tasks'() {
		given: "triangle cycle 1"
		graphBuilder.addTaskWaitsFor('t1', 't2')
		graphBuilder.addTaskWaitsFor('t2', 't3')
		graphBuilder.addTaskWaitsFor('t3', 't1')

		and: "triangle cycle 2"
		graphBuilder.addTaskWaitsFor('t4', 't2')
		graphBuilder.addTaskWaitsFor('t5', 't4')
		graphBuilder.addTaskWaitsFor('t2', 't5')

		and:
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>(['t1', 't2', 't3', 't1'], ['t4': ['t2'] as Set, 't5': ['t4'] as Set])
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

	def 'findDeadlock: many tasks no locking dependencies'() {
		given:
		def numberOfTasks = 100000
		(1..numberOfTasks).each { int it ->
			graphBuilder.addTask("t${it}".toString())
		}
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		!result.hasDeadlock()
		result.deadlockCycles.size() == 0
	}

	def 'findDeadlock: many tasks with one simple deadlock'() {
		given:
		def numberOfTasks = 100000
		(1..numberOfTasks).each { int it ->
			graphBuilder.addTask("t${it}".toString())
		}
		def deadLockedTask1 = 't_deadlocked1'
		def deadLockedTask2 = 't_deadlocked2'
		graphBuilder.addTaskWaitsFor(deadLockedTask1, deadLockedTask2)
		graphBuilder.addTaskWaitsFor(deadLockedTask2, deadLockedTask1)
		def graph = graphBuilder.build()

		when:
		def result = testee.analyze(graph)

		then:
		result != null
		result.hasDeadlock()
		result.deadlockCycles.size() == 1
		result.deadlockCycles.getAt(0) == new DeadlockCycle<>([deadLockedTask1, deadLockedTask2, deadLockedTask1], null)
	}
}
