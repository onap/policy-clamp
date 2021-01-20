/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.transaction.Transactional;
import org.junit.Test;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test class validates the PdpGroupAnalyzer class.
 */
public class PdpGroupAnalyzerTest {

    private PolicyModel createPolicyModel(String policyType, String policyModelTosca, String version,
                                          String policyAcronym, String policyVariant, String createdBy) {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setCreatedBy(createdBy);
        policyModel.setPolicyAcronym(policyAcronym);
        policyModel.setPolicyModelTosca(policyModelTosca);
        policyModel.setPolicyModelType(policyType);
        policyModel.setUpdatedBy(createdBy);
        policyModel.setVersion(version);
        return policyModel;
    }

    /**
     * This tests the pdpgroup GSON encode/decode and saving.
     */
    @Test
    @Transactional
    public void testUpdatePdpGroup() {
        // Create policyModel
        PolicyModel policyModel = new PolicyModel();
        policyModel.setCreatedBy("user");
        policyModel.setPolicyAcronym("TEST");
        policyModel.setPolicyModelTosca("yaml");
        policyModel.setPolicyModelType("org.onap.test");
        policyModel.setUpdatedBy("user");
        policyModel.setVersion("1.0.0");

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


        PdpSubGroup pdpSubgroup1 = new PdpSubGroup();
        pdpSubgroup1.setPdpType("subGroup1");
        pdpSubgroup1.setSupportedPolicyTypes(
                Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.*", "1.0.0")));

        PdpSubGroup pdpSubgroup2 = new PdpSubGroup();
        pdpSubgroup2.setPdpType("subGroup2");
        pdpSubgroup2.setSupportedPolicyTypes(Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.test", "1.0.0")));

        PdpSubGroup pdpSubgroup3 = new PdpSubGroup();
        pdpSubgroup3.setPdpType("subGroup3");
        pdpSubgroup3.setSupportedPolicyTypes(Arrays.asList(new ToscaPolicyTypeIdentifier("org.onap.test*", "1.0.0")));


        PdpGroup pdpGroup1 = new PdpGroup();
        pdpGroup1.setName("pdpGroup1");
        pdpGroup1.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup1.setPdpSubgroups(Arrays.asList(pdpSubgroup1, pdpSubgroupBad));

        PdpGroup pdpGroup2 = new PdpGroup();
        pdpGroup2.setName("pdpGroup2");
        pdpGroup2.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup2.setPdpSubgroups(Arrays.asList(pdpSubgroup1, pdpSubgroup2, pdpSubgroup3, pdpSubgroupBad));

        PdpGroup pdpGroup3 = new PdpGroup();
        pdpGroup3.setName("pdpGroup3");
        pdpGroup3.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup3.setPdpSubgroups(Arrays.asList(pdpSubgroupBad));

        PdpGroup pdpGroup4 = new PdpGroup();
        pdpGroup4.setName("pdpGroup4");
        pdpGroup4.setPdpGroupState(PdpState.PASSIVE);
        pdpGroup4.setPdpSubgroups(Arrays.asList(pdpSubgroup1));

        PdpGroups pdpGroups = new PdpGroups();
        pdpGroups.setGroups(Arrays.asList(pdpGroup1, pdpGroup2, pdpGroup3, pdpGroup4));
        PdpGroupsAnalyzer.updatePdpGroup(Arrays.asList(policyModel), pdpGroups);

        assertThat(policyModel.getPolicyPdpGroup().toString()).isEqualTo(
                "{\"supportedPdpGroups\":[{\"pdpGroup1\":[\"subGroup1\"]},{\"pdpGroup2\":[\"subGroup1\",\"subGroup2\",\"subGroup3\"]}]}");
    }
}
