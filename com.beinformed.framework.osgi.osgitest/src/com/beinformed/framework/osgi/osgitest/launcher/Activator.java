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
package com.beinformed.framework.osgi.osgitest.launcher;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.beinformed.framework.osgi.frameworkstate.FrameworkStateListener;
import com.beinformed.framework.osgi.osgitest.TestRunner;

public class Activator extends DependencyActivatorBase {
	
	@Override
	public void init(BundleContext ctx, DependencyManager mgr)
			throws Exception {

		mgr.add(createComponent().setImplementation(TestLauncher.class)
				.setInterface(FrameworkStateListener.class.getName(), null)
				.add(createServiceDependency().setService(TestRunner.class).setRequired(true))
				.setCallbacks("init", null, null, null));
	}

	@Override
	public void destroy(BundleContext ctx, DependencyManager mgr)
			throws Exception {

	}

}
