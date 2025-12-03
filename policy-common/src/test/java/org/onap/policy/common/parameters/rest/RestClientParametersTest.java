/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.parameters.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;

class RestClientParametersTest {

    private RestClientParameters params;

    @BeforeEach
    void setUp() {
        params = new RestClientParameters();
    }

    @Test
    void testValidate_ValidParameters() {
        params.setHostname("localhost");
        params.setClientName("testClient");
        params.setPort(8080);

        BeanValidationResult result = params.validate();

        assertEquals(ValidationStatus.CLEAN, result.getStatus(), "Expected the parameters to be valid");
        assertNull(result.getResult(), "Expected no validation errors");
    }

    @Test
    void testValidate_InvalidHostname() {
        params.setHostname("");
        params.setClientName("testClient");
        params.setPort(8080);

        BeanValidationResult result = params.validate();

        assertEquals(ValidationStatus.INVALID, result.getStatus(), "Expected the parameters to be invalid");
        assertTrue(result.getResult().contains("hostname") && result.getResult().contains("is blank"),
            "Expected invalid hostname error message");
    }

    @Test
    void testValidate_InvalidClientName() {
        params.setHostname("localhost");
        params.setClientName("");
        params.setPort(8080);

        BeanValidationResult result = params.validate();

        assertEquals(ValidationStatus.INVALID, result.getStatus(), "Expected the parameters to be invalid");
        assertTrue(result.getResult().contains("clientName") && result.getResult().contains("is blank"),
            "Expected invalid clientName error message");
    }

    @Test
    void testValidate_InvalidPort() {
        params.setHostname("localhost");
        params.setClientName("testClient");
        params.setPort(-1);

        BeanValidationResult result = params.validate();

        assertEquals(ValidationStatus.INVALID, result.getStatus(), "Expected the parameters to be invalid");
        assertTrue(result.getResult().contains("port") && result.getResult().contains("is not valid"),
            "Expected invalid port error message");
    }

    @Test
    void testValidate_MultipleInvalidParameters() {
        params.setHostname("");
        params.setClientName("");
        params.setPort(-1);

        BeanValidationResult result = params.validate();

        assertEquals(ValidationStatus.INVALID, result.getStatus(), "Expected the parameters to be invalid");

        assertTrue(result.getResult().contains("hostname") && result.getResult().contains("is blank"),
            "Expected invalid hostname error message");

        assertTrue(result.getResult().contains("clientName") && result.getResult().contains("is blank"),
            "Expected invalid clientName error message");

        assertTrue(result.getResult().contains("port") && result.getResult().contains("is not valid"),
            "Expected invalid port error message");
    }

    @Test
    void testGetAndSetName() {
        String name = "testClient";
        params.setName(name);
        assertEquals(name, params.getName(), "Expected the client name to be set and retrieved correctly");
    }
}
