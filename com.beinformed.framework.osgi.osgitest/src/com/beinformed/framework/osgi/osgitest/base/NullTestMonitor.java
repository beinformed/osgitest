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

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestSuite;

/**
 * Null test monitor implementation.
 */
public class NullTestMonitor implements TestMonitor {
	@Override
	public void error(String message, Throwable exception) {

	}

	@Override
	public void endTest(TestCase testCase) {
	}

	@Override
	public void beginTest(TestCase testCase) {
	}

	@Override
	public void assertion(boolean condition, String messageOnFailure) {

	}

	@Override
	public void beginTestRun() {
	}

	@Override
	public void endTestRun() {
	}

	@Override
	public void beginTestSuite(TestSuite suite) {
		
	}

	@Override
	public void endTestSuite(TestSuite suite) {
		
	}
}