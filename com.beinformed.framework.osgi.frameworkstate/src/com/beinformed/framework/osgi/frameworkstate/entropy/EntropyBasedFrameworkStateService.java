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
package com.beinformed.framework.osgi.frameworkstate.entropy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.time.StopWatch;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beinformed.framework.osgi.frameworkstate.FrameworkStateListener;
import com.beinformed.framework.osgi.frameworkstate.FrameworkStateService;
import com.beinformed.framework.osgi.frameworkstate.State;
import com.beinformed.framework.osgi.frameworkstate.Token;

/**
 * FrameworkStateService implementation based on framework entropy. It monitors (un)publishing of services and considers
 * the framework to be available once no services are being (un)published for a certain amount of time. 
 *
 */
public class EntropyBasedFrameworkStateService implements BundleListener, FrameworkStateService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntropyBasedFrameworkStateService.class);

	private final Set<Token> tokens = Collections.synchronizedSet(new HashSet<Token>());

	private final List<FrameworkStateListener> listeners = new ArrayList<FrameworkStateListener>();

	private int entropyTimeoutMillis = 1000;

	private int pollingIntervalMillis = 200;

	private final StopWatch idleWatch = new StopWatch();

	private Object stateLock = new Object();

	private volatile State currentState = State.UNAVAILABLE;

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private volatile BundleContext bundleContext;

	private AtomicInteger monitorStatus = new AtomicInteger(0);

	private final static List<String> serviceInterfacesToIgnore = new ArrayList<String>();
	static {
		serviceInterfacesToIgnore.add("org.osgi.service.event.EventHandler");
	}

	public EntropyBasedFrameworkStateService() {
		if (System.getProperty("entropyTimeout") != null) {
			entropyTimeoutMillis = Integer.parseInt(System.getProperty("entropyTimeout"));
			LOGGER.info("Using configured entropy timeout of {} ms.", new String[] { String.valueOf(entropyTimeoutMillis) });
		}
		if (System.getProperty("entropyInterval") != null) {
			pollingIntervalMillis = Integer.parseInt(System.getProperty("entropyInterval"));
			LOGGER.info("Using configured entropy polling interval of {} ms.", new String[] { String.valueOf(pollingIntervalMillis) });
		}
	}

	void init() {
		LOGGER.debug("init");
		bundleContext.addBundleListener(this);
	}

	void start() {
		LOGGER.debug("start");
		currentState = State.STARTING;
		handleStateChange(State.STARTING);
	}

	void stop() {
		LOGGER.debug("stop");
		executor.shutdownNow();
	}

	void destroy() {
		bundleContext.removeBundleListener(this);
		LOGGER.debug("Destroy called");
	}

	private void handleStateChange(State newState) {
		LOGGER.info("System state changed to: " + newState);
		FrameworkStateListener[] list;
		// first make a copy of the current list of listeners in a synchronized block
		// because we want to handle concurrent access to this list
		// e.g. if during notification one of the listeners adds/removes another systemstate listener
		synchronized (listeners) {
			list = listeners.toArray(new FrameworkStateListener[listeners.size()]);
		}

		// then invoke all listeners outside of the synchronized block because
		// we don't want to be holding any locks while invoking callbacks
		for (FrameworkStateListener listener : list) {
			try {
				if (listeners.contains(listener)) {
					//listeners might have changed in the mean time (proofed to happen), narrow the chance on notifying old listeners
					notifyListener(listener, newState);
				}
			} catch (Exception e) {
				LOGGER.error("Exception while trying to notify listener " + listener.getClass().getName() + "(" + listener
						+ ") of system stable state change to " + newState, e);
			}
		}

		LOGGER.info("Notified listeners, system is: " + newState);
	}

	private void notifyListener(FrameworkStateListener listener, State state) {
		switch (state) {
			case STARTING:
				listener.onStarting();
				break;
			case STOPPING:
				listener.onStopping();
				break;
			case AVAILABLE:
				listener.onAvailable();
				break;
			case UNAVAILABLE:
				listener.onUnavailable();
				break;
			default:
				throw new IllegalStateException("Attempt to notify of an unknown system state: " + state);
		}
	}

	void listenerAdded(FrameworkStateListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
		// notify the listener outside of the synchronized block so we're
		// not invoking any callbacks while holding a lock
		notifyListener(listener, currentState);
	}

	void listenerRemoved(FrameworkStateListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private void handleSystemNoise() {
		boolean stateChanged = false;
		synchronized (stateLock) {
			idleWatch.reset();
			idleWatch.start();
			if (currentState == State.AVAILABLE || currentState == State.STARTING) {
				currentState = State.UNAVAILABLE;
				// flag that indicates we still need to do work outside of the
				// synchronized block (work we can't do here)
				stateChanged = true;
			}
		}
		if (stateChanged) {
			handleStateChange(State.UNAVAILABLE);
			executor.execute(new IdleWatchMonitor());
		}
	}

	void serviceAdded(ServiceReference reference, Object service) {
		if (isValidService(reference, service)) {
			handleSystemNoise();
		}
	}

	void serviceSwapped(ServiceReference oldReference, Object oldService, ServiceReference newReference, Object newService) {
		if (isValidService(newReference, newService)) {
			handleSystemNoise();
		}
	}

	void serviceRemoved(ServiceReference reference, Object service) {
		if (isValidService(reference, service)) {
			handleSystemNoise();
		}
	}

	private boolean isValidService(ServiceReference reference, Object service) {
		Object o = reference.getProperty("objectClass");
		if (o instanceof String[]) {
			String[] serviceInterfaces = (String[]) o;
			// Valid means that at least one of the service interfaces is not in the ignore list.
			for (String serviceInterface : serviceInterfaces) {
				if (!serviceInterfacesToIgnore.contains(serviceInterface)) {
					return true;
				}
			}
			return false;
		}
		// Unable to determine...we will react on it anyway
		return true;
	}

	class IdleWatchMonitor implements Runnable {
		boolean interrupted;

		@Override
		public void run() {
			LOGGER.debug("*** IdleWatchMonitor.run() - " + monitorStatus.get() + " outstanding units of work: " + tokens.size());

			monitorStatus.incrementAndGet();

			while (!interrupted) {
				try {
					Thread.sleep(pollingIntervalMillis);
					LOGGER.debug("*** IdleWatchMonitor waking up.");
					boolean stateChanged = false;

					synchronized (stateLock) {
						if (currentState != State.UNAVAILABLE) {
							interrupted = true;
						} else {
							// compare the split time of the monitor to the timeout, and also make
							// sure we have no outstanding tokens that signal that there is still ongoing
							// work
							if (idleWatch.getTime() > entropyTimeoutMillis && tokens.isEmpty()) {
								// yay, we think the system is stable
								currentState = State.AVAILABLE;
								stateChanged = true;
								interrupted = true;
							}
						}
					}

					if (stateChanged) {
						handleStateChange(State.AVAILABLE);
					}
				} catch (InterruptedException e) {
					LOGGER.info("IdleWatchMonitor got interrupted!");
					interrupted = true;
				}
			}
			LOGGER.debug("*** IdleWatchMonitor.completed()");

			monitorStatus.decrementAndGet();
		}
	}

	public void setTimeoutInMillis(int timeoutInMillis) {
		this.entropyTimeoutMillis = timeoutInMillis;
	}

	public void setPollingInterval(int pollingInterval) {
		this.pollingIntervalMillis = pollingInterval;
	}

	private final Map<Bundle, Token> starting = Collections.synchronizedMap(new HashMap<Bundle, Token>());

	private final Map<Bundle, Token> stopping = Collections.synchronizedMap(new HashMap<Bundle, Token>());

	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		int type = event.getType();

		// if any bundle enters the STARTING state, we create a unit of work
		// for it that will end as soon as that same bundle transitions into
		// any other state
		if (type == BundleEvent.STARTING) {
			Token token = startWork(bundle);
			Token old = starting.put(bundle, token);
			if (old != null) {
				// this is in fact very weird, the bundle already was in
				// starting state and entered the same state *again*
				LOGGER.error("Bundle entered STARTING state twice, old token {}, new token {}", old, token);

				// for now we will try to recover by simply ending the old unit
				// of work
				endWork(old);
			}
		} else {
			// when a bundle enters any other state than STARTING, we should make sure it's
			// no longer on the starting list
			Token token = starting.remove(bundle);
			if (token != null) {
				// it was on the list, so we end the unit of work
				endWork(token);
			}
		}

		// if any bundle enters the STOPPING state, we create a unit of work
		// for it that will end as soon as that same bundle transitions into
		// any other state
		if (type == BundleEvent.STOPPING) {
			Token token = startWork(bundle);
			Token old = stopping.put(bundle, token);
			if (old != null) {
				// this is in fact very weird, the bundle already was in
				// starting state and entered the same state *again*
				LOGGER.error("Bundle entered STOPPING state twice, old token {}, new token {}", old, token);

				// for now we will try to recover by simply ending the old unit
				// of work
				endWork(old);
			}
		} else {
			// when a bundle enters any other state than STOPPING, we should make sure it's
			// no longer on the stopping list
			Token token = stopping.remove(bundle);
			if (token != null) {
				// it was on the list, so we end the unit of work
				endWork(token);
			}
		}

		// when the system bundle is stopping, that's a special case, as we know
		// our framework is going down (sorry folks, no way out anymore now)
		if (bundle.getBundleId() == 0) {
			if (type == BundleEvent.STOPPING) {
				LOGGER.info("Framework bundle status changed to STOPPING");
				handleStateChange(State.STOPPING);
			}
		}
	}

	@Override
	public Token startWork(Object reference) {
		Token token = new Token(reference);
		tokens.add(token);
		return token;
	}

	@Override
	public void endWork(Token token) {
		if (token == null) {
			LOGGER.debug("End of work signalled with null token, ignoring.");
			return;
		}
		boolean removed = tokens.remove(token);
		if (!removed) {
			LOGGER.debug("{} signalled the end of work for a task that had not been started, ignoring.", token);
		}
	}

}