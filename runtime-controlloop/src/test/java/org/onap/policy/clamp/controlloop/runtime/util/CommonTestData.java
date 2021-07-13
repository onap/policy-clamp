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

package org.onap.policy.clamp.controlloop.runtime.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {
    private static final Coder CODER = new StandardCoder();

    public static final List<TopicParameters> TOPIC_PARAMS = Arrays.asList(getTopicParams());

    /**
     * Gets the standard Control Loop parameters.
     *
     * @param dbName the database name
     * @return the standard Control Loop parameters
     * @throws ControlLoopRuntimeException on errors reading the control loop parameters
     */
    public static ClRuntimeParameterGroup geParameterGroup(final String dbName) {
        try {
            return CODER.convert(getParameterGroupMap(dbName), ClRuntimeParameterGroup.class);

        } catch (CoderException e) {
            throw new ControlLoopRuntimeException(Status.NOT_ACCEPTABLE, "cannot read Control Loop parameters", e);
        }
    }

    /**
     * Gets the standard Control Loop parameters, as a Map.
     *
     * @param dbName the database name
     * @return the standard Control Loop parameters as Map
     */
    public static Map<String, Object> getParameterGroupMap(final String dbName) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("topicParameterGroup", getTopicParametersMap(false));
        map.put("databaseProviderParameters", getDatabaseProviderParametersMap(dbName));
        return map;
    }

    private static Map<String, Object> getDatabaseProviderParametersMap(String dbName) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("name", "PolicyProviderParameterGroup");
        map.put("implementation", "org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl");
        map.put("databaseDriver", "org.h2.Driver");
        map.put("databaseUrl", "jdbc:h2:mem:testdb" + dbName);
        map.put("databaseUser", "policy");
        map.put("databasePassword", "P01icY");
        map.put("persistenceUnit", "ToscaConceptTest");
        return map;
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    private static Map<String, Object> getTopicParametersMap(final boolean isEmpty) {
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
    private static TopicParameters getTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
        topicParams.setTopic("POLICY-CLRUNTIME-PARTICIPANT");
        topicParams.setTopicCommInfrastructure("dmaap");
        topicParams.setServers(Arrays.asList("localhost"));
        return topicParams;
    }
}
