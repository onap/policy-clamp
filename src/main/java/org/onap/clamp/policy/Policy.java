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

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import java.io.UnsupportedEncodingException;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.onap.clamp.dao.model.jsontype.StringJsonUserType;
import org.onap.clamp.loop.common.AuditEntity;
import org.onap.clamp.loop.template.LoopElementModel;

@MappedSuperclass
@TypeDefs({@TypeDef(name = "json", typeClass = StringJsonUserType.class)})
public abstract class Policy extends AuditEntity {

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

    public abstract String createPolicyPayload() throws UnsupportedEncodingException;

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

}
