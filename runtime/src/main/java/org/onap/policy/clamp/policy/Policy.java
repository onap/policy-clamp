/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.policy;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import java.io.UnsupportedEncodingException;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.onap.policy.clamp.dao.model.jsontype.StringJsonUserType;
import org.onap.policy.clamp.loop.common.AuditEntity;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MappedSuperclass
@TypeDef(name = "json", typeClass = StringJsonUserType.class)
public abstract class Policy extends AuditEntity {

    @Transient
    private static final Logger logger = LoggerFactory.getLogger(Policy.class);

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "json_representation", nullable = false)
    private JsonObject jsonRepresentation;

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "configurations_json")
    private JsonObject configurationsJson;

    @Expose
    @Column(name = "pdp_group")
    private String pdpGroup;

    @Expose
    @Column(name = "pdp_sub_group")
    private String pdpSubgroup;

    @Expose
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_model_type", referencedColumnName = "policy_model_type")
    @JoinColumn(name = "policy_model_version", referencedColumnName = "version")
    private PolicyModel policyModel;

    /**
     * This method create the policy payload that must be sent to PEF.
     *
     * @return A String containing the payload
     * @throws UnsupportedEncodingException In case of failure
     */
    public String createPolicyPayload() throws UnsupportedEncodingException {
        return PolicyPayload
                .createPolicyPayload(this.getPolicyModel().getPolicyModelType(), this.getPolicyModel().getVersion(),
                        this.getName(), this.getPolicyModel().getVersion(), this.getConfigurationsJson(),
                        this.getPolicyModel() != null ? this.getPolicyModel().getPolicyModelTosca() : "");
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
        return buffer.toString().replace('.', '_').replace(" ", "");
    }
}
