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

import org.apache.camel.Exchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.exception.ModelBpmnException;

@RunWith(MockitoJUnitRunner.class)
public class GuardPolicyDelegateTest {

    private static final String TEST_KEY = "isTest";
    private static final String MODEL_BPMN_KEY = "modelBpmnProp";
    private static final String MODEL_PROP_KEY = "modelProp";
    private static final String POLICY_ID_FROM_JSON = "{policy:[{id:guard,from:''}]}";
    private static final String ID_WITH_CHAIN_JSON = "{guard:{q:["
            + "{name:timeout,value:200},"
            + "{policyConfigurations:["
            + "[{name:maxRetries,value:3},"
            + "{name:retryTimeLimit,value:800},"
            + "{name:enableGuardPolicy,value:on}]]}]}}";
    private static final String SIMPLE_JSON = "{}";
    private static final String NOT_JSON = "not json";

    @Mock
    private Exchange exchange;

    @Mock
    private PolicyClient policyClient;

    @InjectMocks
    private GuardPolicyDelegate guardPolicyDelegate;

    @Test
    public void shouldExecuteSuccessfully() {
        // given
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(POLICY_ID_FROM_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_WITH_CHAIN_JSON);

        // when
        guardPolicyDelegate.execute(exchange);

        // then
        verify(policyClient).sendGuardPolicy(any(), any(), any(), any());
    }

    @Test
    public void shouldExecutePolicyNotFound() {
        // given
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(SIMPLE_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(SIMPLE_JSON);

        // when
        guardPolicyDelegate.execute(exchange);

        // then
        verify(policyClient, never()).sendGuardPolicy(any(), any(), any(), any());
    }

    @Test(expected = ModelBpmnException.class)
    public void shouldThrowModelBpmnException() {
        // given
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(true);
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(NOT_JSON);

        // when
        guardPolicyDelegate.execute(exchange);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException() {
        // when
        guardPolicyDelegate.execute(exchange);
    }
}
