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
package com.beinformed.framework.osgi.osgitest.samples;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.beinformed.framework.osgi.osgitest.TestSuite;
import com.beinformed.framework.osgi.osgitest.samples.annotation.MyAnnotationBasedTestSuite;
import com.beinformed.framework.osgi.osgitest.samples.base.MyBaseClassBasedLifecycleTestSuite;
import com.beinformed.framework.osgi.osgitest.samples.base.MyBaseClassBasedLifecycleTestSuiteWithUnresolvableDependency;
import com.beinformed.framework.osgi.osgitest.samples.base.MyBaseClassBasedTestSuite;
import com.beinformed.framework.osgi.osgitest.samples.coded.SimpleCodedTestSuite;

public class Activator extends DependencyActivatorBase {

	@Override
	public void init(BundleContext ctx, DependencyManager manager)
			throws Exception {
		
		manager.add(manager.createComponent().setInterface(TestSuite.class.getName(), null)
				.setImplementation(SimpleCodedTestSuite.class));
		
		manager.add(manager.createComponent().setInterface(MyAnnotationBasedTestSuite.class.getName(), null)
				.setImplementation(MyAnnotationBasedTestSuite.class));
		
		TestSuite testSuite = new MyBaseClassBasedTestSuite("My base class based testsuite");
		manager.add(manager.createComponent().setInterface(TestSuite.class.getName(), null)
				.setImplementation(testSuite));
		
		TestSuite lifecycleTestSuite = new MyBaseClassBasedLifecycleTestSuite("My base class lifecycle based testsuite");
		manager.add(manager.createComponent().setInterface(TestSuite.class.getName(), null)
				.setImplementation(lifecycleTestSuite));
		
		TestSuite lifecycleTestSuiteUnresolvableDependency = new MyBaseClassBasedLifecycleTestSuiteWithUnresolvableDependency("My base class lifecycle based testsuite with unresolvable dependency.");
		manager.add(manager.createComponent().setInterface(TestSuite.class.getName(), null)
				.setImplementation(lifecycleTestSuiteUnresolvableDependency));
	}
	
	@Override
	public void destroy(BundleContext ctx, DependencyManager manager)
			throws Exception {
		
	}

}
