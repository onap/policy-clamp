/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.http.utils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.http.main.models.RestParams;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    private static final String TEST_KEY_NAME =
        "org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement";
    public static final UUID AC_ID = UUID.randomUUID();

    /**
     * Get a automationComposition Element.
     *
     * @return automationCompositionElement object
     */
    public AutomationCompositionElement getAutomationCompositionElement() {
        AutomationCompositionElement element = new AutomationCompositionElement();
        element.setId(UUID.randomUUID());
        element.setDefinition(new ToscaConceptIdentifier(TEST_KEY_NAME, "1.0.1"));
        element.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        return element;
    }

    /**
     * Get query params.
     *
     * @return Map of query params
     */
    public Map<String, String> getQueryParams() {
        return Map.of("name", "dummy", "version", "1.0");
    }

    /**
     * Get path params.
     *
     * @return Map of path params
     */
    public Map<String, Object> getPathParams() {
        return Map.of("id", "123", "name", "dummy");
    }

    /**
     * Rest params with GET request.
     *
     * @return RestParams obj
     */
    public RestParams restParamsWithGet() {
        return new RestParams(new ToscaConceptIdentifier("getRequest", "1.0"), "GET", "get", 200, null,
            getQueryParams(), null);
    }

    /**
     * Rest params with POST request.
     *
     * @return RestParams obj
     */
    public RestParams restParamsWithPost() {
        return new RestParams(new ToscaConceptIdentifier("postRequest", "1.0"), "POST", "post", 200, null,
            getQueryParams(), "Test body");
    }

    /**
     * Rest params with POST request.
     *
     * @return RestParams obj
     */
    public RestParams restParamsWithInvalidPost() {
        return new RestParams(new ToscaConceptIdentifier("postRequest", "1.0"), "POST", "post/{id}/{name}", 200,
            getPathParams(), getQueryParams(), "Test body");
    }

    /**
     * Get invalid configuration entity.
     *
     * @return ConfigurationEntity obj
     */
    public ConfigurationEntity getInvalidConfigurationEntity() {
        return new ConfigurationEntity(new ToscaConceptIdentifier("config1", "1.0.1"),
            List.of(restParamsWithGet(), restParamsWithInvalidPost()));
    }

    /**
     * Get configuration entity.
     *
     * @return ConfigurationEntity obj
     */
    public ConfigurationEntity getConfigurationEntity() {
        return new ConfigurationEntity(new ToscaConceptIdentifier("config1", "1.0.1"),
            List.of(restParamsWithGet(), restParamsWithPost()));
    }

    /**
     * Get automation composition id.
     *
     * @return UUID automationCompositionId
     */
    public UUID getAutomationCompositionId() {
        return AC_ID;
    }

    /**
     * Get headers for config request.
     *
     * @return Map of headers
     */
    public Map<String, String> getHeaders() {
        return Map.of("Content-Type", "application/json", "Accept", "application/json");
    }

}
