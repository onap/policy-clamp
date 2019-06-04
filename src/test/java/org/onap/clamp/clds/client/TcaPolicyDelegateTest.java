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
import org.onap.clamp.clds.util.JsonUtils;

@RunWith(MockitoJUnitRunner.class)
public class TcaPolicyDelegateTest {

    private static final String MODEL_BPMN_KEY = "modelBpmnProp";
    private static final String MODEL_PROP_KEY = "modelProp";
    private static final String MODEL_NAME_KEY = "modelName";
    private static final String TEST_KEY = "isTest";
    private static final String USERID_KEY = "userid";
    private static final String TCA_TEMPLATE_KEY = "tca.template";
    private static final String TCA_POLICY_TEMPLATE_KEY = "tca.policy.template";
    private static final String TCA_THRESHOLDS_TEMPLATE_KEY = "tca.thresholds.template";
    private static final String TCA_POLICY_RESPONSE_MESSAGE_KEY = "tcaPolicyResponseMessage";

    private static final String TCA_ID_FROM_JSON = "{\"tca\":[{\"id\":\"id\",\"from\":\"\"}]}";
    private static final String ID_JSON = "{\"id\":{\"r\":[{},{\"serviceConfigurations\":"
            + "[[\"x\",\"+\",\"2\",\"y\"]]}]}}";
    private static final String TCA_TEMPLATE_JSON = "{\"metricsPerEventName\":[{\"thresholds\":[]}]}";
    private static final String TCA_POLICY_TEMPLATE_JSON = "{\"content\":{}}";
    private static final String TCA_THRESHOLDS_TEMPLATE_JSON = "{}";
    private static final String HOLMES_ID_FROM_JSON = "{\"holmes\":[{\"id\":\"\",\"from\":\"\"}]}";
    private static final String NOT_JSON = "not json";

    private static final String RESPONSE_MESSAGE_VALUE = "responseMessage";
    private static final String MODEL_NAME_VALUE = "ModelName";
    private static final String USERID_VALUE = "user";

    private static final String CLDS_MODEL_ID = "id";
    private static final String CLDS_MODEL_PROP_TEXT = "propText";

    @Mock
    private Exchange camelExchange;

    @Mock
    private ClampProperties refProp;

    @Mock
    private PolicyClient policyClient;

    @Mock
    private CldsDao cldsDao;

    @InjectMocks
    private TcaPolicyDelegate tcaPolicyDelegate;

    @Test
    public void shouldExecuteSuccessfully() throws IOException {
        //given
        when(camelExchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(TCA_ID_FROM_JSON);
        when(camelExchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_JSON);
        when(camelExchange.getProperty(eq(MODEL_NAME_KEY))).thenReturn(MODEL_NAME_VALUE);
        when(camelExchange.getProperty(eq(TEST_KEY))).thenReturn(false);
        when(camelExchange.getProperty(eq(USERID_KEY))).thenReturn(USERID_VALUE);

        JsonElement jsonTemplate;
        JsonObject jsonObject;

        jsonTemplate = mock(JsonElement.class);
        when(refProp.getJsonTemplate(eq(TCA_TEMPLATE_KEY), anyString())).thenReturn(jsonTemplate);
        jsonObject = JsonUtils.GSON.fromJson(TCA_TEMPLATE_JSON, JsonObject.class);
        when(jsonTemplate.getAsJsonObject()).thenReturn(jsonObject);

        jsonTemplate = mock(JsonElement.class);
        when(refProp.getJsonTemplate(eq(TCA_POLICY_TEMPLATE_KEY), anyString())).thenReturn(jsonTemplate);
        jsonObject = JsonUtils.GSON.fromJson(TCA_POLICY_TEMPLATE_JSON, JsonObject.class);
        when(jsonTemplate.getAsJsonObject()).thenReturn(jsonObject);

        jsonTemplate = mock(JsonElement.class);
        when(refProp.getJsonTemplate(eq(TCA_THRESHOLDS_TEMPLATE_KEY), anyString())).thenReturn(jsonTemplate);
        jsonObject = JsonUtils.GSON.fromJson(TCA_THRESHOLDS_TEMPLATE_JSON, JsonObject.class);
        when(jsonTemplate.getAsJsonObject()).thenReturn(jsonObject);

        when(policyClient.sendMicroServiceInOther(anyString(), any())).thenReturn(RESPONSE_MESSAGE_VALUE);

        CldsModel cldsModel = new CldsModel();
        cldsModel.setId(CLDS_MODEL_ID);
        cldsModel.setPropText(CLDS_MODEL_PROP_TEXT);
        when(cldsDao.getModelTemplate(eq(MODEL_NAME_VALUE))).thenReturn(cldsModel);

        //when
        tcaPolicyDelegate.execute(camelExchange);

        //then
        verify(camelExchange).setProperty(eq(TCA_POLICY_RESPONSE_MESSAGE_KEY), eq(RESPONSE_MESSAGE_VALUE.getBytes()));
        verify(cldsDao).setModel(eq(cldsModel), eq(USERID_VALUE));
    }

    @Test
    public void shouldExecuteTcaNotFound() {
        //given
        when(camelExchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(HOLMES_ID_FROM_JSON);
        when(camelExchange.getProperty(eq(MODEL_PROP_KEY))).thenReturn(ID_JSON);
        when(camelExchange.getProperty(eq(TEST_KEY))).thenReturn(false);

        //when
        tcaPolicyDelegate.execute(camelExchange);

        //then
        verify(policyClient, never()).sendMicroServiceInOther(any(), any());
    }

    @Test(expected = ModelBpmnException.class)
    public void shouldThrowModelBpmnException() {
        //given
        when(camelExchange.getProperty(eq(MODEL_BPMN_KEY))).thenReturn(NOT_JSON);
        when(camelExchange.getProperty(eq(TEST_KEY))).thenReturn(false);

        //when
        tcaPolicyDelegate.execute(camelExchange);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException() {
        //when
        tcaPolicyDelegate.execute(camelExchange);
    }
}
