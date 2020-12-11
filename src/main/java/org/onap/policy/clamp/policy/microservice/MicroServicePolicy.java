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

package org.onap.policy.clamp.policy.microservice;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.onap.policy.clamp.clds.tosca.update.ToscaConverterWithDictionarySupport;
import org.onap.policy.clamp.dao.model.jsontype.StringJsonUserType;
import org.onap.policy.clamp.loop.Loop;
import org.onap.policy.clamp.loop.service.Service;
import org.onap.policy.clamp.loop.template.LoopElementModel;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.policy.Policy;

@Entity
@Table(name = "micro_service_policies")
@TypeDefs({@TypeDef(name = "json", typeClass = StringJsonUserType.class)})
public class MicroServicePolicy extends Policy implements Serializable {
    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = 6271238288583332616L;

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

    /**
     * Constructor for serialization.
     */
    public MicroServicePolicy() {
    }

    /**
     * The constructor that does not make use of ToscaYamlToJsonConvertor but take
     * the jsonRepresentation instead.
     *
     * @param name               The name of the MicroService
     * @param policyModel        The policy model type of the MicroService
     * @param shared             The flag indicate whether the MicroService is
     *                           shared
     * @param jsonRepresentation The UI representation in json format
     * @param loopElementModel   The loop element model from which this instance should be created
     * @param pdpGroup           The Pdp Group info
     * @param pdpSubgroup        The Pdp Subgroup info
     */
    public MicroServicePolicy(String name, PolicyModel policyModel, Boolean shared,
                              JsonObject jsonRepresentation, LoopElementModel loopElementModel, String pdpGroup,
                              String pdpSubgroup) {
        this.name = name;
        this.setPolicyModel(policyModel);
        this.shared = shared;
        this.setJsonRepresentation(jsonRepresentation);
        this.setLoopElementModel(loopElementModel);
        this.setPdpGroup(pdpGroup);
        this.setPdpSubgroup(pdpSubgroup);
    }

    /**
     * Constructor with tosca converter.
     *
     * @param loop             The loop instance
     * @param service          The service model object
     * @param loopElementModel The loop element model from which this microservice instance is created
     * @param toscaConverter   The tosca converter that will used to convert the tosca policy model
     */
    public MicroServicePolicy(Loop loop, Service service, LoopElementModel loopElementModel,
                              ToscaConverterWithDictionarySupport toscaConverter) {
        this(Policy.generatePolicyName("MICROSERVICE", service.getName(), service.getVersion(),
                loopElementModel.getPolicyModels().first().getPolicyAcronym() + '_'
                        + loopElementModel.getPolicyModels().first().getVersion(),
                RandomStringUtils.randomAlphanumeric(3)),
                loopElementModel.getPolicyModels().first(), false, new JsonObject(), loopElementModel, null, null);
        this.updateJsonRepresentation(toscaConverter, service);
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
        this.setJsonRepresentation(
                toscaConverter.convertToscaToJsonSchemaObject(this.getPolicyModel().getPolicyModelTosca(),
                        this.getPolicyModel().getPolicyModelType(), serviceModel));
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
        }
        return true;
    }
}
