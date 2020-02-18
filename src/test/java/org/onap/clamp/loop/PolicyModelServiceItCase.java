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

package org.onap.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.loop.template.PolicyModelId;
import org.onap.clamp.loop.template.PolicyModelsRepository;
import org.onap.clamp.loop.template.PolicyModelsService;
import org.onap.clamp.policy.pdpgroup.PdpGroup;
import org.onap.clamp.policy.pdpgroup.PdpSubgroup;
import org.onap.clamp.policy.pdpgroup.PolicyModelKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.JsonObject;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class PolicyModelServiceItCase {

    @Autowired
    PolicyModelsService policyModelsService;

    @Autowired
    PolicyModelsRepository policyModelsRepository;

    private static final String POLICY_MODEL_TYPE_1 = "org.onap.testos";
    private static final String POLICY_MODEL_TYPE_1_VERSION_1 = "1.0.0";

    private static final String POLICY_MODEL_TYPE_2 = "org.onap.testos2";
    private static final String POLICY_MODEL_TYPE_3 = "org.onap.testos3";
    private static final String POLICY_MODEL_TYPE_2_VERSION_1 = "1.0.0";
    private static final String POLICY_MODEL_TYPE_3_VERSION_1 = "1.0.0";
    private static final String POLICY_MODEL_TYPE_2_VERSION_2 = "2.0.0";

    private PolicyModel getPolicyModel(String policyType, String policyModelTosca, String version, String policyAcronym,
            String policyVariant, String createdBy) {
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
     * This test the create policy Model.
     */
    @Test
    @Transactional
    public void shouldCreatePolicyModel() {
        // given
        PolicyModel policyModel = getPolicyModel(POLICY_MODEL_TYPE_1, "yaml", POLICY_MODEL_TYPE_1_VERSION_1, "TEST",
                "VARIANT", "user");

        // when
        PolicyModel actualPolicyModel = policyModelsService.saveOrUpdatePolicyModel(policyModel);

        // then
        assertThat(actualPolicyModel).isNotNull();
        assertThat(actualPolicyModel).isEqualTo(policyModelsRepository
                .findById(new PolicyModelId(actualPolicyModel.getPolicyModelType(), actualPolicyModel.getVersion()))
                .get());
        assertThat(actualPolicyModel.getPolicyModelType()).isEqualTo(policyModel.getPolicyModelType());
        assertThat(actualPolicyModel.getCreatedBy()).isEqualTo("Not found");
        assertThat(actualPolicyModel.getCreatedDate()).isNotNull();
        assertThat(actualPolicyModel.getPolicyAcronym()).isEqualTo(policyModel.getPolicyAcronym());
        assertThat(actualPolicyModel.getPolicyModelTosca()).isEqualTo(policyModel.getPolicyModelTosca());
        assertThat(actualPolicyModel.getUpdatedBy()).isEqualTo("Not found");
        assertThat(actualPolicyModel.getUpdatedDate()).isNotNull();
        assertThat(actualPolicyModel.getVersion()).isEqualTo(policyModel.getVersion());

        assertThat(policyModelsService.getPolicyModel(POLICY_MODEL_TYPE_1, POLICY_MODEL_TYPE_1_VERSION_1))
                .isEqualToIgnoringGivenFields(policyModel, "createdDate", "updatedDate", "createdBy", "updatedBy");
    }

    @Test
    @Transactional
    public void shouldReturnAllPolicyModelTypes() {
        // given
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_1, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_2, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);
        List<String> policyModelTypesList = policyModelsService.getAllPolicyModelTypes();

        assertThat(policyModelTypesList).contains(policyModel1.getPolicyModelType(), policyModel2.getPolicyModelType());
    }

    @Test
    @Transactional
    public void shouldReturnAllPolicyModels() {
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_1, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_2, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);

        assertThat(policyModelsService.getAllPolicyModels()).contains(policyModel1, policyModel2);
    }

    @Test
    @Transactional
    public void shouldReturnAllModelsByType() {
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_1, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_2, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);

        assertThat(policyModelsService.getAllPolicyModelsByType(POLICY_MODEL_TYPE_2)).contains(policyModel1,
                policyModel2);
    }

    @Test
    @Transactional
    public void shouldReturnSortedSet() {
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_1, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_2, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);
        PolicyModel policyModel3 = getPolicyModel(POLICY_MODEL_TYPE_3, "yaml", POLICY_MODEL_TYPE_3_VERSION_1, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel3);

        SortedSet<PolicyModel> sortedSet = new TreeSet<>();
        policyModelsService.getAllPolicyModels().forEach(sortedSet::add);
        List<PolicyModel> listToCheck = sortedSet.stream().filter(
            policy -> policy.equals(policyModel3) || policy.equals(policyModel2) || policy.equals(policyModel1))
                .collect(Collectors.toList());
        assertThat(listToCheck.get(0)).isEqualByComparingTo(policyModel2);
        assertThat(listToCheck.get(1)).isEqualByComparingTo(policyModel1);
        assertThat(listToCheck.get(2)).isEqualByComparingTo(policyModel3);
    }

    @Test
    @Transactional
    public void shouldAddPdpGroupInfo() {
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_1, "yaml", POLICY_MODEL_TYPE_1_VERSION_1, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml", POLICY_MODEL_TYPE_2_VERSION_2, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);
        PolicyModel policyModel3 = getPolicyModel(POLICY_MODEL_TYPE_3, "yaml", POLICY_MODEL_TYPE_3_VERSION_1, "TEST",
                "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel3);


        PolicyModelKey type1 = new PolicyModelKey("org.onap.testos", "1.0.0");
        PolicyModelKey type2 = new PolicyModelKey("org.onap.testos2", "2.0.0");

        PdpSubgroup pdpSubgroup1 = new PdpSubgroup();
        pdpSubgroup1.setPdpType("subGroup1");
        List<PolicyModelKey> pdpTypeList = new LinkedList<PolicyModelKey>();
        pdpTypeList.add(type1);
        pdpTypeList.add(type2);
        pdpSubgroup1.setSupportedPolicyTypes(pdpTypeList);

        PolicyModelKey type3 = new PolicyModelKey("org.onap.testos3", "2.0.0");
        PdpSubgroup pdpSubgroup2 = new PdpSubgroup();
        pdpSubgroup2.setPdpType("subGroup2");
        List<PolicyModelKey> pdpTypeList2 = new LinkedList<PolicyModelKey>();
        pdpTypeList2.add(type2);
        pdpTypeList2.add(type3);
        pdpSubgroup2.setSupportedPolicyTypes(pdpTypeList2);

        List<PdpSubgroup> pdpSubgroupList = new LinkedList<PdpSubgroup>();
        pdpSubgroupList.add(pdpSubgroup1);

        PdpGroup pdpGroup1 = new PdpGroup();
        pdpGroup1.setName("pdpGroup1");
        pdpGroup1.setPdpGroupState("ACTIVE");
        pdpGroup1.setPdpSubgroups(pdpSubgroupList);

        List<PdpSubgroup> pdpSubgroupList2 = new LinkedList<PdpSubgroup>();
        pdpSubgroupList2.add(pdpSubgroup1);
        pdpSubgroupList2.add(pdpSubgroup2);
        PdpGroup pdpGroup2 = new PdpGroup();
        pdpGroup2.setName("pdpGroup2");
        pdpGroup2.setPdpGroupState("ACTIVE");
        pdpGroup2.setPdpSubgroups(pdpSubgroupList2);

        List<PdpGroup> pdpGroupList = new LinkedList<PdpGroup>();
        pdpGroupList.add(pdpGroup1);
        pdpGroupList.add(pdpGroup2);
        policyModelsService.updatePdpGroupInfo(pdpGroupList);

        JsonObject res1 = policyModelsService.getPolicyModel("org.onap.testos", "1.0.0").getPolicyPdpGroup();
        String expectedRes1 = "{\"supportedPdpGroups\":[{\"pdpGroup1\":[\"subGroup1\"]},{\"pdpGroup2\":[\"subGroup1\"]}]}";
        JsonObject expectedJson1 = JsonUtils.GSON.fromJson(expectedRes1, JsonObject.class);
        assertThat(res1).isEqualTo(expectedJson1);

        JsonObject res2 = policyModelsService.getPolicyModel("org.onap.testos2", "2.0.0").getPolicyPdpGroup();
        String expectedRes2 = "{\"supportedPdpGroups\":[{\"pdpGroup1\":[\"subGroup1\"]},{\"pdpGroup2\":[\"subGroup1\",\"subGroup2\"]}]}";
        JsonObject expectedJson2 = JsonUtils.GSON.fromJson(expectedRes2, JsonObject.class);
        assertThat(res2).isEqualTo(expectedJson2);

        JsonObject res3 = policyModelsService.getPolicyModel("org.onap.testos3", "1.0.0").getPolicyPdpGroup();
        assertThat(res3).isNull();
    }
}
