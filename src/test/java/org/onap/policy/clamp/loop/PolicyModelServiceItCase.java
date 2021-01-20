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

package org.onap.policy.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.policy.clamp.clds.Application;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.loop.template.PolicyModelId;
import org.onap.policy.clamp.loop.template.PolicyModelsRepository;
import org.onap.policy.clamp.loop.template.PolicyModelsService;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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

    private PolicyModel getPolicyModel(String policyType, String policyModelTosca, String version,
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
     * This test the create policy Model.
     */
    @Test
    @Transactional
    public void shouldCreatePolicyModel() {
        // given
        PolicyModel policyModel = getPolicyModel(POLICY_MODEL_TYPE_1, "yaml",
            POLICY_MODEL_TYPE_1_VERSION_1, "TEST", "VARIANT", "user");

        // when
        PolicyModel actualPolicyModel = policyModelsService.saveOrUpdatePolicyModel(policyModel);

        // then
        assertThat(actualPolicyModel).isNotNull();
        assertThat(actualPolicyModel).isEqualTo(policyModelsRepository
            .findById(new PolicyModelId(actualPolicyModel.getPolicyModelType(),
                actualPolicyModel.getVersion()))
            .get());
        assertThat(actualPolicyModel.getPolicyModelType())
            .isEqualTo(policyModel.getPolicyModelType());
        Assertions.assertThat(actualPolicyModel.getCreatedBy()).isEqualTo("Not found");
        Assertions.assertThat(actualPolicyModel.getCreatedDate()).isNotNull();
        assertThat(actualPolicyModel.getPolicyAcronym()).isEqualTo(policyModel.getPolicyAcronym());
        assertThat(actualPolicyModel.getPolicyModelTosca())
            .isEqualTo(policyModel.getPolicyModelTosca());
        Assertions.assertThat(actualPolicyModel.getUpdatedBy()).isEqualTo("Not found");
        Assertions.assertThat(actualPolicyModel.getUpdatedDate()).isNotNull();
        assertThat(actualPolicyModel.getVersion()).isEqualTo(policyModel.getVersion());

        assertThat(
            policyModelsService.getPolicyModel(POLICY_MODEL_TYPE_1, POLICY_MODEL_TYPE_1_VERSION_1))
                .isEqualToIgnoringGivenFields(policyModel, "createdDate", "updatedDate",
                    "createdBy", "updatedBy");
    }

    /**
     * This tests a create Policy Model from Tosca.
     *
     * @throws IOException In case of failure
     */
    @Test
    @Transactional
    public void shouldCreatePolicyModelFromTosca() throws IOException {
        String toscaModelYaml =
            ResourceFileUtils.getResourceAsString("tosca/tosca_with_metadata.yaml");
        PolicyModel policyModel = policyModelsService.createNewPolicyModelFromTosca(toscaModelYaml);

        assertThat(policyModelsService.getAllPolicyModels()).contains(policyModel);

        assertThat(policyModelsService.getPolicyModelTosca(policyModel.getPolicyModelType(),
            policyModel.getVersion())).contains(toscaModelYaml);
    }

    /**
     * This tests a update Policy Model.
     *
     * @throws IOException In case of failure
     */
    @Test
    @Transactional
    public void shouldUpdatePolicyModel() throws IOException {
        String toscaModelYaml =
            ResourceFileUtils.getResourceAsString("tosca/tosca_with_metadata.yaml");
        PolicyModel policyModel = policyModelsService.createNewPolicyModelFromTosca(toscaModelYaml);
        String newToscaModelYaml =
            ResourceFileUtils.getResourceAsString("tosca/tosca_metadata_clamp_possible_values.yaml");

        PolicyModel updatedPolicyModel = policyModelsService.updatePolicyModelTosca(
            policyModel.getPolicyModelType(), policyModel.getVersion(), newToscaModelYaml);

        assertThat(updatedPolicyModel.getPolicyModelTosca()).isEqualTo(newToscaModelYaml);

    }

    /**
     * This tests a getAllPolicyModelTypes get.
     */
    @Test
    @Transactional
    public void shouldReturnAllPolicyModelTypes() {
        // given
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_1, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_2, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);
        List<String> policyModelTypesList = policyModelsService.getAllPolicyModelTypes();

        assertThat(policyModelTypesList).contains(policyModel1.getPolicyModelType(),
            policyModel2.getPolicyModelType());
    }

    /**
     * This tests a getAllPolicyModels get.
     */
    @Test
    @Transactional
    public void shouldReturnAllPolicyModels() {
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_1, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_2, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);

        assertThat(policyModelsService.getAllPolicyModels()).contains(policyModel1, policyModel2);
    }

    /**
     * This tests a getAllPolicyModelsByType get.
     */
    @Test
    @Transactional
    public void shouldReturnAllModelsByType() {
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_1, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_2, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);

        assertThat(policyModelsService.getAllPolicyModelsByType(POLICY_MODEL_TYPE_2))
            .contains(policyModel1, policyModel2);
    }

    /**
     * This tests the sorting of policyModel.
     */
    @Test
    @Transactional
    public void shouldReturnSortedSet() {
        PolicyModel policyModel1 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_1, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
            POLICY_MODEL_TYPE_2_VERSION_2, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);
        PolicyModel policyModel3 = getPolicyModel(POLICY_MODEL_TYPE_3, "yaml",
            POLICY_MODEL_TYPE_3_VERSION_1, "TEST", "VARIANT", "user");
        policyModelsService.saveOrUpdatePolicyModel(policyModel3);

        SortedSet<PolicyModel> sortedSet = new TreeSet<>();
        policyModelsService.getAllPolicyModels().forEach(sortedSet::add);
        List<PolicyModel> listToCheck =
            sortedSet
                .stream().filter(policy -> policy.equals(policyModel3)
                    || policy.equals(policyModel2) || policy.equals(policyModel1))
                .collect(Collectors.toList());
        assertThat(listToCheck.get(0)).isEqualByComparingTo(policyModel2);
        assertThat(listToCheck.get(1)).isEqualByComparingTo(policyModel1);
        assertThat(listToCheck.get(2)).isEqualByComparingTo(policyModel3);
    }

    /**
     * This tests the pdpgroup GSON encode/decode and saving.
     */
    @Test
    @Transactional
    public void shouldAddPdpGroupInfo() {
        policyModelsService.saveOrUpdatePolicyModel(getPolicyModel(POLICY_MODEL_TYPE_1, "yaml",
                POLICY_MODEL_TYPE_1_VERSION_1, "TEST", "VARIANT", "user"));
        policyModelsService.saveOrUpdatePolicyModel(getPolicyModel(POLICY_MODEL_TYPE_2, "yaml",
                POLICY_MODEL_TYPE_2_VERSION_2, "TEST", "VARIANT", "user"));
        policyModelsService.saveOrUpdatePolicyModel(getPolicyModel(POLICY_MODEL_TYPE_3, "yaml",
                POLICY_MODEL_TYPE_3_VERSION_1, "TEST", "VARIANT", "user"));

        ToscaPolicyTypeIdentifier type1 = new ToscaPolicyTypeIdentifier("org.onap.testos", "1.0.0");
        ToscaPolicyTypeIdentifier type2 = new ToscaPolicyTypeIdentifier("org.onap.testos2", "2.0.0");

        PdpSubGroup pdpSubgroup1 = new PdpSubGroup();
        pdpSubgroup1.setPdpType("subGroup1");
        List<ToscaPolicyTypeIdentifier> pdpTypeList = new LinkedList<>();
        pdpTypeList.add(type1);
        pdpTypeList.add(type2);
        pdpSubgroup1.setSupportedPolicyTypes(pdpTypeList);

        ToscaPolicyTypeIdentifier type3 = new ToscaPolicyTypeIdentifier("org.onap.testos3", "2.0.0");
        PdpSubGroup pdpSubgroup2 = new PdpSubGroup();
        pdpSubgroup2.setPdpType("subGroup2");
        List<ToscaPolicyTypeIdentifier> pdpTypeList2 = new LinkedList<>();
        pdpTypeList2.add(type2);
        pdpTypeList2.add(type3);
        pdpSubgroup2.setSupportedPolicyTypes(pdpTypeList2);

        List<PdpSubGroup> pdpSubgroupList = new LinkedList<>();
        pdpSubgroupList.add(pdpSubgroup1);

        PdpGroup pdpGroup1 = new PdpGroup();
        pdpGroup1.setName("pdpGroup1");
        pdpGroup1.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup1.setPdpSubgroups(pdpSubgroupList);

        List<PdpSubGroup> pdpSubgroupList2 = new LinkedList<>();
        pdpSubgroupList2.add(pdpSubgroup1);
        pdpSubgroupList2.add(pdpSubgroup2);
        PdpGroup pdpGroup2 = new PdpGroup();
        pdpGroup2.setName("pdpGroup2");
        pdpGroup2.setPdpGroupState(PdpState.ACTIVE);
        pdpGroup2.setPdpSubgroups(pdpSubgroupList2);

        List<PdpGroup> pdpGroupsList = new LinkedList<>();
        pdpGroupsList.add(pdpGroup1);
        pdpGroupsList.add(pdpGroup2);

        PdpGroups pdpGroups = new PdpGroups();
        pdpGroups.setGroups(pdpGroupsList);
        policyModelsService.updatePdpGroupInfo(pdpGroups);

        JsonObject res1 =
            policyModelsService.getPolicyModel("org.onap.testos", "1.0.0").getPolicyPdpGroup();
        String expectedRes1 =
            "{\"supportedPdpGroups\":[{\"pdpGroup1\":[\"subGroup1\"]},{\"pdpGroup2\":[\"subGroup1\"]}]}";
        JsonObject expectedJson1 = JsonUtils.GSON.fromJson(expectedRes1, JsonObject.class);
        assertThat(res1).isEqualTo(expectedJson1);

        JsonObject res2 =
            policyModelsService.getPolicyModel("org.onap.testos2", "2.0.0").getPolicyPdpGroup();
        String expectedRes2 =
            "{\"supportedPdpGroups\":[{\"pdpGroup1\":[\"subGroup1\"]},{\"pdpGroup2\":[\"subGroup1\",\"subGroup2\"]}]}";
        JsonObject expectedJson2 = JsonUtils.GSON.fromJson(expectedRes2, JsonObject.class);
        assertThat(res2).isEqualTo(expectedJson2);

        JsonObject res3 =
            policyModelsService.getPolicyModel("org.onap.testos3", "1.0.0").getPolicyPdpGroup();
        assertThat(res3).isNull();
    }
}
