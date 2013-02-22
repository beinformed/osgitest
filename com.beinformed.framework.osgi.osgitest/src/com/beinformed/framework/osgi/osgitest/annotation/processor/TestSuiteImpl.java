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
package com.beinformed.framework.osgi.osgitest.annotation.processor;

import java.util.ArrayList;
import java.util.List;

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestSuite;


/**
 * A {@code TestSuite} implementation.
 */
public class TestSuiteImpl implements TestSuite {

	private String label;

	private final List<TestCase> testCases;

	/**
	 * Default {@code TestSuiteImpl} constructor.
	 */
	public TestSuiteImpl() {
		this.testCases = new ArrayList<TestCase>();
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public List<TestCase> getTestCases() {
		return testCases;
	}

	@Override
	public void test(String testCaseId, TestMonitor monitor) {
		for (TestCase tc : testCases) {
			if (tc.getIdentifier().equals(testCaseId)) {
				TestCaseImpl impl = (TestCaseImpl) tc;
				impl.invoke(monitor);
			}
		}
	}

	/**
	 * Setter for the label member of this {@code TestSuite}.
	 * @param	label
	 * 			the new label to use for this {@code TestSuite}.
	 */
	protected void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Adds a {@code TestCaseImpl} to this {@code TestSuite}.
	 * @param	testCase
	 * 			the {@code TestCaseImpl} to add to this {@code TestSuite}.
	 */
	protected void addTestCase(TestCaseImpl testCase) {
		testCases.add(testCase);
	}

}