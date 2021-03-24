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

package org.onap.policy.clamp.controlloop.participant.simulator.main.parameters;

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
import org.onap.policy.common.parameters.ParameterGroup;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to hold/create all parameters for test cases.
 */
public class CommonTestData {
    public static final String PARTICIPANT_GROUP_NAME = "ControlLoopParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> TOPIC_PARAMS = Arrays.asList(getTopicParams());
    private static final String REST_SERVER_PASSWORD = "zb!XztG34";
    private static final String REST_SERVER_USER = "healthcheck";
    private static final int REST_SERVER_PORT = 6969;
    private static final String REST_SERVER_HOST = "0.0.0.0";
    private static final boolean REST_SERVER_HTTPS = true;
    private static final boolean REST_SERVER_AAF = false;

    public static final Coder coder = new StandardCoder();

    /**
     * Converts the contents of a map to a parameter class.
     *
     * @param source property map
     * @param clazz class of object to be created from the map
     * @return a new object represented by the map
     */
    public <T extends ParameterGroup> T toObject(final Map<String, Object> source, final Class<T> clazz) {
        try {
            return coder.convert(source, clazz);
        } catch (final CoderException e) {
            throw new RuntimeException("cannot create " + clazz.getName() + " from map", e);
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
        map.put("restServerParameters", getRestServerParametersMap(false));
        map.put("intermediaryParameters", getIntermediaryParametersMap(false));
        map.put("databaseProviderParameters", getDatabaseProviderParametersMap(false));
        return map;
    }

    /**
     * Returns a property map for a RestServerParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getRestServerParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("https", REST_SERVER_HTTPS);
        map.put("aaf", REST_SERVER_AAF);

        if (!isEmpty) {
            map.put("host", REST_SERVER_HOST);
            map.put("port", REST_SERVER_PORT);
            map.put("userName", REST_SERVER_USER);
            map.put("password", REST_SERVER_PASSWORD);
        }

        return map;
    }

    /**
     * Returns a property map for a databaseProviderParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getDatabaseProviderParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("name", "PolicyProviderParameterGroup");
            map.put("implementation", "org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl");
            map.put("databaseDriver", "org.h2.Driver");
            map.put("databaseUrl", "jdbc:h2:mem:testdb");
            map.put("databaseUser", "policy");
            map.put("databasePassword", "P01icY");
            map.put("persistenceUnit", "ToscaConceptTest");
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
        final ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("CDSParticipant0", "1.0.0");
        return participantId;
    }

    /**
     * Gets the standard participant parameters.
     *
     * @param port port to be inserted into the parameters
     * @return the standard participant parameters
     */
    public ParticipantSimulatorParameters getParticipantParameterGroup(int port) {
        try {
            return coder.decode(getParticipantParameterGroupAsString(port), ParticipantSimulatorParameters.class);

        } catch (CoderException e) {
            throw new ControlLoopRuntimeException(Response.Status.NOT_ACCEPTABLE,
                    "cannot read participant parameters", e);
        }
    }

    /**
     * Gets the standard participant parameters, as a String.
     *
     * @param port port to be inserted into the parameters
     * @return the standard participant parameters
     */
    public static String getParticipantParameterGroupAsString(int port) {

        try {
            File file = new File(getParamFile());
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

            json = json.replace("${port}", String.valueOf(port));
            json = json.replace("${dbName}", "jdbc:h2:mem:testdb");

            return json;

        } catch (IOException e) {
            throw new ControlLoopRuntimeException(Response.Status.NOT_ACCEPTABLE,
                    "cannot read participant parameters", e);

        }
    }

    /**
     * Gets the full path to the parameter file, which may vary depending on whether or
     * not this is an end-to-end test.
     *
     * @return the parameter file name
     */
    private static String getParamFile() {
        String paramFile = "src/test/resources/parameters/TestParametersStd.json";
        return paramFile;
    }

    /**
     * Nulls out a field within a JSON string.
     * @param json JSON string
     * @param field field to be nulled out
     * @return a new JSON string with the field nulled out
     */
    public String nullifyField(String json, String field) {
        return json.replace(field + "\"", field + "\":null, \"" + field + "Xxx\"");
    }
}
