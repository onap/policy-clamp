/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.utils;

import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.common.utils.coder.MapperFactory;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {

    public static final UUID PARTICIPANT_ID = UUID.randomUUID();
    public static final UUID REPLICA_ID = UUID.randomUUID();
    private static final ObjectMapper MAPPER = MapperFactory.createJsonMapper();
    private static final YAMLMapper YAML_MAPPER = MapperFactory.createYamlMapper();

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getParticipantId() {
        return PARTICIPANT_ID;
    }

    /**
     * Returns participant replica Id for test cases.
     *
     * @return replica Id
     */
    public static UUID getReplicaId() {
        return REPLICA_ID;
    }

    /**
     * Returns participantId for test Jpa cases.
     *
     * @return participant Id
     */
    public static String getJpaParticipantId() {
        return PARTICIPANT_ID.toString();
    }

    /**
     * Returns random participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getRndParticipantId() {
        return UUID.randomUUID();
    }

    /**
     * Get ToscaServiceTemplate from resource.
     *
     * @param path path of the resource
     */
    public static ToscaServiceTemplate getToscaServiceTemplate(String path) {
        try {
            return YAML_MAPPER.readValue(ResourceUtils.getResourceAsStream(path), ToscaServiceTemplate.class);
        } catch (IOException e) {
            fail("Cannot read or decode " + e.getMessage());
            return null;
        }
    }

    /**
     * Get Object from string in yaml format.
     *
     * @param yaml the string in yaml format
     * @param clazz the Class of the Object
     * @return the Object
     */
    public static <T> T getObjectFromYaml(String yaml, Class<T> clazz) {
        try {
            return YAML_MAPPER.readValue(yaml, clazz);
        } catch (IOException e) {
            fail("Cannot decode " + yaml);
            return null;
        }
    }

    /**
     * Get Object from json file.
     *
     * @param path path of the resource
     * @param clazz the Class of the Object
     * @return the Object
     */
    public static <T> T getObjectFromJsonFile(final String path, Class<T> clazz) {
        try {
            return MAPPER.readValue(new File(path), clazz);
        } catch (IOException e) {
            fail("Cannot decode " + path);
            return null;
        }
    }

    /**
     * Get Object from json string.
     *
     * @param json the json
     * @param clazz the Class of the Object
     * @return the Object
     */
    public static <T> T getObjectFromJson(final String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            fail("Cannot decode " + json);
            return null;
        }
    }

    /**
     * Get Json string from Object.
     *
     * @param object the Object
     * @return the Json
     */
    public static String getJsonFromObject(final Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            fail("Cannot encode " + object);
            return null;
        }
    }

    /**
     * Get new AutomationCompositionElementDefinition.
     *
     * @param id the ToscaConceptIdentifier
     * @return a new AutomationCompositionElementDefinition
     */
    public static AutomationCompositionElementDefinition getAcElementDefinition(ToscaConceptIdentifier id) {
        var toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setName("nodeTemplate");
        toscaNodeTemplate.setDerivedFrom("parentNodeTemplate");
        toscaNodeTemplate.setDescription("Description of nodeTemplate");
        toscaNodeTemplate.setVersion("1.2.3");
        toscaNodeTemplate.setType("org.onap.policy.clamp.acm.TestNodeType");
        toscaNodeTemplate.setTypeVersion("1.0.0");

        var acDefinition = new AutomationCompositionElementDefinition();
        acDefinition.setAcElementDefinitionId(id);
        acDefinition.setAutomationCompositionElementToscaNodeTemplate(toscaNodeTemplate);
        return acDefinition;
    }
}
