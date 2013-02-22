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
package com.beinformed.framework.osgi.osgitest.samples.annotation;

import com.beinformed.framework.osgi.osgitest.TestMonitor;
import com.beinformed.framework.osgi.osgitest.annotation.TestCase;
import com.beinformed.framework.osgi.osgitest.annotation.TestSuite;

@TestSuite(label="My annotation based testsuite")
public class MyAnnotationBasedTestSuite {

	@TestCase(identifier="annotation-1", label="First annotation based test")
	public void testAnnotationTestCase(TestMonitor monitor) {
		monitor.assertion(true, "The world is ending.");
	}
}
