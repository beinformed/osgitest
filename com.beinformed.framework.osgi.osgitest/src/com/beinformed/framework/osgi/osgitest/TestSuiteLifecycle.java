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
package com.beinformed.framework.osgi.osgitest;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

/**
 * Lifecycle interface allowing a TestSuite to perform service initialization and wiring
 * as part of test suite execution. This prevents configurations and required services to be
 * running in the system that are solely needed for executing a test suite and have no other
 * significance in the system.
 * @author uiterlix
 */
public interface TestSuiteLifecycle {

	/**
	 * Setup configurations and services required for the test suite.
	 * @param dependencyManager
	 */
	public void setup(DependencyManager dependencyManager);
	
	/**
	 * Declare test suite dependencies.
	 * @param component
	 * @param dependencyManager
	 */
	public void declareDependencies(Component component, DependencyManager dependencyManager);
	
	/**
	 * Test suite initialization.
	 */
	public void initializeTestSuite();
	
	/**
	 * Test suite cleanup.
	 */
	public void cleanupTestSuite();
	
}
