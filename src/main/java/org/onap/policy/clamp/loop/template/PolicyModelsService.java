/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.loop.template;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.onap.policy.clamp.clds.tosca.ToscaSchemaConstants;
import org.onap.policy.clamp.clds.tosca.ToscaYamlToJsonConvertor;
import org.onap.policy.clamp.policy.pdpgroup.PdpGroupsAnalyzer;
import org.onap.policy.clamp.util.SemanticVersioning;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class contains the methods to access the policyModel object in db.
 */
@Service
public class PolicyModelsService {
    private final PolicyModelsRepository policyModelsRepository;
    private ToscaYamlToJsonConvertor toscaYamlToJsonConvertor;

    @Autowired
    public PolicyModelsService(PolicyModelsRepository policyModelrepo,
                               ToscaYamlToJsonConvertor convertor) {
        policyModelsRepository = policyModelrepo;
        toscaYamlToJsonConvertor = convertor;
    }

    /**
     * Save or Update Policy Model.
     *
     * @param policyModel The policyModel
     * @return The Policy Model
     */
    public PolicyModel saveOrUpdatePolicyModel(PolicyModel policyModel) {
        return policyModelsRepository.saveAndFlush(policyModel);
    }

    /**
     * Verify whether Policy Model exist by ID.
     *
     * @param policyModelId The policyModel Id
     * @return The flag indicates whether Policy Model exist
     */
    public boolean existsById(PolicyModelId policyModelId) {
        return policyModelsRepository.existsById(policyModelId);
    }

    /**
     * Creates or updates the Tosca Policy Model.
     *
     * @param policyModelTosca The Policymodel object
     * @return The Policy Model created
     */
    public PolicyModel createNewPolicyModelFromTosca(String policyModelTosca) {
        JsonObject jsonObject = toscaYamlToJsonConvertor.validateAndConvertToJson(policyModelTosca);
        String policyModelTypeFromTosca = toscaYamlToJsonConvertor.getValueFromMetadata(jsonObject,
                ToscaSchemaConstants.METADATA_POLICY_MODEL_TYPE);
        Iterable<PolicyModel> models = getAllPolicyModelsByType(policyModelTypeFromTosca);
        Collections.sort((List<PolicyModel>) models);
        PolicyModel newPolicyModel = new PolicyModel(policyModelTypeFromTosca, policyModelTosca,
                SemanticVersioning.incrementMajorVersion(((ArrayList) models).isEmpty() ? null
                        : ((ArrayList<PolicyModel>) models).get(0).getVersion()),
                toscaYamlToJsonConvertor.getValueFromMetadata(jsonObject,
                        ToscaSchemaConstants.METADATA_ACRONYM));
        return saveOrUpdatePolicyModel(newPolicyModel);
    }

    /**
     * Update an existing Tosca Policy Model.
     *
     * @param policyModelType    The policy Model type in Tosca yaml
     * @param policyModelVersion The policy Version to update
     * @param policyModelTosca   The Policy Model tosca
     * @return The Policy Model updated
     */
    public PolicyModel updatePolicyModelTosca(String policyModelType, String policyModelVersion,
                                              String policyModelTosca) {
        JsonObject jsonObject = toscaYamlToJsonConvertor.validateAndConvertToJson(policyModelTosca);
        PolicyModel thePolicyModel = getPolicyModel(policyModelType, policyModelVersion);
        thePolicyModel.setPolicyAcronym(toscaYamlToJsonConvertor.getValueFromMetadata(jsonObject,
                ToscaSchemaConstants.METADATA_ACRONYM));
        thePolicyModel.setPolicyModelTosca(policyModelTosca);
        return saveOrUpdatePolicyModel(thePolicyModel);
    }

    public List<String> getAllPolicyModelTypes() {
        return policyModelsRepository.getAllPolicyModelType();
    }

    public Iterable<PolicyModel> getAllPolicyModels() {
        return policyModelsRepository.findAll();
    }

    public PolicyModel getPolicyModel(String type, String version) {
        return policyModelsRepository.findById(new PolicyModelId(type, version)).orElse(null);
    }

    public Iterable<PolicyModel> getAllPolicyModelsByType(String type) {
        return policyModelsRepository.findByPolicyModelType(type);
    }

    /**
     * Retrieves the Tosca model Yaml string.
     *
     * @param type    The Policy Model Type
     * @param version The policy model version
     * @return The Tosca model Yaml string
     */
    public String getPolicyModelTosca(String type, String version) {
        return policyModelsRepository.findById(new PolicyModelId(type, version))
                .orElse(new PolicyModel()).getPolicyModelTosca();
    }

    /**
     * This method creates an PolicyModel in Db if it does not exist.
     *
     * @param policyModel The policyModel to save
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PolicyModel savePolicyModelInNewTransaction(PolicyModel policyModel) {
        return policyModelsRepository.saveAndFlush(policyModel);
    }

    /**
     * Update the Pdp Group info in Policy Model DB.
     *
     * @param pdpGroups The list of Pdp Group info received from Policy Engine
     */
    public void updatePdpGroupInfo(PdpGroups pdpGroups) {
        List<PolicyModel> policyModelsList = policyModelsRepository.findAll();
        PdpGroupsAnalyzer.updatePdpGroupOfPolicyModels(policyModelsList, pdpGroups);
        this.policyModelsRepository.saveAll(policyModelsList);
    }
}
