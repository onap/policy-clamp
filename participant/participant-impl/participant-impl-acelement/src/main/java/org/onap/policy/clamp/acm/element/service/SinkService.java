/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022,2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.service;

import org.onap.policy.clamp.acm.element.handler.messages.ElementMessage;
import org.onap.policy.clamp.acm.element.main.concepts.ElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sink Service.
 */
@Service
public class SinkService extends AbstractElementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SinkService.class);

    @Override
    public void handleMessage(ElementMessage message) {
        LOGGER.info("Cycle completed in sink service with the full message: {}", message);
    }

    @Override
    public ElementType getType() {
        return ElementType.SINK;
    }
}
