/**
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
package ch.petikoch.libs.jtwfg.assertion;

public class Preconditions {

	/**
	 * @param obj
	 * 		an object or null
	 * @param message
	 * 		or null
	 *
	 * @throws IllegalArgumentException
	 * 		in case of obj is null
	 */
	public static void checkArgumentNotNull(Object obj, String message) {
		if (null == obj) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @param condition
	 * 		a condition which must be fulfilled
	 * @param message
	 * 		or null
	 *
	 * @throws IllegalArgumentException
	 * 		in case of condition is false
	 */
	public static void checkArgument(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}
}
