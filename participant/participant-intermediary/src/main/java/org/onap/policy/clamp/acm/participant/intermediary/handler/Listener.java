/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2024 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import org.onap.policy.common.endpoints.listeners.ScoListener;

public interface Listener<T> {

    /**
     * Get the type of message of interest to the listener.
     *
     * @return type of message of interest to the listener
     */
    String getType();

    /**
     * Get listener to register.
     *
     * @return listener to register
     */
    ScoListener<T> getScoListener();

    /**
     * Check if default topic.
     * @return true if default topic
     */
    boolean isDefaultTopic();
}
