/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.loop.template;

import com.google.gson.JsonObject;
import java.util.List;
import org.onap.clamp.clds.tosca.ToscaSchemaConstants;
import org.onap.clamp.clds.tosca.ToscaYamlToJsonConvertor;
import org.onap.clamp.util.SemanticVersioning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public PolicyModel saveOrUpdatePolicyModel(PolicyModel policyModel) {
        return policyModelsRepository.save(policyModel);
    }

    /**
     * Creates or updates the Tosca Policy Model.
     *
     * @param policyModelType
     *        The policyModeltype in Tosca yaml
     * @param policyModel
     *        The Policymodel object
     * @return The Policy Model
     */
    public PolicyModel saveOrUpdateByPolicyModelType(String policyModelType,
        String policyModelTosca) {
        JsonObject jsonObject = toscaYamlToJsonConvertor.validateAndConvertToJson(policyModelTosca);

        String policyModelTypeName = toscaYamlToJsonConvertor.getValueFromMetadata(jsonObject,
            ToscaSchemaConstants.METADATA_POLICY_MODEL_TYPE);
        String acronym = toscaYamlToJsonConvertor.getValueFromMetadata(jsonObject,
            ToscaSchemaConstants.METADATA_ACRONYM);
        PolicyModel model = getPolicyModelByType(
            policyModelTypeName != null ? policyModelTypeName : policyModelType);

        if (model == null) {
            model = new PolicyModel(policyModelTypeName, policyModelTosca,
                SemanticVersioning.incrementMajorVersion(null), acronym);
        } else {
            model.setVersion(SemanticVersioning
                .incrementMajorVersion(model.getVersion() != null ? model.getVersion() : null));
            model.setPolicyModelType(policyModelTypeName);
            model.setPolicyAcronym(acronym);
        }
        return saveOrUpdatePolicyModel(model);
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

    public PolicyModel getPolicyModelByType(String type) {
        List<PolicyModel> list = policyModelsRepository.findByPolicyModelType(type);
        return list.stream().sorted().findFirst().orElse(null);
    }

    /**
     * Retrieves the Tosca model Yaml string.
     *
     * @param type The PolicyModelType
     * @return The Tosca model Yaml string
     */
    public String getPolicyModelTosca(String type) {
        PolicyModel policyModel = getPolicyModelByType(type);
        return policyModel != null ? policyModel.getPolicyModelTosca() : null;
    }
}
