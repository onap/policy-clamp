/*
 * ============LICENSE_START=======================================================
 * utils
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a template for building a sorted list of service instances,
 * which are discovered and created using 'ServiceLoader'.
 */
public class OrderedServiceImpl<T extends OrderedService> {
    // logger
    private static final Logger logger = LoggerFactory.getLogger(OrderedServiceImpl.class);

    // sorted list of instances implementing the service
    private List<T> implementers = null;

    // 'ServiceLoader' that is used to discover and create the services
    private final ServiceLoader<T> serviceLoader;

    // use this to ensure that we only use one unique instance of each class
    private static final Map<Class<?>, OrderedService> classToSingleton = new HashMap<>();

    /**
     * Constructor - create the 'ServiceLoader' instance.
     *
     * @param clazz the class object associated with 'T' (I supposed it could
     *              be a subclass, but I'm not sure if this is useful)
     */
    public OrderedServiceImpl(Class<T> clazz) {
        // This constructor wouldn't be needed if 'T.class' was legal
        serviceLoader = ServiceLoader.load(clazz);
    }

    /**
     * Get List of implementers.
     *
     * @return the sorted list of services implementing interface 'T' discovered by 'ServiceLoader'.
     */
    public synchronized List<T> getList() {
        if (implementers == null) {
            rebuildList();
        }
        return implementers;
    }

    /**
     * This method is called by 'getList', but could also be called directly if
     * we were running with a 'ClassLoader' that supported the dynamic addition
     * of JAR files. In this case, it could be invoked in order to discover any
     * new services implementing interface 'T'. This is probably a relatively
     * expensive operation in terms of CPU and elapsed time, so it is best if it
     * isn't invoked too frequently.
     *
     * @return the sorted list of services implementing interface 'T' discovered by 'ServiceLoader'.
     */
    @SuppressWarnings("unchecked")
    public synchronized List<T> rebuildList() {
        // build a list of all the current implementors
        List<T> tmp = new LinkedList<>();
        for (T service : serviceLoader) {
            tmp.add((T) getSingleton(service));
        }

        // Sort the list according to sequence number, and then alphabetically
        // according to full class name.
        tmp.sort((o1, o2) -> {
            int s1 = o1.getSequenceNumber();
            int s2 = o2.getSequenceNumber();
            if (s1 < s2) {
                return -1;
            } else if (s1 > s2) {
                return 1;
            } else {
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
        });

        // create an unmodifiable version of this list
        implementers = Collections.unmodifiableList(tmp);
        logger.info("***** OrderedServiceImpl implementers:\n {}", implementers);
        return implementers;
    }

    /**
     * If a service implements multiple APIs managed by 'ServiceLoader', a
     * separate instance is created for each API. This method ensures that
     * the first instance is used in all the lists.
     *
     * @param service this is the object created by ServiceLoader
     * @return the object to use in place of 'service'. If 'service' is the first
     *     object of this class created by ServiceLoader, it is returned. If not,
     *     the object of this class that was initially created is returned
     *     instead.
     */
    private static synchronized OrderedService getSingleton(OrderedService service) {
        // see if we already have an instance of this class
        OrderedService rval = classToSingleton.get(service.getClass());
        if (rval == null) {
            // No previous instance of this class exists -- use the supplied
            // instance, and place it in the table.
            rval = service;
            classToSingleton.put(service.getClass(), service);
        }
        return rval;
    }
}
