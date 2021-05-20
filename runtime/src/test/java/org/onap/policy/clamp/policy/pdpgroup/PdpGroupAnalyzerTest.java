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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This test class validates the PdpGroupAnalyzer class.
 */
public class PdpGroupAnalyzerTest {

    private static PdpGroups pdpGroups;
    private static PdpGroup pdpGroup1;
    private static PdpGroup pdpGroup2;
    private static PdpGroup pdpGroup3;
    private static PdpGroup pdpGroup4;

    private static PdpSubGroup pdpSubgroupBad;
    private static PdpSubGroup pdpSubgroup1;
    private static PdpSubGroup pdpSubgroup2;
    private static PdpSubGroup pdpSubgroup3;

    /**
     * This method preloads the pdpGroups for the tests.
     */
    @BeforeClass
    public static void setupPdpGroup() {
        // Create Pdp Groups
        // Those that do not work first
        pdpSubgroupBad = new PdpSubGroup();
        pdpSubgroupBad.setPdpType("subGroupBad");
        pdpSubgroupBad.setSupportedPolicyTypes(Arrays.asList(new ToscaConceptIdentifier("org.onap.test", "2.0.0"),
                new ToscaConceptIdentifier("org.onap.test.*", "1.0.0"),
                new ToscaConceptIdentifier("org.onip.testos", "1.0.0"),
                new ToscaConceptIdentifier("org.onap.testos3", "2.0.0"),
                new ToscaConceptIdentifier("org.onap.tes", "1.0.0"),
                new ToscaConceptIdentifier("org.onap", "1.0.0")
        ));
        pdpSubgroupBad.setPolicies(Arrays.asList(new ToscaConceptIdentifier("org.onap", "1.0.0"),
                new ToscaConceptIdentifier("org.onap.testos", "2.0.0"),
                new ToscaConceptIdentifier("org.onap.testos", "1.0.1"),
                new ToscaConceptIdentifier("org.onap.testos.new", "1.0.0"),
                new ToscaConceptIdentifier("org.onap.", "1.0.0")));
        pdpSubgroupBad.setPdpInstances(Collections.emptyList());

        pdpSubgroup1 = new PdpSubGroup();
        pdpSubgroup1.setPdpType("subGroup1");
        pdpSubgroup1.setSupportedPolicyTypes(
                Arrays.asList(new ToscaConceptIdentifier("org.onap.*", "1.0.0")));
        pdpSubgroup1.setPdpInstances(Collections.emptyList());

        pdpSubgroup2 = new PdpSubGroup();
        pdpSubgroup2.setPdpType("subGroup2");
        pdpSubgroup2.setSupportedPolicyTypes(Arrays.asList(new ToscaConceptIdentifier("org.onap.test", "1.0.0")));
        pdpSubgroup2.setPolicies(Arrays.asList(new ToscaConceptIdentifier("org.onap.testos", "1.0.0")));
        pdpSubgroup2.setPdpInstances(Collections.emptyList());

        pdpSubgroup3 = new PdpSubGroup();
        pdpSubgroup3.setPdpType("subGroup3");
        pdpSubgroup3.setSupportedPolicyTypes(Arrays.asList(new ToscaConceptIdentifier("org.onap.test*", "1.0.0")));
        pdpSubgroup3.setPolicies(Arrays.asList(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")));
        pdpSubgroup3.setPdpInstances(Collections.emptyList());

        // Should match pdpSubgroup1
        pdpGroup1 = new PdpGroup();
        pdpGroup1.setName("pdpGroup1");
        pdpGroup1.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup1.setPdpSubgroups(Arrays.asList(pdpSubgroup1, pdpSubgroupBad));

        // Should match pdpSubgroup1, pdpSubgroup2, pdpSubgroup3
        // Should match also for the policy (pdpSubgroup2)
        pdpGroup2 = new PdpGroup();
        pdpGroup2.setName("pdpGroup2");
        pdpGroup2.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup2.setPdpSubgroups(Arrays.asList(pdpSubgroup1, pdpSubgroup2, pdpSubgroup3, pdpSubgroupBad));

        /// Should not match
        pdpGroup3 = new PdpGroup();
        pdpGroup3.setName("pdpGroup3");
        pdpGroup3.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup3.setPdpSubgroups(Arrays.asList(pdpSubgroupBad));

        // Should not match
        pdpGroup4 = new PdpGroup();
        pdpGroup4.setName("pdpGroup4");
        pdpGroup4.setPdpGroupState(PdpState.TERMINATED);
        pdpGroup4.setPdpSubgroups(Arrays.asList(pdpSubgroup1));

        pdpGroups = new PdpGroups();
        pdpGroups.setGroups(Arrays.asList(pdpGroup1, pdpGroup2, pdpGroup3, pdpGroup4));
    }

    @Test
    public void testStructuresConstruction() {
        PdpGroupsAnalyzer pdpGroupsAnalyzer = new PdpGroupsAnalyzer(pdpGroups);
        assertThat(pdpGroupsAnalyzer).isNotNull();
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()).hasSize(6);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0"))).hasSize(3);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")).get("pdpGroup1").getPdpSubgroups().size())
                .isEqualTo(1);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")).get("pdpGroup1").getPdpSubgroups())
                .contains(pdpSubgroupBad);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")).get("pdpGroup2").getPdpSubgroups().size())
                .isEqualTo(2);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")).get("pdpGroup2").getPdpSubgroups())
                .contains(pdpSubgroup3);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")).get("pdpGroup2").getPdpSubgroups())
                .contains(pdpSubgroupBad);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")).get("pdpGroup3").getPdpSubgroups().size())
                .isEqualTo(1);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "2.0.0")).get("pdpGroup3").getPdpSubgroups())
                .contains(pdpSubgroupBad);

        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.0"))).hasSize(1);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.0")).get("pdpGroup2").getPdpSubgroups())
                .hasSize(1);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.0")).get("pdpGroup2").getPdpSubgroups())
                .contains(pdpSubgroup2);

        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.1"))).hasSize(3);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.1")).get("pdpGroup1").getPdpSubgroups())
                .hasSize(1);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.1")).get("pdpGroup1").getPdpSubgroups())
                .contains(pdpSubgroupBad);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.1")).get("pdpGroup2").getPdpSubgroups())
                .hasSize(1);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.1")).get("pdpGroup2").getPdpSubgroups())
                .contains(pdpSubgroupBad);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.1")).get("pdpGroup3").getPdpSubgroups())
                .hasSize(1);
        assertThat(pdpGroupsAnalyzer.getPdpGroupsDeploymentPerPolicy()
                .get(new ToscaConceptIdentifier("org.onap.testos", "1.0.1")).get("pdpGroup3").getPdpSubgroups())
                .contains(pdpSubgroupBad);
    }

    @Test
    public void testGetSupportedPdpGroupsForPolicy() throws IOException {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setCreatedBy("user");
        policyModel.setPolicyAcronym("TEST");
        policyModel.setPolicyModelTosca("yaml");
        policyModel.setPolicyModelType("org.onap.test");
        policyModel.setUpdatedBy("user");
        policyModel.setVersion("1.0.0");

        PdpGroupsAnalyzer pdpGroupsAnalyzer = new PdpGroupsAnalyzer(pdpGroups);
        assertThat(pdpGroupsAnalyzer).isNotNull();

        assertThat(
                JsonUtils.GSON.toJson(pdpGroupsAnalyzer.getPdpGroupsForPolicy("org.onap.testos", "2.0.0")))
                .isEqualTo(ResourceFileUtils.getResourceAsString("example/policy/pdp-deployment-testos-2_0_0.json"));

        assertThat(
                JsonUtils.GSON.toJson(pdpGroupsAnalyzer.getPdpGroupsForPolicy("org.onap.testos", "1.0.0")))
                .isEqualTo(ResourceFileUtils.getResourceAsString("example/policy/pdp-deployment-testos-1_0_0.json"));

        assertThat(
                pdpGroupsAnalyzer.getPdpGroupsForPolicy("org.onap.donotexist", "1.0.0"))
                .isNull();
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

        assertThat(policyModel.getPolicyPdpGroup()).hasToString(
                "{\"supportedPdpGroups\":[{\"pdpGroup1\":[\"subGroup1\"]},"
                        + "{\"pdpGroup2\":[\"subGroup1\",\"subGroup2\",\"subGroup3\"]}]}");
    }
}
