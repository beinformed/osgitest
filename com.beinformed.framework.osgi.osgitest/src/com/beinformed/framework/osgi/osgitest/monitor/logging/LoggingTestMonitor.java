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
package com.beinformed.framework.osgi.osgitest.monitor.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestSuite;

/**
 * TestSuiteMonitor implementation that outputs info as info log statements using the configured log framework.
 */
public class LoggingTestMonitor implements TestMonitor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingTestMonitor.class);

	private boolean testPassed = true;

	boolean allTestsPassed = true;

	private int assertions = 0;

	private int totalAssertions = 0;

	private int failCount = 0;

	private int totalFailCount = 0;

	private int failedTestCases = 0;

	private int totalTestCases = 0;

	@Override
	public void beginTest(TestCase testCase) {
		LOGGER.info("== Begin test {} '{}'", testCase.getIdentifier(), testCase.getLabel());
		testPassed = true;
		assertions = 0;
		failCount = 0;
		totalTestCases++;
	}

	@Override
	public void assertion(boolean condition, String messageOnFailure) {
		registerAssertion(condition, messageOnFailure, null);
	}

	@Override
	public void error(String message, Throwable exception) {
		registerAssertion(false, message, exception);
	}

	@Override
	public void endTest(TestCase testCase) {
		if (!testPassed) {
			failedTestCases++;
		}
		totalAssertions += assertions;
		totalFailCount += failCount;

		if (testPassed()) {
			LOGGER.info("\tAll {} assertions passed for testcase {} '{}'", new Object[] { getAssertionCount(), testCase.getIdentifier(), testCase.getLabel() });
		} else {
			LOGGER.info("\t{} of {} assertions failed for testcase {} '{}'", new Object[] { getFailCount(), getAssertionCount(), testCase.getIdentifier(),
					testCase.getLabel() });
		}

		LOGGER.info("== End test {} '{}'", testCase.getIdentifier(), testCase.getLabel());
	}

	private void registerAssertion(boolean passed, String messageOnFailure, Throwable exception) {
		assertions++;
		if (!passed) {
			testPassed = false;
			allTestsPassed = false;
			failCount++;
			if (exception == null) {
				LOGGER.error("\t\t" + messageOnFailure);
			} else {
				LOGGER.error("\t\t" + messageOnFailure, exception);
			}
		}
	}

	public boolean testPassed() {
		return testPassed;
	}

	public int getFailCount() {
		return failCount;
	}

	public int getTotalFailCount() {
		return totalFailCount;
	}

	public int getAssertionCount() {
		return assertions;
	}

	public int getTotalAssertions() {
		return totalAssertions;
	}

	public int getFailedTestCases() {
		return failedTestCases;
	}

	public int getTotalTestCases() {
		return totalTestCases;
	}

	private void init() {
		testPassed = true;
		allTestsPassed = true;
		assertions = 0;
		totalAssertions = 0;
		failCount = 0;
		totalFailCount = 0;
		failedTestCases = 0;
		totalTestCases = 0;
	}

	@Override
	public void beginTestSuite(TestSuite suite) {
		init();

		LOGGER.info("= START =====================================================================");
		LOGGER.info("Testing: {}", suite.getLabel());
		LOGGER.info("=============================================================================");

	}

	@Override
	public void endTestSuite(TestSuite suite) {
		LOGGER.info("=============================================================================");

		if (allTestsPassed) {
			LOGGER.info("ALL TESTS PASSED for suite {}", suite.getLabel());
		} else {
			LOGGER.info("TESTS FAILED for suite {}", suite.getLabel());
		}
		LOGGER.info("\t {} of {} testcases failed.", new Object[] { getFailedTestCases(), getTotalTestCases(), });
		LOGGER.info("\t {} of {} assertions failed.", new Object[] { getTotalFailCount(), getTotalAssertions(), });

		LOGGER.info("= END =======================================================================");
	}

	@Override
	public void beginTestRun() {
		LOGGER.info("= BEGIN TESTRUN =============================================================");
	}

	@Override
	public void endTestRun() {
		LOGGER.info("= END TESTRUN ===============================================================");
	}

}