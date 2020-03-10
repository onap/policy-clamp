/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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

package org.onap.clamp.policy;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONObject;
import org.onap.clamp.clds.tosca.update.TemplateManagement;
import org.onap.clamp.clds.tosca.update.UnknownComponentException;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.dao.model.jsontype.StringJsonUserType;
import org.onap.clamp.loop.common.AuditEntity;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.PolicyModel;
import org.yaml.snakeyaml.Yaml;

@MappedSuperclass
@TypeDefs({@TypeDef(name = "json", typeClass = StringJsonUserType.class)})
public abstract class Policy extends AuditEntity {

    @Transient
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(Policy.class);

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "json_representation", nullable = false)
    private JsonObject jsonRepresentation;

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "configurations_json")
    private JsonObject configurationsJson;

    /**
     * This attribute can be null when the user add a policy on the loop instance, not the template.
     * When null, It therefore indicates that this policy is not by default in the loop template.
     */
    @Expose
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loop_element_model_id")
    private LoopElementModel loopElementModel;

    @Expose
    @Column(name = "pdp_group")
    private String pdpGroup;

    @Expose
    @Column(name = "pdp_sub_group")
    private String pdpSubgroup;

    @Expose
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({@JoinColumn(name = "policy_model_type", referencedColumnName = "policy_model_type"),
            @JoinColumn(name = "policy_model_version", referencedColumnName = "version")})
    private PolicyModel policyModel;

    private JsonObject createJsonFromPolicyTosca() {
        Map<String, Object> map =
                new Yaml().load(this.getPolicyModel() != null ? this.getPolicyModel().getPolicyModelTosca() : "");
        JSONObject jsonObject = new JSONObject(map);
        return new Gson().fromJson(jsonObject.toString(), JsonObject.class);
    }

    private String getModelPropertyNameFromTosca(JsonObject object, String policyModelType) {
        return object.getAsJsonObject("policy_types").getAsJsonObject(policyModelType)
                .getAsJsonObject(
                        "properties")
                .keySet().toArray(new String[1])[0];
    }

    /**
     * This method create the policy payload that must be sent to PEF.
     *
     * @return A String containing the payload
     * @throws UnsupportedEncodingException In case of failure
     */
    public String createPolicyPayload() throws UnsupportedEncodingException {
        JsonObject toscaJson = createJsonFromPolicyTosca();

        JsonObject policyPayloadResult = new JsonObject();

        policyPayloadResult.add("tosca_definitions_version", toscaJson.get("tosca_definitions_version"));

        JsonObject topologyTemplateNode = new JsonObject();
        policyPayloadResult.add("topology_template", topologyTemplateNode);

        JsonArray policiesArray = new JsonArray();
        topologyTemplateNode.add("policies", policiesArray);

        JsonObject thisPolicy = new JsonObject();
        policiesArray.add(thisPolicy);

        JsonObject policyDetails = new JsonObject();
        thisPolicy.add(this.getName(), policyDetails);
        policyDetails.addProperty("type", this.getPolicyModel().getPolicyModelType());
        policyDetails.addProperty("version", this.getPolicyModel().getVersion());

        JsonObject policyMetadata = new JsonObject();
        policyDetails.add("metadata", policyMetadata);
        policyMetadata.addProperty("policy-id", this.getName());

        JsonObject policyProperties = new JsonObject();
        policyDetails.add("properties", policyProperties);
        policyProperties
                .add(this.getModelPropertyNameFromTosca(toscaJson, this.getPolicyModel().getPolicyModelType()),
                        this.getConfigurationsJson());
        String policyPayload = new GsonBuilder().setPrettyPrinting().create().toJson(policyPayloadResult);
        logger.info("Policy payload: " + policyPayload);
        return policyPayload;
    }


    /**
     * Name getter.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Name setter.
     */
    public abstract void setName(String name);

    /**
     * jsonRepresentation getter.
     *
     * @return the jsonRepresentation
     */
    public JsonObject getJsonRepresentation() {
        return jsonRepresentation;
    }

    /**
     * jsonRepresentation setter.
     *
     * @param jsonRepresentation The jsonRepresentation to set
     */
    public void setJsonRepresentation(JsonObject jsonRepresentation) {
        this.jsonRepresentation = jsonRepresentation;
    }

    /**
     * policyModel getter.
     *
     * @return the policyModel
     */
    public PolicyModel getPolicyModel() {
        return policyModel;
    }

    /**
     * policyModel setter.
     *
     * @param policyModel The new policyModel
     */
    public void setPolicyModel(PolicyModel policyModel) {
        this.policyModel = policyModel;
    }

    /**
     * configurationsJson getter.
     *
     * @return The configurationsJson
     */
    public JsonObject getConfigurationsJson() {
        return configurationsJson;
    }

    /**
     * configurationsJson setter.
     *
     * @param configurationsJson the configurationsJson to set
     */
    public void setConfigurationsJson(JsonObject configurationsJson) {
        this.configurationsJson = configurationsJson;
    }

    /**
     * loopElementModel getter.
     *
     * @return the loopElementModel
     */
    public LoopElementModel getLoopElementModel() {
        return loopElementModel;
    }

    /**
     * loopElementModel setter.
     *
     * @param loopElementModel the loopElementModel to set
     */
    public void setLoopElementModel(LoopElementModel loopElementModel) {
        this.loopElementModel = loopElementModel;
    }

    /**
     * pdpGroup getter.
     *
     * @return the pdpGroup
     */
    public String getPdpGroup() {
        return pdpGroup;
    }

    /**
     * pdpGroup setter.
     *
     * @param pdpGroup the pdpGroup to set
     */
    public void setPdpGroup(String pdpGroup) {
        this.pdpGroup = pdpGroup;
    }

    /**
     * pdpSubgroup getter.
     *
     * @return the pdpSubgroup
     */
    public String getPdpSubgroup() {
        return pdpSubgroup;
    }

    /**
     * pdpSubgroup setter.
     *
     * @param pdpSubgroup the pdpSubgroup to set
     */
    public void setPdpSubgroup(String pdpSubgroup) {
        this.pdpSubgroup = pdpSubgroup;
    }

    /**
     * Generate the policy name.
     *
     * @param policyType        The policy type
     * @param serviceName       The service name
     * @param serviceVersion    The service version
     * @param resourceName      The resource name
     * @param blueprintFilename The blueprint file name
     * @return The generated policy name
     */
    public static String generatePolicyName(String policyType, String serviceName, String serviceVersion,
                                            String resourceName, String blueprintFilename) {
        StringBuilder buffer = new StringBuilder(policyType).append("_").append(serviceName).append("_v")
                .append(serviceVersion).append("_").append(resourceName).append("_")
                .append(blueprintFilename.replaceAll(".yaml", ""));
        return buffer.toString().replace('.', '_').replaceAll(" ", "");
    }

    /**
     * This method can be used to generate the json Schema used by the UI.
     *
     * @param policyToscaModel The tosca model as String that must be converted
     * @param policyModelType The tosca model type (the policy_type entry in the tosca) that will used to create the
     *                        json schema
     * @return THe Json Schema as JsonObject
     * @throws IOException In case of failure when opening the templates.properties file
     * @throws UnknownComponentException If the policyModelType is not found in the tosca model
     */
    public static JsonObject generateJsonRepresentationFromToscaModel(String policyToscaModel,
                                                                      String policyModelType)
            throws IOException, UnknownComponentException {
        return new TemplateManagement(policyToscaModel,ResourceFileUtil.getResourceAsString(
                "clds/tosca_update/defaultToscaTypes.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"))
                .launchTranslation(policyModelType);
    }
}
