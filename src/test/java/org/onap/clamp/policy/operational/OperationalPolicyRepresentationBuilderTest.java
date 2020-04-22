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

package org.onap.clamp.policy.operational;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.Test;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.loop.service.Service;
import org.skyscreamer.jsonassert.JSONAssert;

public class OperationalPolicyRepresentationBuilderTest {

    @Test
    public void testOperationalPolicyPayloadConstruction() throws IOException {
        JsonObject jsonModel = new GsonBuilder().create()
                .fromJson(ResourceFileUtil.getResourceAsString("tosca/model-properties.json"), JsonObject.class);
        Service service = new Service(jsonModel.get("serviceDetails").getAsJsonObject(),
                jsonModel.get("resourceDetails").getAsJsonObject(), "1.0");

        JsonObject jsonSchema = OperationalPolicyRepresentationBuilder.generateOperationalPolicySchema(service);

        assertThat(jsonSchema).isNotNull();

        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("tosca/operational-policy-json-schema.json"),
                new GsonBuilder().create().toJson(jsonSchema), false);
    }

    @Test
    public void testOperationalPolicyPayloadConstructionForCds() throws IOException {
        JsonObject jsonModel = new GsonBuilder().create()
                .fromJson(ResourceFileUtil.getResourceAsString("tosca/model-properties-cds.json"), JsonObject.class);
        Service service = new Service(jsonModel.get("serviceDetails").getAsJsonObject(),
                jsonModel.get("resourceDetails").getAsJsonObject(),
                "1.0");

        JsonObject jsonSchema = OperationalPolicyRepresentationBuilder.generateOperationalPolicySchema(service);
        assertThat(jsonSchema).isNotNull();
        JSONAssert.assertEquals(
                ResourceFileUtil.getResourceAsString("tosca/operational-policy-cds-payload-with-list.json"),
                new GsonBuilder().create().toJson(jsonSchema), false);
    }
}
