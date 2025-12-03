/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import org.onap.policy.common.capabilities.Startable;
import org.onap.policy.common.utils.services.ServiceManager.RunnableWithEx;

/**
 * Container, of a service manager, that implements a {@link Startable} interface by
 * delegating to the contained service manager. This allows subclasses to add actions to
 * the service manager, while preventing other classes from doing so.
 */
public class ServiceManagerContainer implements Startable {

    /**
     * The contained manager.
     */
    private final ServiceManager serviceManager;

    /**
     * Constructs the object, with a default name.
     */
    public ServiceManagerContainer() {
        serviceManager = new ServiceManager();
    }

    /**
     * Constructs the object.
     *
     * @param name the manager's name, used for logging purposes
     */
    public ServiceManagerContainer(String name) {
        serviceManager = new ServiceManager(name);
    }

    public String getName() {
        return serviceManager.getName();
    }

    /**
     * Adds a pair of service actions to the manager.
     *
     * @param stepName name to be logged when the service is started/stopped
     * @param starter function to start the service
     * @param stopper function to stop the service
     */
    protected void addAction(String stepName, RunnableWithEx starter, RunnableWithEx stopper) {
        serviceManager.addAction(stepName, starter, stopper);
    }

    /**
     * Adds a service to the manager. The manager will invoke the service's
     * {@link Startable#start()} and {@link Startable#stop()} methods.
     *
     * @param stepName name to be logged when the service is started/stopped
     * @param service object to be started/stopped
     */
    protected void addService(String stepName, Startable service) {
        serviceManager.addService(stepName, service);
    }

    @Override
    public boolean isAlive() {
        return serviceManager.isAlive();
    }

    @Override
    public boolean start() {
        return serviceManager.start();
    }

    @Override
    public boolean stop() {
        return serviceManager.stop();
    }

    @Override
    public void shutdown() {
        serviceManager.shutdown();
    }
}
