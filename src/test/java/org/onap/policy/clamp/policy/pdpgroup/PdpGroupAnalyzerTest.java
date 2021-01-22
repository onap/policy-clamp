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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

/**
 * This test class validates the PdpGroupAnalyzer class.
 */
public class PdpGroupAnalyzerTest {

    private static PdpGroups pdpGroups;

    /**
     * This method preloads the pdpGroups for the tests.
     */
    @BeforeClass
    public static void setupPdpGroup() {
        // Create Pdp Groups
        // Those that do not work first
        PdpSubGroup pdpSubgroupBad = new PdpSubGroup();
        pdpSubgroupBad.setPdpType("subGroupBad");
        pdpSubgroupBad.setSupportedPolicyTypes(Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.test", "2.0.0"),
                new ToscaPolicyTypeIdentifier("org.onap.test.*", "1.0.0"),
                new ToscaPolicyTypeIdentifier("org.onip.testos", "1.0.0"),
                new ToscaPolicyTypeIdentifier("org.onap.testos3", "2.0.0"),
                new ToscaPolicyTypeIdentifier("org.onap.tes", "1.0.0"),
                new ToscaPolicyTypeIdentifier("org.onap", "1.0.0")
        ));
        pdpSubgroupBad.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap", "1.0.0")));
        pdpSubgroupBad.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap.testos", "2.0.0")));
        pdpSubgroupBad.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap.testos", "1.0.1")));
        pdpSubgroupBad.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap.testos.new", "1.0.0")));
        pdpSubgroupBad.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap.*", "1.0.0")));

        PdpSubGroup pdpSubgroup1 = new PdpSubGroup();
        pdpSubgroup1.setPdpType("subGroup1");
        pdpSubgroup1.setSupportedPolicyTypes(
                Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.*", "1.0.0")));

        PdpSubGroup pdpSubgroup2 = new PdpSubGroup();
        pdpSubgroup2.setPdpType("subGroup2");
        pdpSubgroup2.setSupportedPolicyTypes(Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.test", "1.0.0")));
        pdpSubgroup2.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap.testos", "1.0.0")));

        PdpSubGroup pdpSubgroup3 = new PdpSubGroup();
        pdpSubgroup3.setPdpType("subGroup3");
        pdpSubgroup3.setSupportedPolicyTypes(Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.test*", "1.0.0")));
        pdpSubgroup3.setPolicies(Arrays.asList(new ToscaPolicyIdentifier("org.onap.testos", "2.0.0")));

        // Should match pdpSubgroup1
        PdpGroup pdpGroup1 = new PdpGroup();
        pdpGroup1.setName("pdpGroup1");
        pdpGroup1.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup1.setPdpSubgroups(Arrays.asList(pdpSubgroup1, pdpSubgroupBad));

        // Should match pdpSubgroup1, pdpSubgroup2, pdpSubgroup3
        // Should match also for the policy (pdpSubgroup2)
        PdpGroup pdpGroup2 = new PdpGroup();
        pdpGroup2.setName("pdpGroup2");
        pdpGroup2.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup2.setPdpSubgroups(Arrays.asList(pdpSubgroup1, pdpSubgroup2, pdpSubgroup3, pdpSubgroupBad));

        /// Should not match
        PdpGroup pdpGroup3 = new PdpGroup();
        pdpGroup3.setName("pdpGroup3");
        pdpGroup3.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup3.setPdpSubgroups(Arrays.asList(pdpSubgroupBad));

        // Should not match
        PdpGroup pdpGroup4 = new PdpGroup();
        pdpGroup4.setName("pdpGroup4");
        pdpGroup4.setPdpGroupState(PdpState.PASSIVE);
        pdpGroup4.setPdpSubgroups(Arrays.asList(pdpSubgroup1));

        pdpGroups = new PdpGroups();
        pdpGroups.setGroups(Arrays.asList(pdpGroup1, pdpGroup2, pdpGroup3, pdpGroup4));
    }

    @Test
    public void testUpdatePdpGroupOfPolicyModels() {
        // Create policyModel
        PolicyModel policyModel = new PolicyModel();
        policyModel.setCreatedBy("user");
        policyModel.setPolicyAcronym("TEST");
        policyModel.setPolicyModelTosca("yaml");
        policyModel.setPolicyModelType("org.onap.test");
        policyModel.setUpdatedBy("user");
        policyModel.setVersion("1.0.0");
        PdpGroupsAnalyzer.updatePdpGroupOfPolicyModels(Arrays.asList(policyModel), pdpGroups);

        assertThat(policyModel.getPolicyPdpGroup().toString()).isEqualTo(
                "{\"supportedPdpGroups\":[{\"pdpGroup1\":[\"subGroup1\"]},"
                        + "{\"pdpGroup2\":[\"subGroup1\",\"subGroup2\",\"subGroup3\"]}]}");
    }

    @Test
    public void testGetPdpGroupDeploymentsOfOnePolicy() {
        assertThat(
                PdpGroupsAnalyzer.getPdpGroupDeploymentOfOnePolicy(pdpGroups, "org.onap.testos", "1.0.0").toString())
                .isEqualTo("{\"pdpGroupInfo\":{\"pdpGroup\":\"pdpGroup2\",\"pdpSubGroup\":\"subGroup2\"}}");
    }

    @Test
    public void testGetPdpGroupDeploymentsOfOnePolicyNull() {
        assertThat(
                PdpGroupsAnalyzer.getPdpGroupDeploymentOfOnePolicy(pdpGroups, "org.onap.DoNotExist", "1.0.0"))
                .isNull();
    }
}
