/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Samsung. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */

package org.onap.clamp.clds.client;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.exception.ModelBpmnException;

@RunWith(MockitoJUnitRunner.class)
public class HolmesPolicyDeleteDelegateTest {

    private static final String MODEL_BPMN_KEY = "modelBpmnProp";
    private static final String MODEL_PROP_KEY = "modelProp";
    private static final String TEST_KEY = "isTest";

    private static final String HOLMES_ID_FROM_JSON = "{\"holmes\":[{\"id\":\"\",\"from\":\"\"}]}";
    private static final String TCA_ID_FROM_JSON = "{\"tca\":[{\"id\":\"\",\"from\":\"\"}]}";
    private static final String ID_JSON = "{\"id\":\"\"}";
    private static final String NOT_JSON = "not json";

    @Mock
    private Exchange exchange;

    @Mock
    private PolicyClient policyClient;

    @InjectMocks
    private HolmesPolicyDeleteDelegate holmesPolicyDeleteDelegate;

    @Test
    public void shouldExecuteSuccessfully() {
        // given
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(HOLMES_ID_FROM_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_JSON);
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);

        // when
        holmesPolicyDeleteDelegate.execute(exchange);

        // then
        verify(policyClient).deleteBasePolicy(any());
    }

    @Test
    public void shouldExecuteHolmesNotFound() {
        // given
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(TCA_ID_FROM_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_JSON);
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);

        // when
        holmesPolicyDeleteDelegate.execute(exchange);

        // then
        verify(policyClient, never()).deleteBasePolicy(any());
    }

    @Test(expected = ModelBpmnException.class)
    public void shouldThrowModelBpmnException() {
        // given
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(NOT_JSON);
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);

        // when
        holmesPolicyDeleteDelegate.execute(exchange);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException() {
        // when
        holmesPolicyDeleteDelegate.execute(exchange);
    }
}
