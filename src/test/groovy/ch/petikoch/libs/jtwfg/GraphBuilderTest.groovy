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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GraphBuilderTest extends Specification {

	def testee = new GraphBuilder()

	def 'addTask and build'() {
		given:
		def taskId1 = 't1'
		def taskId2 = 't2'

		when:
		testee.addTask(taskId1)
		testee.addTask(taskId2)

		and:
		def graph = testee.build()

		then:
		graph != null
		graph.getTasks() == [new Task<String>(taskId1), new Task<String>(taskId2)] as Set
	}

	def 'addTasks and build'() {
		given:
		def taskId1 = 't1'
		def taskId2 = 't2'

		when:
		testee.addTasks([taskId1, taskId2])

		and:
		def graph = testee.build()

		then:
		graph != null
		graph.getTasks() == [new Task<String>(taskId1), new Task<String>(taskId2)] as Set
	}

	def 'addTasks, removeTask and build'() {
		given:
		def taskId1 = 't1'
		def taskId2 = 't2'

		when:
		testee.addTasks([taskId1, taskId2])

		and:
		testee.removeTask(taskId1)
		testee.removeTask(taskId2)

		and:
		def graph = testee.build()

		then:
		graph != null
		graph.getTasks() == [] as Set
	}

	def 'addTasks, removeTasks and build'() {
		given:
		def taskId1 = 't1'
		def taskId2 = 't2'

		when:
		testee.addTasks([taskId1, taskId2])

		and:
		testee.removeTasks([taskId1, taskId2])

		and:
		def graph = testee.build()

		then:
		graph != null
		graph.getTasks() == [] as Set
	}

	def 'addTask: taskId must not be null'() {
		when:
		testee.addTask(null)

		then:
		def ex = thrown(IllegalArgumentException)
		ex.message == 'taskId must not be null'
	}

	def 'removeTask: taskId must not be null'() {
		when:
		testee.removeTask(null)

		then:
		def ex = thrown(IllegalArgumentException)
		ex.message == 'taskId must not be null'
	}

	def 'removeTasks: taskIds may be null or empty'() {
		when:
		testee.removeTasks(null)
		then:
		noExceptionThrown()

		when:
		testee.removeTasks([])
		then:
		noExceptionThrown()
	}

	def 'removeTask: taskId must have been added before'() {
		when:
		testee.removeTask('t1')

		then:
		def ex = thrown(IllegalArgumentException)
		ex.message == "taskId t1 is unknown and can't be removed"
	}

	def 'removeTasks: ALL taskIds must have been added before'() {
		given:
		testee.addTasks(['t2', 't3'])

		when:
		testee.removeTasks(['t1', 't2'])

		then:
		def ex = thrown(IllegalArgumentException)
		ex.message == "taskId t1 is unknown and can't be removed. None of the given tasks [t1, t2] were removed"
	}

	def 'hasTask'() {
		given:
		def taskId1 = 't1'

		when:
		def result = testee.hasTask(taskId1)

		then:
		!result

		when:
		testee.addTask(taskId1)
		result = testee.hasTask(taskId1)

		then:
		result

		when:
		testee.removeTask(taskId1)
		result = testee.hasTask(taskId1)

		then:
		!result
	}

	def 'hasTask: taskId must not be null'() {
		when:
		testee.hasTask(null)

		then:
		def ex = thrown(IllegalArgumentException)
		ex.message == 'taskId must not be null'
	}

	def 'removeTaskWaitForDependency: happy flow'() {
		setup:
		def taskId1 = 't1'
		def taskId2 = 't2'
		testee.addTaskWaitsFor(taskId1, taskId2)

		when:
		def graph = testee.build()

		then:
		graph.getTasks().getAt(0).getId() == taskId1
		!graph.getTasks().getAt(0).getWaitsForTasks().isEmpty()

		when:
		testee.removeTaskWaitForDependency(taskId1, taskId2)
		graph = testee.build()

		then:
		graph.getTasks().getAt(0).getId() == taskId1
		graph.getTasks().getAt(0).getWaitsForTasks().isEmpty()
	}

	def 'removeTaskWaitForDependency: works only on existing tasks'() {
		setup:
		def taskId1 = 't1'
		def taskId2 = 't2'
		def taskId3 = 't3'
		testee.addTaskWaitsFor(taskId1, taskId2)
		testee.addTask(taskId3)

		when:
		testee.removeTaskWaitForDependency('t42', taskId2)
		then:
		def ex = thrown(IllegalArgumentException)
		ex.message == 'taskId t42 is unknown'

		when:
		testee.removeTaskWaitForDependency(taskId1, 't42')
		then:
		ex = thrown(IllegalArgumentException)
		ex.message == 'taskId t42 is unknown'

		when:
		testee.removeTaskWaitForDependency(taskId1, taskId3)
		then:
		ex = thrown(IllegalArgumentException)
		ex.message == 't1 is existing but was not waiting on t3'
	}

	def 'build creates always a separate graph instance with separate task instance copies'() {
		setup:
		def taskId1 = 't1'
		def taskId2 = 't2'
		def taskId3 = 't3'
		testee.addTasks([taskId1, taskId2, taskId3])
		testee.addTaskWaitsFor(taskId1, taskId3)
		testee.addTaskWaitsFor(taskId2, taskId3)

		when:
		def graph1 = testee.build()
		def graph2 = testee.build()

		then:
		graph1 != null
		graph2 != null
		!graph1.is(graph2)
		graph1 == graph2
		graph1.getTasks().getAt(0) == graph2.getTasks().getAt(0)
		!graph1.getTasks().getAt(0).is(graph2.getTasks().getAt(0))
		graph1.getTasks().getAt(1) == graph2.getTasks().getAt(1)
		!graph1.getTasks().getAt(1).is(graph2.getTasks().getAt(1))
		graph1.getTasks().getAt(2) == graph2.getTasks().getAt(2)
		!graph1.getTasks().getAt(2).is(graph2.getTasks().getAt(2))
		graph1.getTasks().getAt(0).getWaitsForTasks()[0] == graph2.getTasks().getAt(0).getWaitsForTasks()[0]
		!graph1.getTasks().getAt(0).getWaitsForTasks()[0].is(graph2.getTasks().getAt(0).getWaitsForTasks()[0])
		graph1.getTasks().getAt(1).getWaitsForTasks()[0] == graph2.getTasks().getAt(1).getWaitsForTasks()[0]
		!graph1.getTasks().getAt(1).getWaitsForTasks()[0].is(graph2.getTasks().getAt(1).getWaitsForTasks()[0])
	}

	def 'adders can be called multiple times, it only adds if not yet present'() {
		given:
		def taskId1 = 't1'
		def taskId2 = 't2'
		def taskId3 = 't3'

		when:
		10.times {
			testee.addTasks([taskId1, taskId2, taskId3])
			testee.addTaskWaitsFor(taskId1, taskId3)
			testee.addTaskWaitsFor(taskId2, taskId3)
		}
		def graph = testee.build()

		then:
		graph != null
		graph.getTasks().size() == 3
		graph.getTasks().getAt(0).getId() == taskId1
		graph.getTasks().getAt(1).getId() == taskId2
		graph.getTasks().getAt(2).getId() == taskId3
		graph.getTasks().getAt(0).getWaitsForTasks().size() == 1
		graph.getTasks().getAt(0).getWaitsForTasks().getAt(0).getId() == taskId3
		graph.getTasks().getAt(1).getWaitsForTasks().size() == 1
		graph.getTasks().getAt(1).getWaitsForTasks().getAt(0).getId() == taskId3
	}

	def 'concurrency: the graph can be built using multiple threads'() {
		given:
		def numberOfCpuCores = Runtime.getRuntime().availableProcessors()
		def numberOfThreads = numberOfCpuCores * 8
		def numberOfTasksPerThread = 100
		def totalNumberOfTasksOverall = numberOfThreads * numberOfTasksPerThread

		and:
		def expectedTaskIdsInGraph = [] as Set
		numberOfThreads.times { int threadNumber ->
			numberOfTasksPerThread.times { int taskPerThreadNumber ->
				def taskId = "task ${taskPerThreadNumber} of thread ${threadNumber}".toString()
				expectedTaskIdsInGraph.add(taskId)
			}
		}
		assert expectedTaskIdsInGraph.size() == totalNumberOfTasksOverall

		and:
		def threadCanStartCountDownLatch = new CountDownLatch(numberOfThreads)
		def allThreadsDoneCountDownLatch = new CountDownLatch(numberOfThreads)

		when:
		numberOfThreads.times { int threadNumber ->
			Thread.startDaemon(GraphBuilderTest.class.simpleName + '-thread-' + threadNumber) {
				threadCanStartCountDownLatch.countDown()
				threadCanStartCountDownLatch.await(1, TimeUnit.MINUTES)

				numberOfTasksPerThread.times { int taskPerThreadNumber ->
					def taskId = "task ${taskPerThreadNumber} of thread ${threadNumber}".toString()
					testee.addTask(taskId)
				}

				println Thread.currentThread().getName() + ' added ' + numberOfTasksPerThread + 'tasks'

				allThreadsDoneCountDownLatch.countDown()
			}
		}
		allThreadsDoneCountDownLatch.await(1, TimeUnit.MINUTES)
		def graph = testee.build()

		then:
		graph != null
		graph.getTasks().size() == totalNumberOfTasksOverall
		graph.getTasks().collect { it.getId() }.toSet() == expectedTaskIdsInGraph
	}

	def 'concurrency: a consistent snapshot of the graph can be built anytime, especially while other threads are adding tasks'() {
		given:
		def numberOfCpuCores = Runtime.getRuntime().availableProcessors()
		def numberOfOtherThreads = numberOfCpuCores * 8
		def numberOfTasksPerOtherThread = 100
		def numberOfTasksOfMainThread = numberOfTasksPerOtherThread

		and:
		def threadCanStartCountDownLatch = new CountDownLatch(numberOfOtherThreads)

		when:
		numberOfOtherThreads.times { int threadNumber ->
			Thread.startDaemon(GraphBuilderTest.class.simpleName + '-thread-' + threadNumber) {
				threadCanStartCountDownLatch.countDown()
				threadCanStartCountDownLatch.await(1, TimeUnit.MINUTES)

				numberOfTasksPerOtherThread.times { int taskPerThreadNumber ->
					def taskId = "task ${taskPerThreadNumber} of thread ${threadNumber}".toString()
					testee.addTask(taskId)
				}
			}
		}
		threadCanStartCountDownLatch.await(1, TimeUnit.MINUTES)
		List<String> mainThreadTaskIds = []
		numberOfTasksOfMainThread.times {
			mainThreadTaskIds.add('task ' + it + ' of main thread')
		}
		testee.addTasks(mainThreadTaskIds)
		def graph = testee.build()

		then:
		graph != null
		graph.getTasks().collect { it.getId() }.toSet().containsAll(mainThreadTaskIds)
	}
}
