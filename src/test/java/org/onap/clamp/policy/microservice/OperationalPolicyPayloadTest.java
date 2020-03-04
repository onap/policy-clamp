/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.policy.microservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.policy.operational.LegacyOperationalPolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.skyscreamer.jsonassert.JSONAssert;

public class OperationalPolicyPayloadTest {

    @Test
    public void testOperationalPolicyPayloadConstruction() throws IOException {
        JsonObject jsonConfig = new GsonBuilder().create().fromJson(
                ResourceFileUtil.getResourceAsString("tosca/operational-policy-properties.json"), JsonObject.class);
        OperationalPolicy policy = new OperationalPolicy("testPolicy", null, jsonConfig, null, null, null, null);

        assertThat(policy.createPolicyPayloadYaml())
                .isEqualTo(ResourceFileUtil.getResourceAsString("tosca/operational-policy-payload.yaml"));

        assertThat(policy.createPolicyPayload())
                .isEqualTo(ResourceFileUtil.getResourceAsString("tosca/operational-policy-payload.json"));
    }

    @Test
    public void testLegacyOperationalPolicyPayloadConstruction() throws IOException {
        JsonObject jsonConfig = new GsonBuilder().create().fromJson(
                ResourceFileUtil.getResourceAsString("tosca/operational-policy-properties.json"), JsonObject.class);
        assertThat(LegacyOperationalPolicy.createPolicyPayloadYamlLegacy(jsonConfig.get("operational_policy")))
                .isEqualTo(ResourceFileUtil.getResourceAsString("tosca/operational-policy-payload-legacy.yaml"));
    }

    @Test
    public void testGuardPolicyEmptyPayloadConstruction() throws IOException {
        JsonObject jsonConfig = new GsonBuilder().create().fromJson(
                ResourceFileUtil.getResourceAsString("tosca/operational-policy-no-guard-properties.json"),
                JsonObject.class);
        OperationalPolicy policy = new OperationalPolicy("testPolicy", null, jsonConfig, null, null, null, null);
        Map<String, String> guardsMap = policy.createGuardPolicyPayloads();
        assertThat(guardsMap).isEmpty();
        assertThat(guardsMap.entrySet()).isEmpty();
    }

    @Test
    public void testGuardPolicyPayloadConstruction() throws IOException {
        JsonObject jsonConfig = new GsonBuilder().create().fromJson(
                ResourceFileUtil.getResourceAsString("tosca/operational-policy-properties.json"), JsonObject.class);
        OperationalPolicy policy = new OperationalPolicy("testPolicy", null, jsonConfig, null, null, null, null);

        Map<String, String> guardsMap = policy.createGuardPolicyPayloads();

        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("tosca/guard1-policy-payload.json"),
                guardsMap.get("guard.minmax.new"), false);

        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("tosca/guard2-policy-payload.json"),
                guardsMap.get("guard.frequency.new"), false);
    }
}
