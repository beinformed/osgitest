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
package com.beinformed.framework.osgi.frameworkstate;

/**
 * The FrameworkState service offers methods to tell the system it's busy and therefore should not yet me made available or be made unavailable for
 * interaction with external actors, i.e. availability of the web and the service channels. There are two ways to report busy state to the 
 * system state service. The first is to register mark a unit of work though the startWork and endWork methods. The system state will not be 
 * set to available as long as the endWork for a unit of work has not been called. 
 * 
 * Components can be informed on the actual system state through the {@link FrameworkStateListener} capability.
 */
public interface FrameworkStateService {

	/**
	 * Indicates the start of a unit of work. This marks the system being unavailable, at least until the endWork method is called.
	 * 
	 * @param callerReference
	 *            Identification of the caller.
	 * @return identification of the component that starts the work. Must also
	 *         be provided when calling {@link #endWork(Object)}
	 */
	Token startWork(Object callerReference);

	/**
	 * Indicates the end of a unit of work. This tells the FrameworkState service the system could be made available again.
	 * The caller must pass the token it obtained when calling the startWork method. 
	 * @param token the token identifying the unit of work.
	 */
	void endWork(Token token);

}