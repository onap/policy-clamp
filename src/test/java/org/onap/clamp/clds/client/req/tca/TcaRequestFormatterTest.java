/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
 *                             reserved.
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

package org.onap.clamp.clds.client.req.tca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.Test;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Tca;
import org.onap.clamp.clds.model.properties.TcaItem;
import org.onap.clamp.clds.util.JsonUtils;

public class TcaRequestFormatterTest {

    private static final String TCA_POLICY_PROPERTIES_TEMPLATE = "{"
        + " \"domain\": \"measurementsForVfScaling\","
        + " \"metricsPerEventName\": ["
        + "  {"
        + "   \"eventName\": \"???\","
        + "   \"controlLoopSchemaType\": \"VNF\","
        + "   \"policyScope\": \"DCAE\","
        + "   \"policyName\": \"???\","
        + "   \"policyVersion\": \"v0.0.1\","
        + "   \"thresholds\": ["
        + "   ]"
        + "  }"
        + " ]"
        + "}";

    @Test
    public void shouldReturnFormattedTcaPolicyRequest() throws IOException {
        //given
        String service = "TestService";
        String policy = "TestService_scope.PolicyName";
        ClampProperties clampProperties = mock(ClampProperties.class);
        String expectedRequestText = 
            "{ "
            + " \"domain\": \"measurementsForVfScaling\", "
            + " \"metricsPerEventName\": [ "
            + "  { "
            + "   \"eventName\": \"vLoadBalancer\", "
            + "   \"controlLoopSchemaType\": \"VNF\", "
            + "   \"policyScope\": \"DCAE\", "
            + "   \"policyName\": \"TestService_scope.PolicyName\", "
            + "   \"policyVersion\": \"v0.0.1\", "
            + "   \"thresholds\": [] "
            + "  } "
            + " ] "
            + "}";
    
        JsonObject tcaPolicyPropertiesTemplate = JsonUtils.GSON
            .fromJson(TCA_POLICY_PROPERTIES_TEMPLATE, JsonObject.class);

        JsonObject expectedRequest = JsonUtils.GSON.fromJson(expectedRequestText, JsonObject.class);

        ModelProperties modelProperties = mock(ModelProperties.class);
        Tca tca = mock(Tca.class);
        TcaItem tcaItem = mock(TcaItem.class);
        when(clampProperties.getJsonTemplate(any(), any())).thenReturn(tcaPolicyPropertiesTemplate);
        when(tca.getTcaItem()).thenReturn(tcaItem);
        when(tcaItem.getEventName()).thenReturn("vLoadBalancer");
        when(tcaItem.getControlLoopSchemaType()).thenReturn("VNF");

        //when
        JsonObject policyContent = TcaRequestFormatter
            .createPolicyContent(clampProperties, modelProperties, service, policy, tca);

        //then
        assertThat(expectedRequest).isEqualTo(policyContent);
    }
}