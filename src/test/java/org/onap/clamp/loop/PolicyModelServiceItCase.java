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

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.loop.template.PolicyModelId;
import org.onap.clamp.loop.template.PolicyModelsRepository;
import org.onap.clamp.loop.template.PolicyModelsService;
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

    private static final String POLICY_MODEL_TYPE_1 = "org.onap.test";
    private static final String POLICY_MODEL_TYPE_1_VERSION_1 = "1.0.0";

    private static final String POLICY_MODEL_TYPE_2 = "org.onap.test2";
    private static final String POLICY_MODEL_TYPE_2_VERSION_1 = "1.0.0";
    private static final String POLICY_MODEL_TYPE_2_VERSION_2 = "2.0.0";

    private PolicyModel getPolicyModel(String policyType, String policyModelTosca, String version, String policyAcronym,
            String policyVariant, String createdBy) {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setCreatedBy(createdBy);
        policyModel.setPolicyAcronym(policyAcronym);
        policyModel.setPolicyModelTosca(policyModelTosca);
        policyModel.setPolicyModelType(policyType);
        policyModel.setPolicyVariant(policyVariant);
        policyModel.setUpdatedBy(createdBy);
        policyModel.setVersion(version);
        return policyModel;
    }

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
        assertThat(actualPolicyModel.getCreatedBy()).isEqualTo("");
        assertThat(actualPolicyModel.getCreatedDate()).isNotNull();
        assertThat(actualPolicyModel.getPolicyAcronym()).isEqualTo(policyModel.getPolicyAcronym());
        assertThat(actualPolicyModel.getPolicyModelTosca()).isEqualTo(policyModel.getPolicyModelTosca());
        assertThat(actualPolicyModel.getPolicyVariant()).isEqualTo(policyModel.getPolicyVariant());
        assertThat(actualPolicyModel.getUpdatedBy()).isEqualTo("");
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

        assertThat(policyModelTypesList).containsOnly(policyModel1.getPolicyModelType(),
                policyModel2.getPolicyModelType());
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

        assertThat(policyModelsService.getAllPolicyModels()).containsOnly(policyModel1, policyModel2);
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

        assertThat(policyModelsService.getAllPolicyModelsByType(POLICY_MODEL_TYPE_2)).containsOnly(policyModel1,
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

        SortedSet<PolicyModel> sortedSet = new TreeSet<>();
        policyModelsService.getAllPolicyModels().forEach(sortedSet::add);
        assertThat(sortedSet).containsExactly(policyModel2, policyModel1);
    }
}
