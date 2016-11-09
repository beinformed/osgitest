/*
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
package com.beinformed.framework.osgi.osgitest.samples.base;

import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestSuiteLifecycle;
import com.beinformed.framework.osgi.osgitest.base.TestSuiteBase;

public class MyBaseClassBasedLifecycleTestSuite extends TestSuiteBase implements TestSuiteLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(MyBaseClassBasedLifecycleTestSuite.class);
	private volatile Object injected;

	public MyBaseClassBasedLifecycleTestSuite(String label) {
		super(label);
		addTest("myTestMethod", "My test method (lifecycle)");
	}
	
	public void myTestMethod(TestMonitor monitor) {
		monitor.assertion(injected != null, "Injected object is null");
		monitor.assertion(injected instanceof MyBaseClassBasedLifecycleTestSuite, "Injected object is of an unexpected type.");
		monitor.assertion(true, "The world is ending (lifecycle). ");
	}

	@Override
	public void setup(final DependencyManager dependencyManager) {
		LOGGER.info("setup...");
		new Thread(() -> {
			LOGGER.info("pausing component publishing...");
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
			}
			LOGGER.info("publising component...");
			Properties properties = new Properties();
			properties.setProperty("key", "bla");
			dependencyManager.add(dependencyManager.createComponent()
					.setImplementation(this)
					.setInterface(Object.class.getName(), properties));
		}).start();
	}

	@Override
	public void declareDependencies(Component component, DependencyManager dependencyManager) {
		LOGGER.info("declare dependencies...");
		component.add(dependencyManager.createServiceDependency()
				.setService(Object.class, "(key=bla)")
				.setRequired(true));
		
	}

	@Override
	public void initializeTestSuite() {
		LOGGER.info("initialize...");
		
	}

	@Override
	public void cleanupTestSuite() {
		LOGGER.info("cleanup...");
	}

}
