/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * Modifications Copyright (C) 2020 Huawei Technologies Co., Ltd.
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

package org.onap.clamp.policy.operational;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.onap.clamp.clds.config.LegacyOperationalPolicyController;
import org.onap.clamp.clds.tosca.update.ToscaConverterWithDictionarySupport;
import org.onap.clamp.dao.model.jsontype.StringJsonUserType;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.policy.Policy;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Entity
@Table(name = "operational_policies")
@TypeDefs({@TypeDef(name = "json", typeClass = StringJsonUserType.class)})
public class OperationalPolicy extends Policy implements Serializable {
    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = 6117076450841538255L;

    @Transient
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(OperationalPolicy.class);

    @Id
    @Expose
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loop_id", nullable = false)
    private Loop loop;

    /**
     * Constructor for serialization.
     */
    public OperationalPolicy() {
    }

    /**
     * The constructor.
     *
     * @param name               The name of the operational policy
     * @param configurationsJson The operational policy property in the format of
     *                           json
     * @param jsonRepresentation The jsonObject defining the json schema
     * @param policyModel        The policy model associated if any, can be null
     * @param loopElementModel   The loop element from which this instance is supposed to be created
     * @param pdpGroup           The Pdp Group info
     * @param pdpSubgroup        The Pdp Subgroup info
     */
    public OperationalPolicy(String name, JsonObject configurationsJson,
                             JsonObject jsonRepresentation, PolicyModel policyModel,
                             LoopElementModel loopElementModel, String pdpGroup, String pdpSubgroup) {
        this.name = name;
        this.setPolicyModel(policyModel);
        this.setConfigurationsJson(configurationsJson);
        this.setPdpGroup(pdpGroup);
        this.setPdpSubgroup(pdpSubgroup);
        this.setLoopElementModel(loopElementModel);
        this.setJsonRepresentation(jsonRepresentation);

    }

    /**
     * Create an operational policy from a loop element model.
     *
     * @param loop             The parent loop
     * @param service          The loop service
     * @param loopElementModel The loop element model
     * @param toscaConverter   The tosca converter that must be used to create the Json representation
     */
    public OperationalPolicy(Loop loop, Service service, LoopElementModel loopElementModel,
                             ToscaConverterWithDictionarySupport toscaConverter) {
        this(Policy.generatePolicyName("OPERATIONAL", service.getName(), service.getVersion(),
                loopElementModel.getPolicyModels().first().getPolicyAcronym() + '_'
                        + loopElementModel.getPolicyModels().first().getVersion(),
                RandomStringUtils.randomAlphanumeric(3)), new JsonObject(),
                new JsonObject(), loopElementModel.getPolicyModels().first(), loopElementModel, null, null);
        this.setLoop(loop);
        this.updateJsonRepresentation(toscaConverter, service);
    }

    /**
     * Create an operational policy from a policy model.
     *
     * @param loop           The parent loop
     * @param service        The loop service
     * @param policyModel    The policy model
     * @param toscaConverter The tosca converter that must be used to create the Json representation
     * @throws IOException In case of issues with the legacy files (generated from resource files
     */
    public OperationalPolicy(Loop loop, Service service, PolicyModel policyModel,
                             ToscaConverterWithDictionarySupport toscaConverter) throws IOException {
        this(Policy.generatePolicyName("OPERATIONAL", service.getName(), service.getVersion(),
                policyModel.getPolicyAcronym() + '_' + policyModel.getVersion(),
                RandomStringUtils.randomAlphanumeric(3)),
                new JsonObject(),
                new JsonObject(), policyModel, null, null, null);
        this.setLoop(loop);
        this.updateJsonRepresentation(toscaConverter, service);
    }

    public void setLoop(Loop loopName) {
        this.loop = loopName;
    }

    public Loop getLoop() {
        return loop;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * name setter.
     *
     * @param name the name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void updateJsonRepresentation(ToscaConverterWithDictionarySupport toscaConverter, Service serviceModel) {
        {
            this.setJsonRepresentation(new JsonObject());
            if (this.getPolicyModel() == null) {
                return;
            }
            if (this.isLegacy()) {
                // Op policy Legacy case
                LegacyOperationalPolicy.preloadConfiguration(this.getConfigurationsJson(), this.loop);
                this.setJsonRepresentation(OperationalPolicyRepresentationBuilder
                        .generateOperationalPolicySchema(this.loop.getModelService()));
            }
            else {
                // Generic Case
                this.setJsonRepresentation(toscaConverter.convertToscaToJsonSchemaObject(
                        this.getPolicyModel().getPolicyModelTosca(),
                        this.getPolicyModel().getPolicyModelType(), serviceModel));
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OperationalPolicy other = (OperationalPolicy) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public Boolean isLegacy() {
        return (this.getPolicyModel() != null) && this.getPolicyModel().getPolicyModelType().contains(
                LegacyOperationalPolicyController.OPERATIONAL_POLICY_LEGACY);
    }

    /**
     * Create policy Yaml from json defined here.
     *
     * @return A string containing Yaml
     */
    public String createPolicyPayloadYaml() {
        JsonObject policyPayloadResult = new JsonObject();

        policyPayloadResult.addProperty("tosca_definitions_version", "tosca_simple_yaml_1_0_0");

        JsonObject topologyTemplateNode = new JsonObject();
        policyPayloadResult.add("topology_template", topologyTemplateNode);

        JsonArray policiesArray = new JsonArray();
        topologyTemplateNode.add("policies", policiesArray);

        JsonObject operationalPolicy = new JsonObject();
        policiesArray.add(operationalPolicy);

        JsonObject operationalPolicyDetails = new JsonObject();
        operationalPolicy.add(this.name, operationalPolicyDetails);
        operationalPolicyDetails.addProperty("type", "onap.policies.controlloop.Operational");
        operationalPolicyDetails.addProperty("version", "1.0.0");

        JsonObject metadata = new JsonObject();
        operationalPolicyDetails.add("metadata", metadata);
        metadata.addProperty("policy-id", this.name);

        operationalPolicyDetails.add("properties", LegacyOperationalPolicy
                .reworkActorAttributes(this.getConfigurationsJson().get("operational_policy").deepCopy()));

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Gson gson = new GsonBuilder().create();

        return (new Yaml(options)).dump(gson.fromJson(gson.toJson(policyPayloadResult), Map.class));
    }

    @Override
    public String createPolicyPayload() throws UnsupportedEncodingException {
        if (isLegacy()) {
            // Now using the legacy payload fo Dublin
            JsonObject payload = new JsonObject();
            payload.addProperty("policy-id", this.getName());
            payload.addProperty("content",
                    URLEncoder.encode(
                            LegacyOperationalPolicy
                                    .createPolicyPayloadYamlLegacy(
                                            this.getConfigurationsJson().get("operational_policy")),
                            StandardCharsets.UTF_8.toString()));
            String opPayload = new GsonBuilder().setPrettyPrinting().create().toJson(payload);
            logger.info("Operational policy payload: " + opPayload);
            return opPayload;
        }
        else {
            return super.createPolicyPayload();
        }
    }
}
