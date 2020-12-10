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

import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.Test;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.ResourceFileUtils;
import org.onap.clamp.loop.template.PolicyModel;
import org.skyscreamer.jsonassert.JSONAssert;

public class MicroServicePayloadTest {

    @Test
    public void testPayloadConstruction() throws IOException {
        MicroServicePolicy policy = new MicroServicePolicy("testPolicy", new PolicyModel(
                "onap.policies.monitoring.cdap.tca.hi.lo.app",
                ResourceFileUtils.getResourceAsString("tosca/tosca_example.yaml"), "1.0.0"), false, null, null, null,
                null);
        policy.setConfigurationsJson(JsonUtils.GSON.fromJson(
                ResourceFileUtils.getResourceAsString("tosca/micro-service-policy-properties.json"), JsonObject.class));
        JSONAssert.assertEquals(ResourceFileUtils.getResourceAsString("tosca/micro-service-policy-payload.json"),
                policy.createPolicyPayload(), false);
    }
}
