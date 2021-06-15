/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.dcae.main.parameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to hold/create all parameters for test cases.
 */
public class CommonTestData {
    public static final String PARTICIPANT_GROUP_NAME = "ControlLoopParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> TOPIC_PARAMS = Arrays.asList(getTopicParams());
    private static final String REST_CLIENT_PASSWORD = "password";
    private static final String REST_CLIENT_USER = "admin";
    private static final String REST_CLAMP_HOST = "0.0.0.0";
    private static final String REST_CONSUL_HOST = "0.0.0.0";
    private static final boolean REST_CLAMP_HTTPS = false;
    private static final boolean REST_CONSUL_HTTPS = false;
    private static final boolean REST_CLIENT_AAF = false;

    public static final Coder CODER = new StandardCoder();

    /**
     * Get ParticipantDcaeParameters.
     *
     * @return ParticipantDcaeParameters
     */
    public ParticipantDcaeParameters getParticipantDcaeParameters() {
        try {
            return CODER.convert(getParticipantParameterGroupMap(PARTICIPANT_GROUP_NAME),
                    ParticipantDcaeParameters.class);
        } catch (final CoderException e) {
            throw new RuntimeException("cannot create ParticipantDcaeParameters from map", e);
        }
    }

    /**
     * Returns a property map for a ApexStarterParameterGroup map for test cases.
     *
     * @param name name of the parameters
     *
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getParticipantParameterGroupMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();

        map.put("name", name);
        map.put("checkCount", 10);
        map.put("secCount", 10);
        map.put("jsonBodyConsulPath", "src/main/resources/parameters/consul.json");
        map.put("clampClientParameters", getClampClientParametersMap(false));
        map.put("consulClientParameters", getConsulClientParametersMap(false));
        map.put("intermediaryParameters", getIntermediaryParametersMap(false));
        map.put("clampClientEndPoints", getClampClientEndPoints());
        map.put("consulClientEndPoints", getConsulClientEndPoints());
        return map;
    }

    private Map<String, Object> getConsulClientEndPoints() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("deploy", "/v1/kv/dcae-pmsh:");
        return map;
    }

    private Map<String, Object> getClampClientEndPoints() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("status", "/restservices/clds/v2/loop/getstatus/");
        map.put("create", "/restservices/clds/v2/loop/create/%s?templateName=%s");
        map.put("deploy", "/restservices/clds/v2/loop/deploy/");
        map.put("stop", "/restservices/clds/v2/loop/stop/");
        map.put("delete", "/restservices/clds/v2/loop/delete/");
        map.put("undeploy", "/restservices/clds/v2/loop/undeploy/");
        return map;
    }

    /**
     * Returns a property map for a RestServerParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     * @throws ControlLoopRuntimeException on errors
     */
    public Map<String, Object> getClampClientParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("clientName", "Clamp");
        map.put("https", REST_CLAMP_HTTPS);
        map.put("aaf", REST_CLIENT_AAF);

        if (!isEmpty) {
            map.put("hostname", REST_CLAMP_HOST);
            try {
                map.put("port", NetworkUtil.allocPort());
            } catch (IOException e) {
                throw new ControlLoopRuntimeException(Response.Status.NOT_ACCEPTABLE, "not valid port", e);
            }
            map.put("userName", REST_CLIENT_USER);
            map.put("password", REST_CLIENT_PASSWORD);
        }

        return map;
    }

    /**
     * Returns a property map for a RestServerParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     * @throws ControlLoopRuntimeException on errors
     */
    public Map<String, Object> getConsulClientParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("clientName", "Consul");
        map.put("https", REST_CONSUL_HTTPS);
        map.put("aaf", REST_CLIENT_AAF);

        if (!isEmpty) {
            map.put("hostname", REST_CONSUL_HOST);
            try {
                map.put("port", NetworkUtil.allocPort());
            } catch (IOException e) {
                throw new ControlLoopRuntimeException(Response.Status.NOT_ACCEPTABLE, "not valid port", e);
            }
            map.put("userName", REST_CLIENT_USER);
            map.put("password", REST_CLIENT_PASSWORD);
        }

        return map;
    }

    /**
     * Returns a property map for a intermediaryParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getIntermediaryParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("name", "Participant parameters");
            map.put("reportingTimeInterval", TIME_INTERVAL);
            map.put("description", DESCRIPTION);
            map.put("participantId", getParticipantId());
            map.put("participantType", getParticipantId());
            map.put("clampControlLoopTopics", getTopicParametersMap(false));
        }

        return map;
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getTopicParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("topicSources", TOPIC_PARAMS);
            map.put("topicSinks", TOPIC_PARAMS);
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
        topicParams.setTopic("POLICY-CLRUNTIME-PARTICIPANT");
        topicParams.setTopicCommInfrastructure("dmaap");
        topicParams.setServers(Arrays.asList("localhost"));
        return topicParams;
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static ToscaConceptIdentifier getParticipantId() {
        final ToscaConceptIdentifier participantId = new ToscaConceptIdentifier();
        participantId.setName("DCAEParticipant0");
        participantId.setVersion("1.0.0");
        return participantId;
    }

    /**
     * Nulls out a field within a JSON string.
     *
     * @param json JSON string
     * @param field field to be nulled out
     * @return a new JSON string with the field nulled out
     */
    public String nullifyField(String json, String field) {
        return json.replace(field + "\"", field + "\":null, \"" + field + "Xxx\"");
    }

    /**
     * Create Json response from getstatus call.
     *
     * @param status the status of Partecipant
     * @return the JSON
     * @throws ControlLoopRuntimeException on errors
     */
    public static String createJsonStatus(String status) {
        try {
            File file = new File("src/test/resources/rest/status.json");
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return json.replace("${status}", status);

        } catch (IOException e) {
            throw new ControlLoopRuntimeException(Response.Status.NOT_ACCEPTABLE, "cannot read json file", e);
        }
    }
}
