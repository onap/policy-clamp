/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.policy.pdpgroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.Test;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * This class tests the PdpGroupPayload features.
 */
public class PdpGroupPayloadTest {

    @Test
    public void setupPdpGroup() throws IOException {
        JsonArray operations = new JsonArray();
        operations.add("POST/pdpgroup1/pdpsubgroup1/policyname1/1.0.0");
        operations.add("POST/pdpgroup1/pdpsubgroup1/policyname2/1.0.0");
        operations.add("POST/pdpgroup1/pdpsubgroup1/policyname1/2.0.0");
        operations.add("DELETE/pdpgroup2/pdpsubgroup2/policyname1/1.0.0");
        operations.add("POST/pdpgroup2/pdpsubgroup2/policyname1/2.0.0");
        operations.add("DELETE/pdpgroup2/pdpsubgroup2/policyname2/1.0.0");
        JsonObject listOfOperations = new JsonObject();
        listOfOperations.add(PdpGroupPayload.PDP_OPERATIONS, operations);

        PdpGroupPayload pdpGroupPayload = new PdpGroupPayload(listOfOperations);
        JSONAssert.assertEquals(
                ResourceFileUtils.getResourceAsString("example/policy/pdp-group-multi-policies-payload.json"),
                pdpGroupPayload.generatePdpGroupPayload(), false);
    }
}
