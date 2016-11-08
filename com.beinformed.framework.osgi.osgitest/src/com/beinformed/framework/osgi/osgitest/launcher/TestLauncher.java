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

import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beinformed.framework.osgi.frameworkstate.FrameworkStateListener;
import com.beinformed.framework.osgi.osgitest.TestRunner;

public class TestLauncher implements FrameworkStateListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestLauncher.class);
	private volatile TestRunner testRunner;
	private volatile DependencyManager manager;
	private boolean shutdownOnFinish = false;
	private boolean hasRun = false;
	
	public TestLauncher() {
		String shutdownOnFinishProperty = System.getProperty("osgitest.shutdownOnFinish");
		if (shutdownOnFinishProperty != null) {
			shutdownOnFinish = Boolean.parseBoolean(shutdownOnFinishProperty);
		}
	}
	
	// DependencyManager lifecycle callback method
	@SuppressWarnings("unused")
	private void init() {
		LOGGER.info("Initialized TestLauncher, waiting for available framework state");
	}

	@Override
	public void onStarting() {
		
	}

	@Override
	public void onStopping() {
		
	}

	@Override
	public void onAvailable() {
		if (!hasRun) {
			LOGGER.info("Executing tests");
			testRunner.executeTests();
			hasRun = true;
			if (shutdownOnFinish) {
				try {
					LOGGER.info("Shutting down the framework");
					manager.getBundleContext().getBundle(0).stop();
				} catch (BundleException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onUnavailable() {
		
	}
	
	
}
