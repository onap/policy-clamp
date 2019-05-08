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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.apache.camel.Exchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.exception.ModelBpmnException;
import org.onap.policy.controlloop.policy.builder.BuilderException;

@RunWith(MockitoJUnitRunner.class)
public class OperationalPolicyDelegateTest {

    private static final String TEST_KEY = "isTest";
    private static final String MODEL_BPMN_KEY = "modelBpmnProp";
    private static final String MODEL_PROP_KEY = "modelProp";
    private static final String RECIPE_TOPIC_KEY = "op.recipeTopic";
    private static final String MESSAGE_KEY = "operationalPolicyResponseMessage";
    private static final String SERVICE_NAME = "service.name";
    private static final String POLICY_ID_FROM_JSON = "{policy:[{id:Oper12,from:''}]}";
    private static final String ID_WITH_CHAIN_JSON = "{Oper12:{ab:["
            + "{name:timeout,value:500},"
            + "{policyConfigurations:["
            + "[{name:maxRetries,value:5},"
            + "{name:retryTimeLimit,value:1000},"
            + "{name:recipe,value:go},"
            + "{name:targetResourceId,"
            + "value:resid234}]]}]},"
            + "global:[{name:service,value:" + SERVICE_NAME + "}]}";
    private static final String SIMPLE_JSON = "{}";
    private static final String NOT_JSON = "not json";
    private static final String MESSAGE_VALUE = "message";
    private static final String RECIPE_TOPIC_VALUE = "recipe.topic";

    @Mock
    private Exchange exchange;

    @Mock
    private PolicyClient policyClient;

    @Mock
    private ClampProperties refProp;

    @InjectMocks
    private OperationalPolicyDelegate operationalPolicyDelegate;

    @Test
    public void shouldExecuteSuccessfully() throws BuilderException, UnsupportedEncodingException {
        // given
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(true);
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(POLICY_ID_FROM_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_WITH_CHAIN_JSON);
        when(policyClient.sendBrmsPolicy(any(), any(), any())).thenReturn(MESSAGE_VALUE);
        when(refProp.getStringValue(eq(RECIPE_TOPIC_KEY), eq(SERVICE_NAME))).thenReturn(RECIPE_TOPIC_VALUE);

        // when
        operationalPolicyDelegate.execute(exchange);

        // then
        verify(exchange).setProperty(eq(MESSAGE_KEY), eq(MESSAGE_VALUE.getBytes()));
    }

    @Test
    public void shouldExecutePolicyNotFound() throws BuilderException, UnsupportedEncodingException {
        // given
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(SIMPLE_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(SIMPLE_JSON);

        // when
        operationalPolicyDelegate.execute(exchange);

        // then
        verify(policyClient, never()).sendBrmsPolicy(any(), any(), any());
    }

    @Test(expected = ModelBpmnException.class)
    public void shouldThrowModelBpmnException() throws BuilderException, UnsupportedEncodingException {
        // given
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(true);
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(NOT_JSON);

        // when
        operationalPolicyDelegate.execute(exchange);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException() throws BuilderException, UnsupportedEncodingException {
        // when
        operationalPolicyDelegate.execute(exchange);
    }
}
