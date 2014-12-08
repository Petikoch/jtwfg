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

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.common.collect.Sets
import com.google.common.collect.TreeMultimap
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessBoolean")
class Documentation extends Specification {

	def 'Use case 1: Check for deadlocks at a specific moment in time'() {

		given: 'Your own domain model with some kind of tasks and dependencies between them'

		Set<String> tasks = Sets.newTreeSet()
		tasks.add('t1')
		tasks.add('t2')
		tasks.add('t3')
		tasks.add('t4')
		tasks.add('t5')

		Multimap<String, String> task2TaskDependencies = TreeMultimap.create()
		task2TaskDependencies.put('t1', 't2')
		task2TaskDependencies.put('t2', 't3')
		task2TaskDependencies.put('t3', 't1')

		when: 'you want to know if you have a deadlock in it, you transform your model into the jtwfg model using the GraphBuilder'

		def graphBuilder = new GraphBuilder<String>()
		for (String myTaskIdFromMyDomainModel : tasks) {
			graphBuilder.addTask(myTaskIdFromMyDomainModel)
			if (task2TaskDependencies.containsKey(myTaskIdFromMyDomainModel)) {
				task2TaskDependencies.get(myTaskIdFromMyDomainModel).each { String waitForTaskId ->
					graphBuilder.addTaskWaitFor(myTaskIdFromMyDomainModel, waitForTaskId)
				}
			}
		}

		and: 'build the graph'

		def jtwfgGraph = graphBuilder.build()

		and: 'run a deadlock analysis using the deadlock detector'

		def analysisResult = new DeadlockDetector().analyze(jtwfgGraph)

		then: 'you check for deadlocks in the analysis report'

		analysisResult.hasDeadlock() == true
		analysisResult.deadlockCycles.size() == 1

		and: 'you see where the deadlock is'

		analysisResult.deadlockCycles.getAt(0).getInvolvedTasks() == ['t1', 't2', 't3', 't1']
	}

	def 'Use case 2: As you update your domain model, you update the jtwg model and check for deadlocks'() {

		given: 'Your own domain model with some kind of tasks and dependencies between them'

		Set<String> tasks = Sets.newTreeSet().asSynchronized()
		Multimap<String, String> task2TaskDependencies = Multimaps.synchronizedSortedSetMultimap(TreeMultimap.create())

		and: 'the jtwfg GraphBuilder with a DeadlockDetector'

		def graphBuilder = new GraphBuilder<String>()
		def deadlockDetector = new DeadlockDetector<String>()

		when: 'you add a task into your model, you add it also into the jtwfg model (might be a separate thread)'

		tasks.add('t1')
		graphBuilder.addTask('t1')

		then: 'you immediately check for deadlocks'
		deadlockDetector.analyze(graphBuilder.build()).hasDeadlock() == false

		when: 'you add more tasks, you update your model and the jtwfg model (might be a separate thread)'

		tasks.add('t2')
		graphBuilder.addTask('t2')
		tasks.add('t3')
		graphBuilder.addTask('t3')
		task2TaskDependencies.put('t2', 't3')
		graphBuilder.addTaskWaitFor('t2', 't3')
		task2TaskDependencies.put('t3', 't2')
		graphBuilder.addTaskWaitFor('t3', 't2')

		and: 'you immediately check for deadlocks again'

		def analysisReport = deadlockDetector.analyze(graphBuilder.build())

		then: 'you see if you have a deadlock'

		analysisReport.hasDeadlock() == true

		and: 'you see where the deadlock is'

		analysisReport.deadlockCycles.size() == 1
		analysisReport.deadlockCycles.getAt(0).getInvolvedTasks() == ['t2', 't3', 't2']
	}
}
