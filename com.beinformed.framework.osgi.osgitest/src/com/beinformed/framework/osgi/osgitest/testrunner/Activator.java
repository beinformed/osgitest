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

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestRunner;
import com.beinformed.framework.osgi.osgitest.TestSuite;

public class Activator extends DependencyActivatorBase {

	@Override
	public void init(BundleContext context, DependencyManager manager) throws Exception {

		manager.add(createComponent()
				.setInterface(TestRunner.class.getName(), null)
				.setImplementation(DefaultTestRunner.class).setCallbacks(null, null, null, null)
				.add(createServiceDependency().setService(TestSuite.class)
						.setCallbacks("addTestSuite", null, "removeTestSuite", "swapTestSuite")
						.setRequired(false))
				.add(createServiceDependency().setService(TestMonitor.class)
						.setCallbacks("addTestMonitor", null, "removeTestMonitor", "swapTestMonitor")
						.setRequired(true))
				);
	}

	@Override
	public void destroy(BundleContext context, DependencyManager manager) throws Exception {

	}
}
