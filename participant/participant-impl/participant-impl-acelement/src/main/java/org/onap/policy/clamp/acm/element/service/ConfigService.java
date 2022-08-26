/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

import java.util.List;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.element.handler.MessageActivator;
import org.onap.policy.clamp.acm.element.handler.MessageHandler;
import org.onap.policy.clamp.acm.element.main.parameters.ElementTopicParameters;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementMessage;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

    private ElementConfig elementConfig = new ElementConfig();

    private final MessageHandler handler;
    private final MessageActivator messageActivator;

    /**
     * Activate messages and service and create the element configuration.
     *
     * @param elementConfig the configuration
     */
    public void activateElement(@NonNull ElementConfig elementConfig) {
        var listenerTopicParameters = new ElementTopicParameters(elementConfig.getTopicParameterGroup());

        var publisherTopicParameters = new ElementTopicParameters(elementConfig.getTopicParameterGroup());
        publisherTopicParameters.setTopic(elementConfig.getTopicParameterGroup().getPublisherTopic());

        var parameters = new TopicParameterGroup();
        parameters.setTopicSinks(List.of(listenerTopicParameters));
        parameters.setTopicSources(List.of(publisherTopicParameters));

        if (!parameters.isValid()) {
            throw new AutomationCompositionRuntimeException(Response.Status.BAD_REQUEST,
                    "Validation failed for topic parameter group. Kafka config not activated");
        }

        if (messageActivator.isAlive()) {
            throw new AutomationCompositionRuntimeException(Response.Status.CONFLICT,
                    "Service Manager already running, cannot add Topic endpoint management");
        }

        handler.active(elementConfig);
        messageActivator.activate(parameters);
        this.elementConfig = elementConfig;

        LOGGER.info("Messages and service activated");
    }

    /**
     * Fetch element configuration.
     *
     * @return element configuration present
     */
    public ElementConfig getElementConfig() {
        return elementConfig;
    }

    /**
     * Deactivate messages and service and delete the element config.
     */
    public void deleteConfig() {
        handler.deactivateElement();
        messageActivator.deactivate();
        elementConfig = new ElementConfig();
        LOGGER.info("Messages and service deactivated");
    }

    public List<ElementMessage> getMessages() {
        return handler.getMessages();
    }
}
