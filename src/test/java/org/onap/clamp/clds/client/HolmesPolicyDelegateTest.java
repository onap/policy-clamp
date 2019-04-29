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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.exception.ModelBpmnException;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.properties.Holmes;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.util.JsonUtils;

@RunWith(MockitoJUnitRunner.class)
public class HolmesPolicyDelegateTest {

    private static final String ID_JSON = "{\"id\":{\"r\":[{},{\"serviceConfigurations\":"
            + "[[\"x\",\"+\",\"2\",\"y\"]]}]}}";
    private static final String METRICS_JSON = "{\"metricsPerEventName\":[{\"thresholds\":[]}]}";
    private static final String CONTENT_JSON = "{\"content\":{}}";
    private static final String NULL_JSON = "{}";
    private static final String HOLMES_ID_FROM_JSON = "{\"holmes\":[{\"id\":\"id\",\"from\":\"\"}]}";
    private static final String TCA_ID_FROM_JSON = "{\"tca\":[{\"id\":\"\",\"from\":\"\"}]}";
    private static final String CORRELATION_LOGIC_JSON = "{\"name\":\"correlationalLogic\"}";
    private static final String NOT_JSON = "not json";
    private static final String MODEL_BPMN_KEY = "modelBpmnProp";
    private static final String MODEL_PROP_KEY = "modelProp";
    private static final String MODEL_NAME_KEY = "modelName";
    private static final String TEST_KEY = "isTest";
    private static final String USERID_KEY = "userid";
    private static final String TCA_TEMPLATE_KEY = "tca.template";
    private static final String TCA_POLICY_TEMPLATE_KEY = "tca.policy.template";
    private static final String TCA_THRESHOLDS_TEMPLATE_KEY = "tca.thresholds.template";
    private static final String HOLMES_POLICY_RESPONSE_MESSAGE_KEY = "holmesPolicyResponseMessage";
    private static final String RESPONSE_MESSAGE_VALUE = "responseMessage";
    private static final String MODEL_NAME_VALUE = "model.name";
    private static final String CONTROL_NAME_VALUE = "control.name";
    private static final String USERID_VALUE = "user";
    private static final String CLDS_MODEL_ID = "id";
    private static final String CLDS_MODEL_PROP_TEXT = "propText";

    @Mock
    private Exchange exchange;

    @Mock
    private PolicyClient policyClient;

    @Mock
    private ClampProperties clampProperties;

    @Mock
    private CldsDao cldsDao;

    @InjectMocks
    private HolmesPolicyDelegate holmesPolicyDelegateTest;

    @Test
    public void shouldExecuteSuccessfully() throws IOException {
        // given
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(HOLMES_ID_FROM_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_JSON);
        when(exchange.getProperty(eq(MODEL_NAME_KEY))).thenReturn(MODEL_NAME_VALUE);
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);
        when(exchange.getProperty(eq(USERID_KEY))).thenReturn(USERID_VALUE);

        JsonElement jsonTemplateA = mock(JsonElement.class);
        when(clampProperties.getJsonTemplate(eq(TCA_TEMPLATE_KEY), anyString())).thenReturn(jsonTemplateA);
        when(jsonTemplateA.getAsJsonObject()).thenReturn(getJsonObject(METRICS_JSON));

        JsonElement jsonTemplateB = mock(JsonElement.class);
        when(clampProperties.getJsonTemplate(eq(TCA_POLICY_TEMPLATE_KEY), anyString())).thenReturn(jsonTemplateB);
        when(jsonTemplateB.getAsJsonObject()).thenReturn(getJsonObject(CONTENT_JSON));

        JsonElement jsonTemplateC = mock(JsonElement.class);
        when(clampProperties.getJsonTemplate(eq(TCA_THRESHOLDS_TEMPLATE_KEY), anyString())).thenReturn(jsonTemplateC);
        when(jsonTemplateC.getAsJsonObject()).thenReturn(getJsonObject(NULL_JSON));

        when(policyClient.sendBasePolicyInOther(anyString(), anyString(), any(), anyString()))
                .thenReturn(RESPONSE_MESSAGE_VALUE);

        CldsModel cldsModel = new CldsModel();
        cldsModel.setId(CLDS_MODEL_ID);
        cldsModel.setPropText(CLDS_MODEL_PROP_TEXT);
        when(cldsDao.getModelTemplate(eq(MODEL_NAME_VALUE))).thenReturn(cldsModel);

        // when
        holmesPolicyDelegateTest.execute(exchange);

        // then
        verify(exchange).setProperty(eq(HOLMES_POLICY_RESPONSE_MESSAGE_KEY), eq(RESPONSE_MESSAGE_VALUE.getBytes()));
        verify(cldsDao).setModel(eq(cldsModel), eq(USERID_VALUE));
    }

    @Test
    public void shouldExecuteHolmesNotFound() {
        // given
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(TCA_ID_FROM_JSON);
        when(exchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_JSON);
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);

        // when
        holmesPolicyDelegateTest.execute(exchange);

        // then
        verify(policyClient, never()).sendBasePolicyInOther(anyString(), anyString(), any(), anyString());
        verify(exchange, never()).setProperty(eq(HOLMES_POLICY_RESPONSE_MESSAGE_KEY), any());
        verify(cldsDao, never()).setModel(any(), anyString());
    }

    @Test(expected = ModelBpmnException.class)
    public void shouldThrowModelBpmnException() {
        // given
        when(exchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(NOT_JSON);
        when(exchange.getProperty(eq(TEST_KEY))).thenReturn(false);

        // when
        holmesPolicyDelegateTest.execute(exchange);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException() {
        // when
        holmesPolicyDelegateTest.execute(exchange);
    }

    @Test
    public void shouldDoFormatHolmesConfigBodySuccessfully() {
        // given
        ModelProperties prop = new ModelProperties(null, CONTROL_NAME_VALUE, null, false,
                HOLMES_ID_FROM_JSON, "{\"id\":" + CORRELATION_LOGIC_JSON + "}");
        Holmes holmes = prop.getType(Holmes.class);

        // when
        String result = HolmesPolicyDelegate.formatHolmesConfigBody(prop, holmes);

        // then
        assertEquals(CONTROL_NAME_VALUE + "$$$" + CORRELATION_LOGIC_JSON, result);
    }

    private static JsonObject getJsonObject(String jsonText) {
        return JsonUtils.GSON.fromJson(jsonText, JsonObject.class);
    }
}
