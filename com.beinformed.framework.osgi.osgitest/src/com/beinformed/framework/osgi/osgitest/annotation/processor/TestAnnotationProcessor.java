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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.ServiceReference;

import com.beinformed.framework.osgi.osgitest.annotation.Dependency;
import com.beinformed.framework.osgi.osgitest.annotation.TestCase;
import com.beinformed.framework.osgi.osgitest.annotation.TestSuite;

/**
 * A {@code TestAnnotationProcessor} instance listens white-board style to all registered services
 * and creates a {@code TestSuite} for every service that is annotated with the {@code TestSuite} annotation.
 * Furthermore, for every method in the service instance that is annotated with the {@code TestCase} annotation
 * a {@code TestCase} is created.
 */
public class TestAnnotationProcessor {

	private volatile DependencyManager manager;

	private final Map<ServiceReference, Component> services;

	/**
	 * Default {@code TestAnnotationProcessor} constructor.
	 */
	public TestAnnotationProcessor() {
		this.services = new HashMap<ServiceReference, Component>();
	}

	/**
	 * Call back method that is called when a service is added.
	 * When the service is annotated with {@code TestSuite} annotation
	 * a {@code TestSuite} is created.
	 * 
	 * @param	ref
	 * 			a reference to the service that is being added.
	 * @param	service
	 * 			the new service that is added.
	 */
	public void add(ServiceReference ref, Object service) {
		if (service.getClass().getAnnotation(TestSuite.class) != null) {
			Component s = manager.createComponent().setInterface(com.beinformed.framework.osgi.osgitest.TestSuite.class.getName(), null)
					.setImplementation(createTestSuite(service));
			handleDependencies(s, service);
			services.put(ref, s);
			manager.add(s);
		}
	}
	
	private void handleDependencies(Component component, Object service){
		Dependency annotation = service.getClass().getAnnotation(Dependency.class);
		if (annotation != null){
			ServiceDependency serviceDependency = manager.createServiceDependency();
			String filter = annotation.filter();
			if (StringUtils.isNotEmpty(filter)){
				serviceDependency.setService(annotation.serviceName(), filter);
			} else {
				serviceDependency.setService(annotation.serviceName());
			}
			serviceDependency.setRequired(annotation.mandatory());
			
			String added = StringUtils.isNotEmpty(annotation.addedCallbackMethod()) ? annotation.addedCallbackMethod() : null ;
			String changed = StringUtils.isNotEmpty(annotation.changedCallbackMethod()) ? annotation.changedCallbackMethod() : null ;
			String removed = StringUtils.isNotEmpty(annotation.removedCallbackMethod()) ? annotation.removedCallbackMethod() : null ;
			serviceDependency.setCallbacks(service, added, changed, removed);

			component.add(serviceDependency);
		}
	}

	/**
	 * Call back method that is called when a service is removed.
	 * When a service is annotated with a {@code TestSuite} annotation
	 * the created {@code TestSuite} is also removed.
	 * 
	 * @param	ref
	 * 			a reference to the service that is being removed.
	 * @param	service
	 * 			the service that is removed.
	 */
	public void remove(ServiceReference ref, Object service) {
		if (service.getClass().getAnnotation(TestSuite.class) != null) {
			Component s = services.remove(ref);
			manager.remove(s);
		}
	}

	/**
	 * Creates a {@code TestSuiteImpl} instance for the given service parameter.
	 * @param	service
	 * 			the service to create a {@code TestSuite} for.
	 * @return	an initialized {@code TestSuiteImpl} instance.
	 */
	private TestSuiteImpl createTestSuite(Object service) {
		TestSuiteImpl testSuite = new TestSuiteImpl();
		Class<?> c = service.getClass();
		String label = c.getAnnotation(TestSuite.class).label();
		testSuite.setLabel(label);
		Method[] declaredMethods = c.getDeclaredMethods();
		for (Method m : declaredMethods) {
			Annotation[] methodAnnotations = m.getAnnotations();
			for (Annotation a : methodAnnotations) {
				if (a instanceof TestCase) {
					TestCase testAnnotation = (TestCase) a;
					TestCaseImpl testCase = new TestCaseImpl();
					boolean valid = true;
					testCase.setIdentifier(testAnnotation.identifier());
					testCase.setLabel(testAnnotation.label());
					testCase.setInstance(service);
					testCase.setMethod(m);
					if (valid) {
						testSuite.addTestCase(testCase);
					}
				}
			}
		}
		return testSuite;
	}

}