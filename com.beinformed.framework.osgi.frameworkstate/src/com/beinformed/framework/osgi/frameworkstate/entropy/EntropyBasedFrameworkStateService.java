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

import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beinformed.framework.osgi.frameworkstate.FrameworkStateListener;
import com.beinformed.framework.osgi.frameworkstate.FrameworkStateService;
import com.beinformed.framework.osgi.frameworkstate.State;
import com.beinformed.framework.osgi.frameworkstate.Token;

/**
 * FrameworkStateService implementation based on framework entropy. It monitors
 * (un)publishing of services and considers the framework to be available once
 * no services are being (un)published for a certain amount of time.
 */
public class EntropyBasedFrameworkStateService implements BundleListener, ServiceListener, FrameworkStateService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntropyBasedFrameworkStateService.class);
	private static final String SERVICES_TO_IGNORE = "(!(objectClass=org.osgi.service.event.EventHandler))";

	private final Map<Bundle, Token> starting;
	private final Map<Bundle, Token> stopping;
	private final Set<Token> tokens;
	private final List<FrameworkStateListener> listeners; // protected by itself
	private final Object stateLock;
	private final ExecutorService executor;
	private final AtomicInteger monitorStatus;

	// Injected by Felix DM...
	private volatile BundleContext bundleContext;

	private Duration entropyTimeout;
	private Duration pollingInterval;
	private Instant idleSince; // protected by stateLock
	private State currentState; // protected by stateLock

	public EntropyBasedFrameworkStateService() {
		starting = new ConcurrentHashMap<Bundle, Token>();
		stopping = new ConcurrentHashMap<Bundle, Token>();
		tokens = newKeySet();
		listeners = new ArrayList<>();
		stateLock = new Object();
		executor = newSingleThreadExecutor();
		monitorStatus = new AtomicInteger(0);
		currentState = State.UNAVAILABLE;
	}

	void init() throws InvalidSyntaxException {
		LOGGER.debug("init");

		entropyTimeout = Duration.ofMillis(getProperty("entropyTimeout", 1000));
		LOGGER.info("Using configured entropy timeout of {} ms.", entropyTimeout.toMillis());

		pollingInterval = Duration.ofMillis(getProperty("entropyInterval", 200));
		LOGGER.info("Using configured entropy polling interval of {} ms.", pollingInterval.toMillis());

		bundleContext.addBundleListener(this);
		bundleContext.addServiceListener(this, SERVICES_TO_IGNORE);
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
		bundleContext.removeServiceListener(this);
		bundleContext.removeBundleListener(this);
		LOGGER.debug("Destroy called");
	}

	private void handleStateChange(State newState) {
		LOGGER.info("System state changed to: {}", newState);
		FrameworkStateListener[] list;
		// first make a copy of the current list of listeners in a synchronized
		// block because we want to handle concurrent access to this list e.g.
		// if during notification one of the listeners adds/removes another
		// systemstate listener
		synchronized (listeners) {
			list = listeners.toArray(new FrameworkStateListener[listeners.size()]);
		}

		// then invoke all listeners outside of the synchronized block because
		// we don't want to be holding any locks while invoking callbacks
		for (FrameworkStateListener listener : list) {
			try {
				if (listeners.contains(listener)) {
					// listeners might have changed in the mean time (proofed to
					// happen), narrow the chance on notifying old listeners
					notifyListener(listener, newState);
				}
			} catch (Exception e) {
				LOGGER.error("Exception while trying to notify listener " + listener.getClass().getName() + "("
						+ listener + ") of system stable state change to " + newState, e);
			}
		}

		LOGGER.debug("Notified listeners, system is: {}", newState);
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

		State state;
		synchronized (stateLock) {
			state = currentState;
		}
		// notify the listener outside of the synchronized block so we're
		// not invoking any callbacks while holding a lock
		notifyListener(listener, state);
	}

	void listenerRemoved(FrameworkStateListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private void handleSystemNoise() {
		boolean stateChanged = false;
		synchronized (stateLock) {
			idleSince = Instant.now();
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

	@Override
	public void serviceChanged(ServiceEvent event) {
		if (event.getType() == REGISTERED || event.getType() == UNREGISTERING) {
			handleSystemNoise();
		}
	}

	class IdleWatchMonitor implements Runnable {
		boolean interrupted;

		@Override
		public void run() {
			monitorStatus.incrementAndGet();

			LOGGER.debug("*** IdleWatchMonitor.run() - {} - outstanding units of work: {}",
					monitorStatus.get(), tokens.size());

			while (!interrupted) {
				try {
					Thread.sleep(pollingInterval.toMillis());
					LOGGER.debug("*** IdleWatchMonitor.run() - {} - waking up...", monitorStatus.get());
					boolean stateChanged = false;

					synchronized (stateLock) {
						if (currentState != State.UNAVAILABLE) {
							interrupted = true;
						} else {
							Duration idleDuration = Duration.between(idleSince, Instant.now());
							// compare the split time of the monitor to the timeout, and also
							// make sure we have no outstanding tokens that signal that there
							// is still ongoing work
							if (idleDuration.compareTo(entropyTimeout) > 0 && tokens.isEmpty()) {
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
					LOGGER.info("*** IdleWatchMonitor - {} - got interrupted!",
							monitorStatus.get());

					interrupted = true;
				}
			}

			LOGGER.debug("*** IdleWatchMonitor.run() - {} - completed...",
					monitorStatus.get());

			monitorStatus.decrementAndGet();
		}
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		int type = event.getType();

		// if any bundle enters the STARTING state, we create a unit of work
		// for it that will end as soon as that same bundle transitions into
		// any other state
		if (type == BundleEvent.STARTING) {
			Token token = startWork(bundle);
			Token old = starting.putIfAbsent(bundle, token);
			if (old != null) {
				// this is in fact very weird, the bundle already was in
				// starting state and entered the same state *again*
				LOGGER.error("Bundle entered STARTING state twice, old token {}, new token {}", old, token);

				// for now we will try to recover by simply ending the old unit
				// of work
				endWork(old);
			}
		} else {
			// when a bundle enters any other state than STARTING, we should
			// make sure it's no longer on the starting list
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
			Token old = stopping.putIfAbsent(bundle, token);
			if (old != null) {
				// this is in fact very weird, the bundle already was in
				// starting state and entered the same state *again*
				LOGGER.error("Bundle entered STOPPING state twice, old token {}, new token {}", old, token);

				// for now we will try to recover by simply ending the old unit
				// of work
				endWork(old);
			}
		} else {
			// when a bundle enters any other state than STOPPING, we should
			// make sure it's no longer on the stopping list
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

	private int getProperty(String key, int dflt) {
		String val = bundleContext.getProperty(key);
		try {
			if (val != null && !"".equals(val)) {
				return Integer.parseInt(val);
			}
		} catch (NumberFormatException e) {
			// Too bad, use default value instead...
			LOGGER.debug("Unable to parse property " + key + ": " + val, e);
		}
		return dflt;
	}
}