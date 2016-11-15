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

import java.util.List;

/**
 * Service interface to execute tests. <br />
 * TestSuite is a capability interface that classes can implement when they want
 * the test framework to test something. <br />
 * TestRunner implementations are responsible for executing the actual tests.
 * 
 * @see TestRunner
 */
public interface TestSuite {

	/**
	 * @return A list of all test cases for this TestSuite
	 */
	List<TestCase> getTestCases();
	
	/**
	 * Runs the given testCase
	 * 
	 * @param testCaseId
	 *            the test case to execute
	 * @param monitor
	 *            the call back monitor to accept test results.
	 */
	void test(String testCaseId, TestMonitor monitor);
	
	/**
	 * @return A descriptive label for this TestSuite
	 * @return
	 */
	String getLabel();
}
