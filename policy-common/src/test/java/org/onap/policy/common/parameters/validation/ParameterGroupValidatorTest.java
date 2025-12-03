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

package org.onap.policy.common.parameters.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ParameterGroup;

class ParameterGroupValidatorTest {

    private ParameterGroupValidator validator;

    @Mock
    private ParameterGroup mockParameterGroup;

    @Mock
    private BeanValidationResult mockBeanValidationResult;

    @Mock
    private ConstraintValidatorContext mockContext;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder mockViolationBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new ParameterGroupValidator();
    }

    @Test
    void testIsValid_NullValue() {
        boolean result = validator.isValid(null, mockContext);
        assertTrue(result, "Expected isValid to return true when value is null");
    }

    @Test
    void testIsValid_ValidParameterGroup() {
        when(mockParameterGroup.validate()).thenReturn(mockBeanValidationResult);
        when(mockBeanValidationResult.isValid()).thenReturn(true);

        boolean result = validator.isValid(mockParameterGroup, mockContext);
        assertTrue(result, "Expected isValid to return true when ParameterGroup is valid");

        verify(mockContext, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void testIsValid_InvalidParameterGroup() {
        when(mockParameterGroup.validate()).thenReturn(mockBeanValidationResult);
        when(mockBeanValidationResult.isValid()).thenReturn(false);
        when(mockBeanValidationResult.getMessage()).thenReturn("Invalid parameters");
        when(mockContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(mockViolationBuilder);

        boolean result = validator.isValid(mockParameterGroup, mockContext);
        assertFalse(result, "Expected isValid to return false when ParameterGroup is invalid");

        InOrder inOrder = inOrder(mockContext, mockViolationBuilder);
        inOrder.verify(mockContext).buildConstraintViolationWithTemplate("Invalid parameters");
        inOrder.verify(mockViolationBuilder).addConstraintViolation();
    }
}
