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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestRunner;
import com.beinformed.framework.osgi.osgitest.TestSuite;
import com.beinformed.framework.osgi.osgitest.TestSuiteLifecycle;
import com.beinformed.framework.osgi.osgitest.base.NullTestMonitor;

/**
 * Default test runner implementation. <br />
 * Test runner acts also as a Job. <br />
 * Manageable settings are: <br />
 */
public class DefaultTestRunner implements TestRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTestRunner.class);

	private final Map<ServiceReference, TestSuite> testSuites = new ConcurrentHashMap<ServiceReference, TestSuite>();

	private final Map<ServiceReference, TestMonitor> testMonitors = new ConcurrentHashMap<ServiceReference, TestMonitor>();

	private boolean deploymentTestingEnabled = false;

	private ExecutorService runTestsExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private String testSuitesConcurrent;

	private final TestMonitor monitor = new CompositeTestMonitor();

	private int nrOfWarmUpRuns = 0;

	private int nrOfTestRuns = 1;

	private AllTestSuitesAvailableAsserter allTestSuitesAvailableAsserter = new AllTestSuitesAvailableAsserter();
	
	private volatile DependencyManager dependencyManager;
	
	public DefaultTestRunner() {
		String deploymentTestingEnabledString = System.getProperty("osgitest.deploymentTestEnabled");
		if (deploymentTestingEnabledString != null) {
			deploymentTestingEnabled = Boolean.parseBoolean(deploymentTestingEnabledString);
		}
		LOGGER.debug("Deployment testing enabled: {}", deploymentTestingEnabled);
	}
	
	public void addTestSuite(ServiceReference reference, TestSuite testSuite) {
		LOGGER.debug("Add test suite {}", testSuite.getLabel());
		testSuites.put(reference, testSuite);

		if (deploymentTestingEnabled) {
			executeTest(testSuite);
		}
	}

	public void removeTestSuite(ServiceReference reference, TestSuite testSuite) {
		LOGGER.debug("Remove test suite {}", testSuite.getLabel());
		testSuites.remove(reference);
	}

	public void swapTestSuite(ServiceReference previousReference, TestSuite previousTestSuite, ServiceReference currentReference, TestSuite currentTestSuite) {
		removeTestSuite(previousReference, previousTestSuite);
		addTestSuite(currentReference, currentTestSuite);
	}

	public void addTestMonitor(ServiceReference reference, TestMonitor testMonitor) {
		LOGGER.debug("Add test monitor {}", testMonitor.getClass().getName());
		testMonitors.put(reference, testMonitor);
	}

	public void removeTestMonitor(ServiceReference reference, TestMonitor testMonitor) {
		LOGGER.debug("Remove test monitor {}", testMonitor.getClass().getName());
		testMonitors.remove(reference);
	}

	public void swapTestMonitor(ServiceReference previousReference, TestMonitor previousTestMonitor, ServiceReference currentReference,
			TestMonitor currentTestMonitor) {
		removeTestMonitor(previousReference, previousTestMonitor);
		addTestMonitor(currentReference, currentTestMonitor);
	}

	public void executeTests() {
		List<TestSuite> testSuitesCopy = new ArrayList<TestSuite>(testSuites.values());
		Collections.sort(testSuitesCopy, new TestSuiteComparator());

		LOGGER.debug("Executing tests (number of testsuites= {})", testSuitesCopy.size());

		LOGGER.debug("Current number of warmup runs {}", nrOfWarmUpRuns);
		LOGGER.debug("Current number of test runs {}", nrOfTestRuns);
		monitor.beginTestRun();
		allTestSuitesAvailableAsserter.assertAllTestSuitesAvailable(monitor);
		try {
			for (TestSuite testSuite : testSuitesCopy) {
				handleWarmUp(nrOfWarmUpRuns, testSuite);

				for (int i = 0; i < nrOfTestRuns; i++) {
					LOGGER.debug("Executing testsuite {} ({})", new Object[] { testSuite.getLabel(), i + 1 });
					executeTest(testSuite);
				}
			}
		} catch (Throwable t) {
			monitor.error("Exception while running test run", t);
		} finally {
			monitor.endTestRun();
		}
	}

	private void handleWarmUp(int nrOfWarmUpRuns, TestSuite suite) {
		if (nrOfWarmUpRuns > 0) {
			TestMonitor nullMonitor = new NullTestMonitor();
			for (TestCase testCase : suite.getTestCases()) {
				suite.test(testCase.getIdentifier(), nullMonitor);
			}
		}
	}

	public boolean executeTestSuite(String testSuiteLabel) {
		TestSuite testSuite = findTestSuiteByLabel(testSuiteLabel);
		if (testSuite != null) {
			monitor.beginTestRun();
			executeTest(testSuite);
			monitor.endTestRun();
			return true;
		} else {
			LOGGER.info("No Test suite found with label: {}", testSuiteLabel);
			return false;
		}
	}

	private TestSuite findTestSuiteByLabel(String testSuiteLabel) {
		List<TestSuite> testSuitesCopy = new ArrayList<TestSuite>(testSuites.values());
		for (TestSuite testSuite : testSuitesCopy) {
			String label = testSuite.getLabel();
			if (label != null && label.equals(testSuiteLabel)) {
				return testSuite;
			}
		}
		return null;
	}

	private void executeTest(TestSuite testSuite) {
		// check for TestSuiteLifecycle
		DependencyManager lifecycleDependencyManager = null;
		Component lifecycleWiringComponent = null;
		boolean runInParallel = isTestSuiteConcurrent(testSuite.getLabel());

		monitor.beginTestSuite(testSuite);
		if (testSuite instanceof TestSuiteLifecycle) {
			TestSuiteLifecycle testSuiteLifecycle = (TestSuiteLifecycle) testSuite;
			lifecycleDependencyManager = new DependencyManager(dependencyManager.getBundleContext());
			TestSuiteWiringService wiringService = new TestSuiteWiringService(testSuiteLifecycle);
			lifecycleWiringComponent = dependencyManager.createComponent()
					.setImplementation(wiringService)
					.setComposition("getComposition")
					.setAutoConfig(Component.class, false)
					.setAutoConfig(BundleContext.class, false)
					.setAutoConfig(ServiceRegistration.class, false)
					.setAutoConfig(DependencyManager.class, false)
					.setCallbacks(wiringService, null, "start", null, null);
			testSuiteLifecycle.setup(lifecycleDependencyManager);
			
			testSuiteLifecycle.declareDependencies(lifecycleWiringComponent, dependencyManager);
			dependencyManager.add(lifecycleWiringComponent);
			
			if (!wiringService.await(30, TimeUnit.SECONDS)) {
				monitor.error("Test lifecycle wiring timed out. Dependencies were not be satisfied.", null);
				dependencyManager.remove(lifecycleWiringComponent);
				monitor.endTestSuite(testSuite);
				return;
			}
			
			((TestSuiteLifecycle) testSuite).initializeTestSuite();
		}

		try {
			for (final TestCase testCase : testSuite.getTestCases()) {
				executeTestCase(testSuite, runInParallel, testCase);
			}
		} catch (Throwable t) {
			monitor.error("Exception while running test suite", t);
		} finally {
			monitor.endTestSuite(testSuite);
			if (testSuite instanceof TestSuiteLifecycle) {
				TestSuiteLifecycle lifecycle = (TestSuiteLifecycle) testSuite;
				lifecycle.cleanupTestSuite();
				lifecycleDependencyManager.clear(); // teardown all configuration
				dependencyManager.remove(lifecycleWiringComponent);
			}
		}
	}
	
	static class TestSuiteWiringService {
		
		private TestSuiteLifecycle testSuiteLifecycle;
		private CountDownLatch latch;

		public TestSuiteWiringService(TestSuiteLifecycle testSuiteLifecycle) {
			this.testSuiteLifecycle = testSuiteLifecycle;
			this.latch = new CountDownLatch(1);
		}
		
		Object[] getComposition() {
			return new Object[] { testSuiteLifecycle };
		}
		
		void start() {
			latch.countDown();
		}
		
		boolean await(long timeout, TimeUnit unit) {
			try {
				return latch.await(timeout, unit);
			} catch (InterruptedException e) {
				return false;
			}
		}
	}
	
	private void executeTestCase(final TestSuite testSuite, boolean runInParallel, final TestCase testCase) {
		monitor.beginTest(testCase);
		try {
			final String testCaseId = testCase.getIdentifier();
			if (runInParallel) {
				runTestsExecutorService.execute(new Runnable() {
					@Override
					public void run() {
						testSuite.test(testCaseId, monitor);
					}
				});
			} else {
				testSuite.test(testCaseId, monitor);
			}
		} catch (Throwable t) {
			monitor.error("Exception while running test case", t);
		} finally {
			monitor.endTest(testCase);
		}
	}

	public final String getTestSuites() {
		List<String> testSuiteLabels = new ArrayList<String>();
		List<TestSuite> testSuitesCopy = new ArrayList<TestSuite>(testSuites.values());
		for (TestSuite testSuite : testSuitesCopy) {
			testSuiteLabels.add(testSuite.getLabel());
		}
		return StringUtils.join(testSuiteLabels, ',');
	}

	public String getTestMonitorClassName() {
		return monitor.getClass().getName();
	}

	public String getTestSuitesConcurrent() {
		return testSuitesConcurrent;
	}

	/**
	 * Comma separated list of test suites (defined by label) which tests must
	 * be run in parallel.
	 */
	public String[] getTestSuitesConcurrentList() {
		return testSuitesConcurrent.split(",");
	}

	private boolean isTestSuiteConcurrent(String testSuiteLabel) {
		if (testSuitesConcurrent != null) {
			List<String> testSuitesConcurrentList = Arrays.asList(testSuitesConcurrent.split(","));
			return testSuitesConcurrentList.contains(testSuiteLabel);
		} else {
			return false;
		}
	}

	/**
	 * Comma separated list of test suites (defined by label) which tests must
	 * be run in parallel.
	 */
	public void setTestSuitesConcurrent(String testSuitesConcurrent) {
		this.testSuitesConcurrent = testSuitesConcurrent;
	}

	public void setNrOfWarmUpRuns(int nrOfWarmUpRuns) {
		this.nrOfWarmUpRuns = nrOfWarmUpRuns;
	}

	public void setNrOfTestRuns(int nrOfTestRuns) {
		this.nrOfTestRuns = nrOfTestRuns;
	}

	public int getNrOfTestRuns() {
		return nrOfTestRuns;
	}

	public int getNrOfWarmUpRuns() {
		return nrOfWarmUpRuns;
	}

	private class CompositeTestMonitor implements TestMonitor {

		@Override
		public void beginTestRun() {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.beginTestRun();
			}
		}

		@Override
		public void beginTestSuite(TestSuite suite) {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.beginTestSuite(suite);
			}
		}

		@Override
		public void beginTest(TestCase testCase) {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.beginTest(testCase);
			}
		}

		@Override
		public void assertion(boolean condition, String messageOnFailure) {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.assertion(condition, messageOnFailure);
			}
		}

		@Override
		public void error(String message, Throwable exception) {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.error(message, exception);
			}
		}

		@Override
		public void endTest(TestCase testCase) {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.endTest(testCase);
			}
		}

		@Override
		public void endTestSuite(TestSuite suite) {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.endTestSuite(suite);
			}
		}

		@Override
		public void endTestRun() {
			for (TestMonitor monitor : testMonitors.values()) {
				monitor.endTestRun();
			}
		}

	}

	private class TestSuiteComparator implements Comparator<TestSuite> {

		@Override
		public int compare(TestSuite t0, TestSuite t1) {
			if (t0.getLabel() == null && t1.getLabel() == null) {
				return 0;
			} else if (t0.getLabel() == null) {
				return -1;
			} else if (t1.getLabel() == null) {
				return 1;
			} else {
				return t0.getLabel().compareToIgnoreCase(t1.getLabel());
			}
		}
	}

}
