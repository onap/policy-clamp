/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.message.bus.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.onap.policy.common.parameters.topic.TopicParameterGroup;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to perform unit test of {@link TopicParameterGroup}.
 *
 * @author Ajith Sreekumar (ajith.sreekumar@est.tech)
 */
class TopicParameterGroupTest {
    private static final CommonTestData testData = new CommonTestData();
    private static final Coder coder = new StandardCoder();
    private final String packageDir = "src/test/resources/org/onap/policy/common/message/bus/parameters/";

    @Test
    void test() throws CoderException {
        final TopicParameterGroup topicParameterGroup =
                testData.toObject(testData.getTopicParameterGroupMap(false), TopicParameterGroup.class);
        final ValidationResult validationResult = topicParameterGroup.validate();
        assertTrue(validationResult.isValid());
        assertEquals(CommonTestData.TOPIC_PARAMS, topicParameterGroup.getTopicSinks());
        assertEquals(CommonTestData.TOPIC_PARAMS, topicParameterGroup.getTopicSources());

        // these should default to true
        assertTrue(new TopicParameters().isManaged());
        assertTrue(coder.decode("{}", TopicParameters.class).isManaged());

        // but can be overridden
        assertFalse(coder.decode("{'managed':false}".replace('\'', '"'), TopicParameters.class).isManaged());
    }

    @Test
    void testValidate() {
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParameterGroupMap(false), TopicParameterGroup.class);
        final ValidationResult result = topicParameterGroup.validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());
    }

    @Test
    void test_valid() throws Exception {
        String json = testData.getParameterGroupAsString(
            packageDir + "TopicParameters_valid.json");
        TopicParameterGroup topicParameterGroup = coder.decode(json, TopicParameterGroup.class);
        final ValidationResult result = topicParameterGroup.validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());
    }

    @Test
    void test_invalid() throws Exception {
        String json = testData.getParameterGroupAsString(
            packageDir + "TopicParameters_invalid.json");
        TopicParameterGroup topicParameterGroup = coder.decode(json, TopicParameterGroup.class);
        final ValidationResult result = topicParameterGroup.validate();
        assertFalse(result.isValid());
        assertTrue(result.getResult().contains("INVALID"));
    }

    @Test
    void test_missing_mandatory_params() throws Exception {
        String json = testData.getParameterGroupAsString(
            packageDir + "TopicParameters_missing_mandatory.json");
        TopicParameterGroup topicParameterGroup = coder.decode(json, TopicParameterGroup.class);
        final ValidationResult result = topicParameterGroup.validate();
        assertTrue(result.getResult().contains("Mandatory parameters are missing"));
        assertFalse(result.isValid());
    }

    @Test
    void test_allParams() throws Exception {
        String json = testData.getParameterGroupAsString(
            packageDir + "TopicParameters_all_params.json");
        TopicParameterGroup topicParameterGroup = coder.decode(json, TopicParameterGroup.class);
        final ValidationResult result = topicParameterGroup.validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());
        assertTrue(checkIfAllParamsNotEmpty(topicParameterGroup.getTopicSinks()));
        assertTrue(checkIfAllParamsNotEmpty(topicParameterGroup.getTopicSources()));
    }

    /**
     * Method to check if all parameters in TopicParameters are set.
     * Any parameters added to @link TopicParameters or @link BusTopicParams must be added to
     * TopicParameters_all_params.json.
     *
     * @param topicParametersList list of topic parameters
     * @return true if all parameters are not empty (if string) or true (if boolean)
     * @throws Exception the exception
     */
    private boolean checkIfAllParamsNotEmpty(List<TopicParameters> topicParametersList) throws Exception {
        for (TopicParameters topicParameters : topicParametersList) {
            Field[] fields = BusTopicParams.class.getDeclaredFields();
            for (Field field : fields) {
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                    Object parameter = new PropertyDescriptor(field.getName(), TopicParameters.class).getReadMethod()
                        .invoke(topicParameters);
                    if ((parameter instanceof String && StringUtils.isBlank(parameter.toString()))
                        || (parameter instanceof Boolean && !(Boolean) parameter)
                        || (parameter instanceof Number && ((Number) parameter).longValue() == 0)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
