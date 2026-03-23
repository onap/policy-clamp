/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024,2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2022 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kserve.utils;

import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.common.utils.coder.MapperFactory;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public class CommonTestData {

    public static final String TOSCA_TEMPLATE_YAML = "clamp/acm/test/participant-kserve.yaml";

    private static final String TEST_KEY_NAME = "org.onap.domain.database.KserveAutomationCompositionElement";
    private static final List<UUID> AC_ID_LIST = List.of(UUID.randomUUID(), UUID.randomUUID());
    private static final ObjectMapper yamlMapper = MapperFactory.createYamlMapper();

    /**
     * Get a new InstanceElement.
     *
     * @return InstanceElementDto object
     */
    public static InstanceElementDto getAutomationCompositionElement() {
        return new InstanceElementDto(
                getAutomationCompositionId(), UUID.randomUUID(), null, Map.of(), Map.of());
    }

    /**
     * Get a new CompositionElement.
     *
     * @param properties common properties from service template
     * @return CompositionElementDto object
     */
    public static CompositionElementDto getCompositionElement(Map<String, Object> properties) {
        return new CompositionElementDto(UUID.randomUUID(),
                new ToscaConceptIdentifier(TEST_KEY_NAME, "1.0.1"),
                properties, Map.of());
    }

    /**
     * Get automation composition id.
     *
     * @return ToscaConceptIdentifier automationCompositionId
     */
    public static UUID getAutomationCompositionId() {
        return AC_ID_LIST.get(0);
    }

    /**
     * Get ToscaServiceTemplate from resource.
     *
     * @param path path of the resource
     */
    public static ToscaServiceTemplate getToscaServiceTemplateFromYamlFile(String path) {
        return getToscaServiceTemplateFromYaml(ResourceUtils.getResourceAsString(path));
    }

    /**
     * Get ToscaServiceTemplate from yaml.
     *
     * @param yaml the resource
     */
    public static ToscaServiceTemplate getToscaServiceTemplateFromYaml(String yaml) {
        try {
            return yamlMapper.readValue(yaml, ToscaServiceTemplate.class);
        } catch (JsonProcessingException e) {
            fail("Cannot read or decode " + yaml);
            return null;
        }
    }
}
