/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kubernetes.parameters;

import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.onap.policy.common.utils.coder.MapperFactory;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public class CommonTestData {

    public static final String TOSCA_TEMPLATE_YAML = "clamp/acm/test/participant-kubernetes-helm.yaml";
    public static final String PARTICIPANT_GROUP_NAME = "AutomationCompositionParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> SINK_TOPIC_PARAMS = List.of(getTopicParams());
    public static final List<TopicParameters> SOURCE_TOPIC_PARAMS = List.of(getTopicParams(), getSyncTopicParams());
    private static final ObjectMapper YAML_MAPPER = MapperFactory.createYamlMapper();
    private static final ObjectMapper MAPPER = MapperFactory.createJsonMapper();
    private static final UUID AC_ID = UUID.randomUUID();

    /**
     * Get ParticipantK8sParameters.
     *
     * @return ParticipantK8sParameters
     */
    public static ParticipantK8sParameters getParticipantK8sParameters() {
        var json = getJsonFromObject(getParticipantK8sParametersMap(PARTICIPANT_GROUP_NAME));
        return getObjectFromJson(json, ParticipantK8sParameters.class);
    }

    /**
     * Returns a property map for a ParticipantK8sParameters map for test cases.
     *
     * @param name name of the parameters
     *
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getParticipantK8sParametersMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();

        map.put("name", name);
        map.put("intermediaryParameters", getIntermediaryParametersMap(false));
        map.put("localChartDirectory", getLocalChartDir());
        map.put("infoFileName", getInfoFileName());
        return map;
    }


    /**
     * Returns string value of local chart Directory.
     * @return a string value
     */
    public static String getLocalChartDir() {
        return "/var/helm-manager/local-charts";
    }

    /**
     * Returns string value of Info file name.
     * @return string value
     */
    public static String getInfoFileName() {
        return "CHART-INFO.json";
    }



    /**
     * Returns a property map for a intermediaryParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getIntermediaryParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("name", "Participant parameters");
            map.put("reportingTimeIntervalMs", TIME_INTERVAL);
            map.put("description", DESCRIPTION);
            map.put("participantId", getParticipantId());
            map.put("clampAutomationCompositionTopics", getTopicParametersMap(false));
            map.put("participantSupportedElementTypes", new ArrayList<>());
            map.put("topics", new Topics("policy-acruntime-participant", "acm-ppnt-sync"));
        }

        return map;
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getParticipantId() {
        return UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c02");
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getTopicParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("topicSources", SOURCE_TOPIC_PARAMS);
            map.put("topicSinks", SINK_TOPIC_PARAMS);
        }
        return map;
    }

    /**
     * Returns topic parameters for test cases.
     *
     * @return topic parameters
     */
    public static TopicParameters getTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
        topicParams.setTopic("policy-acruntime-participant");
        topicParams.setTopicCommInfrastructure("NOOP");
        topicParams.setServers(List.of("localhost"));
        return topicParams;
    }

    /**
     * Returns sync topic parameters for test cases.
     *
     * @return topic parameters
     */
    public static TopicParameters getSyncTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
        topicParams.setTopic("acm-ppnt-sync");
        topicParams.setTopicCommInfrastructure("NOOP");
        topicParams.setServers(List.of("localhost"));
        return topicParams;
    }

    /**
     * Get automation composition id.
     * @return UUID automationCompositionId
     */
    public static UUID getAutomationCompositionId() {
        return AC_ID;
    }

    /**
     * Create an InstanceElementDto.
     *
     * @return an InstanceElementDto
     */
    public static InstanceElementDto createInstanceElementDto(Map<String, Object> inProperties) {
        return new InstanceElementDto(getAutomationCompositionId(), UUID.randomUUID(), inProperties, new HashMap<>());
    }

    /**
     * Create an compositionElementDto.
     *
     * @return an compositionElementDto
     */
    public static CompositionElementDto createCompositionElementDto() {
        return new CompositionElementDto(getAutomationCompositionId(), null,
                Map.of("uninitializedToPassiveTimeout", 100, "podStatusCheckInterval", "30"), null);
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
            return YAML_MAPPER.readValue(yaml, ToscaServiceTemplate.class);
        } catch (JsonProcessingException e) {
            fail("Cannot read or decode " + yaml);
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
     * Get Object from json.
     *
     * @param json the resource
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
}
