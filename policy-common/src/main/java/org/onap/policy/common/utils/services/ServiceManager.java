/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.utils.services;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.onap.policy.common.capabilities.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a series of services. The services are started in order, and stopped in reverse
 * order.
 */
public class ServiceManager implements Startable {
    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * Manager name.
     */
    @Getter
    private final String name;

    /**
     * Services to be started/stopped.
     */
    private final Deque<Service> items = new LinkedList<>();

    /**
     * {@code True} if the services are currently running, {@code false} otherwise.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Constructs the object, with a default name.
     */
    public ServiceManager() {
        this("service manager");
    }

    /**
     * Constructs the object.
     *
     * @param name the manager's name, used for logging purposes
     */
    public ServiceManager(String name) {
        this.name = name;
    }

    /**
     * Adds a pair of service actions to the manager.
     *
     * @param stepName name to be logged when the service is started/stopped
     * @param starter function to start the service
     * @param stopper function to stop the service
     * @return this manager
     */
    public synchronized ServiceManager addAction(String stepName, RunnableWithEx starter, RunnableWithEx stopper) {
        if (isAlive()) {
            throw new IllegalStateException(name + " is already running; cannot add " + stepName);
        }

        items.add(new Service(stepName, starter, stopper));
        return this;
    }

    /**
     * Adds a service to the manager. The manager will invoke the service's
     * {@link Startable#start()} and {@link Startable#stop()} methods.
     *
     * @param stepName name to be logged when the service is started/stopped
     * @param service object to be started/stopped
     * @return this manager
     */
    public synchronized ServiceManager addService(String stepName, Startable service) {
        if (isAlive()) {
            throw new IllegalStateException(name + " is already running; cannot add " + stepName);
        }

        items.add(new Service(stepName, service::start, service::stop));
        return this;
    }

    @Override
    public boolean isAlive() {
        return running.get();
    }

    @Override
    public synchronized boolean start() {
        if (isAlive()) {
            throw new IllegalStateException(name + " is already running");
        }

        logger.info("{} starting", name);

        // tracks the services that have been started so far
        Deque<Service> started = new LinkedList<>();
        Exception ex = null;

        for (Service item : items) {
            try {
                logger.info("{} starting {}", name, item.stepName);
                item.starter.run();
                started.add(item);

            } catch (Exception e) {
                logger.error("{} failed to start {}; rewinding steps", name, item.stepName);
                ex = e;
                break;
            }
        }

        if (ex == null) {
            logger.info("{} started", name);
            running.set(true);
            return true;
        }

        // one of the services failed to start - rewind those we've previously started
        try {
            rewind(started);

        } catch (ServiceManagerException e) {
            logger.error("{} rewind failed", name, e);
        }

        throw new ServiceManagerException(ex);
    }

    @Override
    public synchronized boolean stop() {
        if (!isAlive()) {
            throw new IllegalStateException(name + " is not running");
        }

        running.set(false);
        rewind(items);

        return true;
    }

    @Override
    public void shutdown() {
        stop();
    }

    /**
     * Rewinds a list of services, stopping them in reverse order. Stops all of the
     * services, even if one of the "stop" functions throws an exception.
     *
     * @param running services that are running, in the order they were started
     * @throws ServiceManagerException if a service fails to stop
     */
    private void rewind(Deque<Service> running) {
        Exception ex = null;

        logger.info("{} stopping", name);

        // stop everything, in reverse order
        Iterator<Service> it = running.descendingIterator();
        while (it.hasNext()) {
            Service item = it.next();
            try {
                logger.info("{} stopping {}", name, item.stepName);
                item.stopper.run();
            } catch (Exception e) {
                logger.error("{} failed to stop {}", name, item.stepName);
                ex = e;

                // do NOT break or re-throw, as we must stop ALL remaining items
            }
        }

        logger.info("{} stopped", name);

        if (ex != null) {
            throw new ServiceManagerException(ex);
        }
    }

    /**
     * Service information.
     */
    @AllArgsConstructor
    private static class Service {
        private String stepName;
        private RunnableWithEx starter;
        private RunnableWithEx stopper;
    }

    /*
     * Cannot use a plain Runnable, because it can't throw exceptions. Could use a
     * Callable, instead, but then all the lambda expressions become rather messy, thus
     * we'll stick with RunnableWithEx, and just disable the sonar warning.
     */
    @FunctionalInterface
    public static interface RunnableWithEx {
        void run() throws Exception; // NOSONAR
    }
}
