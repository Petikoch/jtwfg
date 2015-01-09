/*
 * Copyright 2014-2015 Peti Koch und Adrian Elsener
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

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import spock.lang.Specification

class TaskTest extends Specification {

	def 'compareTo: works fine on standard types implementing comparable'() {
		given:
		def taskId1 = 't1'
		def taskId2 = 't2'
		def task1 = new Task<String>(taskId1)
		def task2 = new Task<String>(taskId2)

		when:
		def result = task1.compareTo(task2)

		then:
		result == -1

		when:
		result = task2.compareTo(task1)

		then:
		result == 1
	}

	def 'compareTo: works fine on custom types implementing comparable'() {
		given:
		def taskId1 = new CustomComparableTaskId('t1')
		def taskId2 = new CustomComparableTaskId('t2')
		def task1 = new Task<CustomComparableTaskId>(taskId1)
		def task2 = new Task<CustomComparableTaskId>(taskId2)

		when:
		def result = task1.compareTo(task2)

		then:
		result == -1

		when:
		result = task2.compareTo(task1)

		then:
		result == 1
	}

	def 'compareTo: behaviour on types not implementing comparable consistent to equals'() {
		given:
		def taskId1 = new CustomTaskId('t1')
		def equalTaskId1 = new CustomTaskId('t1')
		def taskId2 = new CustomTaskId('t2')
		def task1 = new Task<CustomTaskId>(taskId1)
		def equalTask1 = new Task<CustomTaskId>(equalTaskId1)
		def task2 = new Task<CustomTaskId>(taskId2)

		when:
		def result = task1.compareTo(equalTask1)
		then:
		result == 0

		when:
		result = equalTask1.compareTo(task1)
		then:
		result == 0

		when:
		result = task1.compareTo(task2)
		then:
		result == -1

		when:
		result = task2.compareTo(task1)
		then:
		result == -1
	}

	@CompileStatic
	@EqualsAndHashCode
	private static class CustomTaskId {

		final String internalId

		CustomTaskId(final String internalId) {
			this.internalId = internalId
		}
	}

	@CompileStatic
	@Sortable
	@EqualsAndHashCode
	private static class CustomComparableTaskId {

		final String internalId

		CustomComparableTaskId(final String internalId) {
			this.internalId = internalId
		}
	}
}
