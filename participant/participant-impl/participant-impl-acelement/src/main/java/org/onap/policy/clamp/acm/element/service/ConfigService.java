/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.element.handler.MessageHandler;
import org.onap.policy.clamp.acm.element.main.concepts.ElementConfig;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    @Getter
    private ElementConfig elementConfig = new ElementConfig();

    private final MessageHandler handler;

    /**
     * Activate messages and service and create the element configuration.
     *
     * @param elementConfig the configuration
     */
    public void activateElement(@NonNull ElementConfig elementConfig) {
        handler.active(elementConfig);
        this.elementConfig = elementConfig;

        log.info("Messages and service activated");
    }

    /**
     * Deactivate messages and service and delete the element config.
     */
    public void deleteConfig() {
        handler.deactivateElement();
        elementConfig = new ElementConfig();
        log.info("Messages and service deactivated");
    }
}
