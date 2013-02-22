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
package com.beinformed.framework.osgi.osgitest.samples.coded;

import java.util.Collections;
import java.util.List;

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.TestSuite;

public class SimpleCodedTestSuite implements TestSuite {
	
	@Override
	public void test(String testCaseId, TestMonitor monitor) {
		if (testCaseId.equals("coded-1")) {
			monitor.assertion(true, "The world is ending.");
		}
	}

	@Override
	public List<TestCase> getTestCases() {
		
		TestCase myFirstTestCodedTestCase = new TestCase() {

			@Override
			public String getIdentifier() {
				return "coded-1";
			}

			@Override
			public String getLabel() {
				return "First coded testcase";
			}
			
		};
		return Collections.singletonList(myFirstTestCodedTestCase);
	}

	@Override
	public String getLabel() {
		return "My simple coded testsuite";
	}

}
