/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2018 Samsung Electronics Co., Ltd.
 * Modifications Copyright (C) 2020,2023 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2022-2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event.base;

public interface BusPublisher {

    String NO_MESSAGE_PROVIDED = "No message provided";
    String LOG_CLOSE = "{}: CLOSE";

    /**
     * sends a message.
     *
     * @param partitionId id
     * @param message     the message
     * @return true if success, false otherwise
     * @throws IllegalArgumentException if no message provided
     */
    boolean send(String partitionId, String message);

    /**
     * closes the publisher.
     */
    void close();
}
