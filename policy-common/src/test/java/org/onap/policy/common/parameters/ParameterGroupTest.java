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

package org.onap.policy.common.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParameterGroupTest {

    private ParameterGroup parameterGroup;

    @BeforeEach
    void setUp() {
        parameterGroup = new ParameterGroup() {
            private String name;
            private BeanValidationResult validationResult = new BeanValidationResult("testGroup", "testObject");

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void setName(final String name) {
                this.name = name;
            }

            @Override
            public BeanValidationResult validate() {
                return validationResult;
            }
        };
    }

    @Test
    void testGetName() {
        String testName = "TestGroupName";
        parameterGroup.setName(testName);
        assertEquals(testName, parameterGroup.getName(), "The group name should match the one set");
    }

    @Test
    void testSetName() {
        String testName = "AnotherGroupName";
        parameterGroup.setName(testName);
        assertEquals(testName, parameterGroup.getName(), "The group name should match the one set");
    }

    @Test
    void testValidate() {
        BeanValidationResult result = parameterGroup.validate();
        assertNotNull(result, "The validation result should not be null");
        assertEquals("testGroup", result.getName(), "The validation result should have the correct group name");
    }

    @Test
    void testIsValid() {
        BeanValidationResult mockValidationResult = mock(BeanValidationResult.class);
        ValidationStatus mockStatus = mock(ValidationStatus.class);

        when(mockStatus.isValid()).thenReturn(true);
        when(mockValidationResult.getStatus()).thenReturn(mockStatus);

        ParameterGroup mockedParameterGroup = spy(parameterGroup);
        doReturn(mockValidationResult).when(mockedParameterGroup).validate();

        assertTrue(mockedParameterGroup.isValid(), "The parameters should be valid");
    }
}
