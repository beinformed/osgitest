# OSGi Service testing

This framework facilitates the testing of live OSGi services runnning in an OSGi framework. 

## Usage
The framework offers two methods for defining test suites.

### Create test suites using annotations
The framework provides annotations for developing test suites. Simply annotating a service implementation is sufficient for it to be discovered as test suite by the framework.

	@TestSuite(label="MyTestSuite")
	public class MyTestSuiteImplementation {

		@TestMethod(identifier="positive", label="My positive scenario")
		public void doTestMyPositiveScenario(TestMonitor monitor) {
			monitor.assertion(eval, "Failure message");
		}
	}

### Implement a TestSuite Service Interface
Besides using the annotations one can also implement the TestSuite interface directly. For the framework to be able to discover the testsuite it has to be published with the TestSuite service interface.

### Executing tests
The framework contains a default test runner that executes any test suite on discovery in the service registry.
Test results are logged to the console.

This test runner can be used in two ways. 
It can be configured to launch tests on discovery. This can be enabled through setting the ``osgitest.deploymentTestEnabled`` system property to true.

You can also use the TestLauncher to execute the tests. It will wait until the framework is ready (no more services are being published) and execute all available tests. The launcher can be configured to shutdown the framework on test completion. This is enabled through setting the ``osgitest.shutdownOnFinish`` system property to true.

### Implement Custom Test Runners and launchers
To develop more advanced running and reporting of test results one can implement custom test runners and launchers.

## Contents
The framework consists of the following bundles
<table>
	<tr>
		<th>Bundle</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>com.beinformed.framework.osgi.osgitest.api</td>
		<td>Framework API's.</td>
	</tr>
	<tr>
		<td>com.beinformed.framework.osgi.osgitest.annotationprocessor</td>
		<td>Runtime annotation processor for test framework annotations.</td>
	</tr>
	<tr>
		<td>com.beinformed.framework.osgi.osgitest.loggingmonitor</td>
		<td>Default test monitor implementation which logs all test results to the log.</td>
	</tr>	
	<tr>
		<td>com.beinformed.framework.osgi.osgitest.testrunner</td>
		<td>Default test runner service implementation.</td>
	</tr>		
	<tr>
		<td>com.beinformed.framework.osgi.osgitest.launcher</td>
		<td>Default launcher which waits for system availability and executes all tests.</td>
	</tr>		
	<tr>
		<td>com.beinformed.framework.osgi.frameworkstate.api</td>
		<td>Service API's for detecting framework availability.</td>
	</tr>		
	<tr>
		<td>com.beinformed.framework.osgi.frameworkstate.entropy</td>
		<td>Entropy based framework availability detection implementation.</td>
	</tr>		
	<tr>
		<td>osgitest.samples</td>
		<td>Example test implementations.</td>
	</tr>		
</table>

## Getting started
### Requirements ###
The following bundle equirements apply:

*  ``ch.qos.logback.core [1.0.0,1.0.1)``
*  ``ch.qos.logback.classic [1.0.0,1.0.1)``
*  ``org.apache.commons.lang [2.6.0,2.6.1)``
*  ``org.apache.felix.configadmin [1.4.0,1.4.1)``
*  ``org.apache.felix.dependencymanager [3.1.0,3.1.1)``
*  ``org.apache.felix.metatype [1.0.4,1.0.5)``
*  ``slf4j.api [1.6.2,1.6.3)``

### Running the samples

Download the standard Apache Felix framework distribution. Unzip and copy the release bundles to the ``bundle`` folder.

Open a terminal

``cd felix-framework-<version>`` 

``java -Dosgitest.shutdownOnFinish=true -jar ./bin/felix.jar``

### Build the framework
The framework can be build using Eclipse and [Bndtools](http://www.bndtools.org).

## License
This software is available under the Apache 2.0 License. The license is available in the LICENSE file or at [http://www.apache.org/licenses/](http://www.apache.org/licenses/).

## Contribute
If you want to contribute please contact Xander Uiterlinden 

Github: ``uiterlix``

E-mail: ``xander.uiterlinden@gmail.com``

