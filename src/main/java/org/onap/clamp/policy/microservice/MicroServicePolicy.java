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

package org.onap.clamp.policy.microservice;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONObject;
import org.onap.clamp.clds.tosca.ToscaYamlToJsonConvertor;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.dao.model.jsontype.StringJsonUserType;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.policy.Policy;
import org.yaml.snakeyaml.Yaml;

@Entity
@Table(name = "micro_service_policies")
@TypeDefs({@TypeDef(name = "json", typeClass = StringJsonUserType.class)})
public class MicroServicePolicy extends Policy implements Serializable {
    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = 6271238288583332616L;

    @Transient
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(MicroServicePolicy.class);

    @Expose
    @Id
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @Expose
    @Column(name = "context")
    private String context;

    @Expose
    @Column(name = "device_type_scope")
    private String deviceTypeScope;

    @Expose
    @Column(name = "shared", nullable = false)
    private Boolean shared;

    @ManyToMany(mappedBy = "microServicePolicies", fetch = FetchType.EAGER)
    private Set<Loop> usedByLoops = new HashSet<>();

    @Expose
    @Column(name = "dcae_deployment_id")
    private String dcaeDeploymentId;

    @Expose
    @Column(name = "dcae_deployment_status_url")
    private String dcaeDeploymentStatusUrl;

    @Expose
    @Column(name = "dcae_blueprint_id")
    private String dcaeBlueprintId;

    @Expose
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({@JoinColumn(name = "policy_model_type", referencedColumnName = "policy_model_type"),
            @JoinColumn(name = "policy_model_version", referencedColumnName = "version")})
    private PolicyModel policyModel;

    public MicroServicePolicy() {
        // serialization
    }

    /**
     * The constructor that creates the json representation from the policyTosca
     * using the ToscaYamlToJsonConvertor.
     *
     * @param name        The name of the MicroService
     * @param policyModel The policy model of the MicroService
     * @param shared      The flag indicate whether the MicroService is shared
     */
    public MicroServicePolicy(String name, PolicyModel policyModel, Boolean shared, LoopElementModel loopElementModel) {
        this(name,policyModel,shared,JsonUtils.GSON_JPA_MODEL
                .fromJson(new ToscaYamlToJsonConvertor().parseToscaYaml(policyModel.getPolicyModelTosca(),
                        policyModel.getPolicyModelType()), JsonObject.class),loopElementModel, null, null);
    }

    private JsonObject createJsonFromPolicyTosca() {
        Map<String, Object> map = new Yaml().load(this.getPolicyModel().getPolicyModelTosca());
        JSONObject jsonObject = new JSONObject(map);
        return new Gson().fromJson(jsonObject.toString(), JsonObject.class);
    }

    /**
     * The constructor that does not make use of ToscaYamlToJsonConvertor but take
     * the jsonRepresentation instead.
     * @param name               The name of the MicroService
     * @param policyModel        The policy model type of the MicroService
     * @param shared             The flag indicate whether the MicroService is
 *                           shared
     * @param jsonRepresentation The UI representation in json format
     * @param loopElementModel   The loop element model from which this instance should be created
     * @param pdpGroup           The Pdp Group info
     * @param pdpSubgroup        The Pdp Subgrouop info
     */
    public MicroServicePolicy(String name, PolicyModel policyModel, Boolean shared,
                              JsonObject jsonRepresentation, LoopElementModel loopElementModel, String pdpGroup, String pdpSubgroup) {
        this.name = name;
        this.policyModel = policyModel;
        this.shared = shared;
        this.setJsonRepresentation(jsonRepresentation);
        this.setLoopElementModel(loopElementModel);
        this.setPdpGroup(pdpGroup);
        this.setPdpSubgroup(pdpSubgroup);
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

    public Boolean getShared() {
        return shared;
    }

    void setShared(Boolean shared) {
        this.shared = shared;
    }

    public Set<Loop> getUsedByLoops() {
        return usedByLoops;
    }

    void setUsedByLoops(Set<Loop> usedBy) {
        this.usedByLoops = usedBy;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDeviceTypeScope() {
        return deviceTypeScope;
    }

    public void setDeviceTypeScope(String deviceTypeScope) {
        this.deviceTypeScope = deviceTypeScope;
    }

    public PolicyModel getPolicyModel() {
        return policyModel;
    }

    public void setPolicyModel(PolicyModel policyModel) {
        this.policyModel = policyModel;
    }

    /**
     * dcaeDeploymentId getter.
     *
     * @return the dcaeDeploymentId
     */
    public String getDcaeDeploymentId() {
        return dcaeDeploymentId;
    }

    /**
     * dcaeDeploymentId setter.
     *
     * @param dcaeDeploymentId the dcaeDeploymentId to set
     */
    public void setDcaeDeploymentId(String dcaeDeploymentId) {
        this.dcaeDeploymentId = dcaeDeploymentId;
    }

    /**
     * dcaeDeploymentStatusUrl getter.
     *
     * @return the dcaeDeploymentStatusUrl
     */
    public String getDcaeDeploymentStatusUrl() {
        return dcaeDeploymentStatusUrl;
    }

    /**
     * dcaeDeploymentStatusUrl setter.
     *
     * @param dcaeDeploymentStatusUrl the dcaeDeploymentStatusUrl to set
     */
    public void setDcaeDeploymentStatusUrl(String dcaeDeploymentStatusUrl) {
        this.dcaeDeploymentStatusUrl = dcaeDeploymentStatusUrl;
    }

    /**
     * dcaeBlueprintId getter.
     *
     * @return the dcaeBlueprintId
     */
    public String getDcaeBlueprintId() {
        return dcaeBlueprintId;
    }

    /**
     * dcaeBlueprintId setter.
     *
     * @param dcaeBlueprintId the dcaeBlueprintId to set
     */
    void setDcaeBlueprintId(String dcaeBlueprintId) {
        this.dcaeBlueprintId = dcaeBlueprintId;
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
        MicroServicePolicy other = (MicroServicePolicy) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }        return true;
    }

    private String getMicroServicePropertyNameFromTosca(JsonObject object) {
        return object.getAsJsonObject("policy_types").getAsJsonObject(this.getPolicyModel().getPolicyModelType())
                .getAsJsonObject(
                        "properties")
                .keySet().toArray(new String[1])[0];
    }

    @Override
    public String createPolicyPayload() {
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
        policyProperties.add(this.getMicroServicePropertyNameFromTosca(toscaJson), this.getConfigurationsJson());
        String policyPayload = new GsonBuilder().setPrettyPrinting().create().toJson(policyPayloadResult);
        logger.info("Micro service policy payload: " + policyPayload);
        return policyPayload;
    }

}
