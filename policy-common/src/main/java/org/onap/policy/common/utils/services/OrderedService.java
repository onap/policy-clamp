/*
 * ============LICENSE_START=======================================================
 * utils
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

/**
 * This is a base interface that is used to control the order of a list
 * of services (features) discovered via 'ServiceLoader'. See
 * 'OrderedServiceImpl' for more details.
 */
@FunctionalInterface
public interface OrderedService {
    /**
     * Get sequence number.
     *
     * @return an integer sequence number, which determines the order of a list
     *     of objects implementing this interface
     */
    public int getSequenceNumber();


    /**
     * Get the name.
     *
     * @return the name of the ordered service
     */
    public default String getName() {
        return this.getClass().getName();
    }
}
