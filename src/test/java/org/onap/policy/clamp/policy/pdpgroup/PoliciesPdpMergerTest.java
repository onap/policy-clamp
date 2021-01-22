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

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test class validates the PdpGroupAnalyzer class.
 */
public class PoliciesPdpMergerTest {

    private static String pdpGroupsJson;

    @BeforeClass
    public static void setupPdpGroup() {
        // Create Pdp Groups
        PdpSubGroup pdpSubgroup1 = new PdpSubGroup();
        pdpSubgroup1.setPdpType("subGroup1");
        pdpSubgroup1.setSupportedPolicyTypes(
                Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.*", "1.0.0")));

        PdpSubGroup pdpSubgroup2 = new PdpSubGroup();
        pdpSubgroup2.setPdpType("subGroup2");
        pdpSubgroup2.setSupportedPolicyTypes(
                Arrays.asList(new ToscaPolicyTypeIdentifier("onap.policies.monitoring.tcagen2", "1.0.0"),
                        new ToscaPolicyTypeIdentifier("onap.policies.controlloop.operational.common.Drools", "1.0.0")));
        pdpSubgroup2.setPolicies(Arrays.asList(
                new ToscaPolicyIdentifier("MICROSERVICE_vLoadBalancerMS_v1_0_tcagen2_1_0_0_AV0", "1.0.0")));

        PdpSubGroup pdpSubgroup3 = new PdpSubGroup();
        pdpSubgroup3.setPdpType("subGroup3");
        pdpSubgroup3.setSupportedPolicyTypes(
                Arrays.asList(new ToscaPolicyTypeIdentifier("onap.policies.monitoring.tcagen2", "1.0.0"),
                        new ToscaPolicyTypeIdentifier("onap.policies.controlloop.operational.common.Drools", "1.0.0")));
        pdpSubgroup3.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap.testos", "2.0.0"),
                new ToscaPolicyIdentifier("OPERATIONAL_vLoadBalancerMS_v1_0_Drools_1_0_0_7xd", "1.0.0")));

        // Should match pdpSubgroup1
        PdpGroup pdpGroup1 = new PdpGroup();
        pdpGroup1.setName("pdpGroup1");
        pdpGroup1.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup1.setPdpSubgroups(Arrays.asList(pdpSubgroup1));

        // Should match pdpSubgroup1, pdpSubgroup2, pdpSubgroup3
        // Should match also for the policy (pdpSubgroup2)
        PdpGroup pdpGroup2 = new PdpGroup();
        pdpGroup2.setName("pdpGroup2");
        pdpGroup2.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup2.setPdpSubgroups(Arrays.asList(pdpSubgroup1, pdpSubgroup2, pdpSubgroup3));

        PdpGroups pdpGroups = new PdpGroups();
        pdpGroups.setGroups(Arrays.asList(pdpGroup1, pdpGroup2));

        pdpGroupsJson = JsonUtils.GSON.toJson(pdpGroups);
    }

    @Test
    public void testUpdatePdpGroupOfPolicyModels() throws IOException {
        JSONAssert.assertEquals(ResourceFileUtils.getResourceAsString("clds/policy-merger.json"), PoliciesPdpMerger
                .mergePoliciesAndPdpGroupStates(
                        ResourceFileUtils.getResourceAsString("http-cache/example/policy/api/v1/policies/.file"),
                        pdpGroupsJson),true);
    }
}
