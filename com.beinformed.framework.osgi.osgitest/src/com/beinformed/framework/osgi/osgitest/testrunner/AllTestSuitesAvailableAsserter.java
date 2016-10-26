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
package com.beinformed.framework.osgi.osgitest.testrunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.context.DependencyContext;

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestSuite;

/**
 * Asserts that there are no unavailable TestSuites due to missing dependencies.<br>
 * For each TestSuite with missing dependencies, it reports an error for a dummy TestCase in that test suite
 * stating the missing dependencies.<br>
 */
public class AllTestSuitesAvailableAsserter {

	/**
	 * Asserts that there are no unavailable TestSuites due to missing dependencies.
	 * @param monitor
	 */
	public void assertAllTestSuitesAvailable(TestMonitor monitor) {
		Map<String, List<String>> missingTestSuitesAndDeps = new LinkedHashMap<String, List<String>>();

		List<DependencyManager> managers = DependencyManager.getDependencyManagers();
		for (DependencyManager manager : managers) {
			for (Object componentObject : new ArrayList<Object>(manager.getComponents())) {
				Component component = (Component) componentObject;

				for (DependencyContext dependencyContext : ((ComponentContext)component).getDependencies()) {
					if (dependencyContext.isRequired() && !dependencyContext.isAvailable()) {
						String componentName = ((ComponentDeclaration) component).getName();
						if (componentName.contains(TestSuite.class.getName())) {
							addMissingTestSuite(missingTestSuitesAndDeps, component, dependencyContext);
						}

					}
				}
			}
		}

		if (!missingTestSuitesAndDeps.isEmpty()) {
			reportMissingTestSuites(monitor, missingTestSuitesAndDeps);
		}
	}

	/**
	 * Add the missing testsuite and the current missing dependency
	 * @param missingTestSuitesAndDeps
	 * @param component the component
	 * @param dependency the current missing dependency
	 * @param componentName
	 */
	private void addMissingTestSuite(Map<String, List<String>> missingTestSuitesAndDeps, Component component, DependencyContext dependencyContext) {
		TestSuite testSuite = getTestSuite(component);
		String componentName;
		if (testSuite != null) {
			componentName = testSuite.getLabel();
		} else {
			//failed to retrieve actual TestSuite from the component
			componentName = "Placeholder for TestSuite with missing dependencies"; //$NON-NLS-1$
		}
		List<String> missingDeps = missingTestSuitesAndDeps.get(componentName);
		if (missingDeps == null) {
			missingDeps = new ArrayList<String>();
			missingTestSuitesAndDeps.put(componentName, missingDeps);
		}
		missingDeps.add(dependencyContext.toString());
	}

	/**
	 * Add error to the monitor for each missing testsuite
	 * @param monitor
	 * @param missingTestSuitesAndDeps
	 */
	private void reportMissingTestSuites(TestMonitor monitor, Map<String, List<String>> missingTestSuitesAndDeps) {

		for (String missingSuite : missingTestSuitesAndDeps.keySet()) {
			TestCase missingTestCase = new MissingTestCase();
			TestSuite missingTestSuite = new MissingTestSuite(missingSuite, missingTestCase);
			monitor.beginTestSuite(missingTestSuite);
			monitor.beginTest(missingTestCase);
			String missing = StringUtils.join(missingTestSuitesAndDeps.get(missingSuite), ", \n"); //$NON-NLS-1$
			monitor.error("A test suite is missing due to the following missing dependencies:\n" + missing, null); //$NON-NLS-1$
			monitor.endTest(missingTestCase);
			monitor.endTestSuite(missingTestSuite);
		}
	}

	/**
	 * Retrieves the actual TestSuite from the component
	 * @param component
	 * @return the TestSuite or null if it could not be retrieved
	 */
	private TestSuite getTestSuite(Component component) {
		//Try to obtain the implementation using reflection...
		Field[] fields = component.getClass().getDeclaredFields();
		Object implementation = null;
		try {
			for (Field field : fields) {
				if (field.getName().equals("m_implementation")) { //$NON-NLS-1$
					field.setAccessible(true);
					implementation = field.get(component);
					field.setAccessible(false);
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		if (implementation instanceof TestSuite) {
			return (TestSuite) implementation;
		}
		return null;
	}

	/**
	 * A dummy Test Suite that represents a missing test suite used only to report an error to the monitor.
	 */
	private static class MissingTestSuite implements TestSuite {

		private final String missingSuite;

		private final TestCase missingTestCase;

		/**
		 * @param missingSuite
		 * @param missingTestCase
		 */
		public MissingTestSuite(String missingSuite, TestCase missingTestCase) {
			this.missingSuite = missingSuite;
			this.missingTestCase = missingTestCase;
		}

		@Override
		public void test(String testCaseId, TestMonitor monitor) {
			//this is a dummy, no actual testing performed
		}

		@Override
		public List<TestCase> getTestCases() {
			return Collections.singletonList(missingTestCase);
		}

		@Override
		public String getLabel() {
			return missingSuite;
		}
	}

	/**
	 * A dummy TestCase used only to report an error to the monitor.
	 */
	private static class MissingTestCase implements TestCase {

		@Override
		public String getIdentifier() {
			return "1"; //$NON-NLS-1$
		}

		@Override
		public String getLabel() {
			return "Missing dependencies"; //$NON-NLS-1$
		}
	}

}
