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
package com.beinformed.framework.osgi.osgitest.annotation.processor;

import java.lang.reflect.Method;

import com.beinformed.framework.osgi.osgitest.TestCase;
import com.beinformed.framework.osgi.osgitest.TestMonitor;


/**
 * A {@code TestCase} implementation.
 */
public class TestCaseImpl implements TestCase {

	private String m_identifier;

	private String m_label;

	private Object m_object;

	private Method m_method;

	@Override
	public String getIdentifier() {
		return m_identifier;
	}

	@Override
	public String getLabel() {
		return m_label;
	}

	/**
	 * @param	identifier
	 * 			the new identifier of this {@code TestCase}. 
	 */
	protected void setIdentifier(String identifier) {
		m_identifier = identifier;
	}

	/**
	 * @param	label
	 * 			the new label of this {@code TestCase}. 
	 */
	protected void setLabel(String label) {
		m_label = label;
	}

	/**
	 * @param	object
	 * 			the service to invoke the method on. 
	 */
	protected void setInstance(Object object) {
		m_object = object;
	}

	/**
	 * @param	method
	 * 			the test method to invoke.
	 */
	protected void setMethod(Method method) {
		m_method = method;
	}

	/**
	 * Invokes the specified test method on the instance object and passes the monitor along.
	 * @param	monitor
	 * 			the monitor to pass onto the invoked test method.
	 */
	protected void invoke(TestMonitor monitor) {
		try {
			m_method.invoke(m_object, monitor);
		} catch (Exception exception) {
			monitor.error("Test failed.", exception);
		}
	}

}