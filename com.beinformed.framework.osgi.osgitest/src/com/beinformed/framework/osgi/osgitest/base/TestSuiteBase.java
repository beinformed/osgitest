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
package com.beinformed.framework.osgi.osgitest.base;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestSuite;

/**
 * Abstract base implementation for TestSuites reflection based. <br />
 * Subclasses can call addTest() methods with as argument the method accepting
 * the TestCaseMonitor as only argument.
 * 
 * @since 4.0
 */
public abstract class TestSuiteBase implements TestSuite {

	private final transient Map<TestCase, Method> testCases = new HashMap<TestCase, Method>();
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSuiteBase.class);

	private final String label;

	public TestSuiteBase(String label) {
		this.label = label;
		initialize();
	}

	public TestSuiteBase(Class<?> clazz) {
		this(StringUtils.replace(clazz.getName(), ".", "/"));
	}

	protected void initialize() {
		// Adds all methods that have a name that starts with 'test' and have only one argument of type TestMonitor
		for (Method method : this.getClass().getMethods()) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length == 1 && parameterTypes[0] == TestMonitor.class && method.getName().startsWith("test")) {
				addTest(method.getName());
			}
		}
	}

	/**
	 * Adds test meta data with the given identifier as identifier and label.
	 * 
	 * @param testMethod
	 *            The name of the test method. This method must accept one
	 *            argument of type {@link TestMonitor}.
	 */
	protected final void addTest(final String testMethod) {
		addTest(testMethod, testMethod);
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Adds test meta data with the given identifier and label.
	 * 
	 * @param testMethod
	 *            The name of the test method. This method must accept one
	 *            argument of type {@link TestMonitor}.
	 * @param label
	 *            The label of the test case. If the label is {@code null}, the
	 *            identifier will be used as a label.
	 */
	protected final void addTest(final String testMethod, final String label) {
		synchronized (testCases) {
			final TestCase testCase = new TestMetaData(testMethod, label);
			if (testCases.containsKey(testCase)) {
				LOGGER.warn("Test method '{}' already exists in this TestSuite. The existing test case will be overridden.", testMethod);
			}
			Method method;
			try {
				method = this.getClass().getMethod(testMethod, TestMonitor.class);
				testCases.put(testCase, method);
			} catch (Exception e) {
				LOGGER.error("Could not add test method " + testMethod, e);
			}
		}
	}

	/**
	 * Removes the given test case.
	 * 
	 * @param testCase
	 *            the meta data of the test case to remove from this TestSuite.
	 */
	protected final void removeTest(final TestCase testCase) {
		removeTest(testCase.getIdentifier());
	}

	/**
	 * Removes the given test case.
	 * 
	 * @param identifier
	 *            the identifier of the test case to remove from this TestSuite.
	 */
	protected final void removeTest(final String identifier) {
		Method removedTestMethod = null;
		synchronized (testCases) {
			removedTestMethod = testCases.remove(new TestMetaData(identifier));
		}
		if (removedTestMethod == null) {
			LOGGER.warn("Test case with identifier '{}' does not exist in this TestSuite. Test case could not be removed.", identifier);
		}
	}

	@Override
	public final List<TestCase> getTestCases() {
		synchronized (testCases) {
			return new ArrayList<TestCase>(testCases.keySet());
		}
	}

	@Override
	public final void test(final String testCaseId, final TestMonitor monitor) {
		Method testMethod = null;
		final TestCase testCase = new TestMetaData(testCaseId);
		synchronized (testCases) {
			testMethod = testCases.get(testCase);
		}
		if (testMethod == null) {
			final String message = "Could not execute test with testCaseId '" + testCaseId + "' because there is no such test case present in this testSuite.";
			monitor.error(message, null);
			return;
		}

		test(testMethod, testCase, monitor);
	}

	private void test(final Method testMethod, final TestCase testCase, final TestMonitor monitor) {
		try {
			LOGGER.debug("Starting test: " + testCase.getIdentifier());
			testMethod.invoke(this, monitor);
			LOGGER.debug("Finished test: " + testCase.getIdentifier());
		} catch (Exception e) {
			final String message = "Could not execute test case " + testCase;
			monitor.error(message, e);
		}
	}

}
