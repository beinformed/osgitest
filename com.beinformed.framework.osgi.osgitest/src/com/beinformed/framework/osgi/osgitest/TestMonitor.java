/*
 *  Copyright 2012 Be Informed B.V.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.beinformed.framework.osgi.osgitest;

/**
 * Monitor interface to provide callback methods for test results
 */
public interface TestMonitor {

	/**
	 * Call back method to indicate the begin of a test run.
	 */
	void beginTestRun();

	/**
	 * Call back method to indicate the begin of a TestSuite.
	 * 
	 * @param suite
	 *            The TestSuite to run
	 */
	void beginTestSuite(TestSuite suite);

	/**
	 * Call back to indicate the start of a test case.
	 * 
	 * @param testCase
	 *            The meta data of the test case that is about to begin.
	 */
	void beginTest(TestCase testCase);

	/**
	 * Performs the given assertion. If the condition evaluates to false, the
	 * failure message applies.
	 * 
	 * @param condition
	 *            The condition to evaluate
	 * @param messageOnFailure
	 *            The message to be applied when the condition fails.
	 */
	void assertion(boolean condition, String messageOnFailure);

	/**
	 * Call back to indicate something went wrong.
	 * 
	 * @param message
	 *            The failure message.
	 * @param exception
	 *            An optional exception.
	 */
	void error(String message, Throwable exception);

	/**
	 * Call back to indicate the end of a test case.
	 * 
	 * @param testCase
	 *            The meta data of the test case that has ended.
	 */
	void endTest(TestCase testCase);

	/**
	 * Call back method to indicate the end of a TestSuite.
	 * 
	 * @param suite
	 *            The TestSuite that has ended
	 */
	void endTestSuite(TestSuite suite);

	/**
	 * Call back method to indicate the end of a test run.
	 */
	void endTestRun();
}
