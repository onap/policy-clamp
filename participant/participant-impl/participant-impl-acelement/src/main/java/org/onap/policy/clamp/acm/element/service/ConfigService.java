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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.element.handler.MessageActivator;
import org.onap.policy.clamp.acm.element.handler.MessageHandler;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
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
        this.elementConfig = elementConfig;

        var topicParameters = new TopicParameters();
        topicParameters.setTopic(elementConfig.getTopicParameterGroup().getTopic());
        topicParameters.setServers(List.of(elementConfig.getTopicParameterGroup().getServer()));
        topicParameters.setFetchTimeout(elementConfig.getTopicParameterGroup().getFetchTimeout());
        topicParameters.setTopicCommInfrastructure(elementConfig.getTopicParameterGroup().getTopicCommInfrastructure());
        topicParameters.setUseHttps(elementConfig.getTopicParameterGroup().isUseHttps());

        var parameters = new TopicParameterGroup();
        parameters.setTopicSinks(List.of(topicParameters));
        parameters.setTopicSources(List.of(topicParameters));
        messageActivator.activate(parameters);

        handler.active(elementConfig);
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
}
