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
 * Interface for services that want to be notified of changes in the framework state.
 * The lifecycle is as follows:<br/>
 * When the framework is starting the state is set to STARTING and the onStarting callback is 
 * called on all {@link FrameworkStateListener} objects. Possible next states: AVAILABLE, UNAVAILABLE, STOPPING.<br/>
 * When according to the algorithm used by the {@link SystemStateService} the system is AVAILABLE
 * the onAvailable callback is called. Possible next states: UNAVAILABLE, STOPPING.
 * When according to the algorithm used by the {@link SystemStateService} the system is UNAVAILABLE
 * the onAvailable callback is called. Possible next states: AVAILABLE, STOPPING.
 * When the framework is stopping by means of stopping the Framework bundle the onStopping callback
 * is called. Possible next states: none.
 */
public interface FrameworkStateListener {

	/**
	 * Indicates the system is starting.
	 */
	void onStarting();
	
	/**
	 * Indicates the system is stopping.
	 */
	void onStopping();
	
	/**
	 * Indicates the system is available.
	 */
	void onAvailable();
	
	/**
	 * Indicates the system is unavailable.
	 */
	void onUnavailable();
	
}