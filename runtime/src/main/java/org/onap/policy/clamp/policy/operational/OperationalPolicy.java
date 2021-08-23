/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
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

package org.onap.policy.clamp.policy.operational;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
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
import org.onap.policy.clamp.clds.tosca.update.ToscaConverterWithDictionarySupport;
import org.onap.policy.clamp.dao.model.jsontype.StringJsonUserType;
import org.onap.policy.clamp.loop.Loop;
import org.onap.policy.clamp.loop.service.Service;
import org.onap.policy.clamp.loop.template.LoopElementModel;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.policy.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "operational_policies")
@TypeDef(name = "json", typeClass = StringJsonUserType.class)
public class OperationalPolicy extends Policy implements Serializable {
    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = 6117076450841538255L;

    @Transient
    private static final Logger logger = LoggerFactory.getLogger(OperationalPolicy.class);

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
                RandomStringUtils.random(3, 0, 0, true, true, null, new SecureRandom())), new JsonObject(),
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
     */
    public OperationalPolicy(Loop loop, Service service, PolicyModel policyModel,
                             ToscaConverterWithDictionarySupport toscaConverter) {
        this(Policy.generatePolicyName("OPERATIONAL", service.getName(), service.getVersion(),
                policyModel.getPolicyAcronym() + '_' + policyModel.getVersion(),
                RandomStringUtils.random(3, 0, 0, true, true, null, new SecureRandom())),
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
        this.setJsonRepresentation(new JsonObject());
        if (this.getPolicyModel() == null) {
            return;
        }

        // Generic Case
        this.setJsonRepresentation(toscaConverter.convertToscaToJsonSchemaObject(
                this.getPolicyModel().getPolicyModelTosca(),
                this.getPolicyModel().getPolicyModelType(), serviceModel));
    }

    @Override
    public int hashCode() {
        final var prime = 31;
        var result = 1;
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
        } else {
            if (!name.equals(other.name)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String createPolicyPayload() throws UnsupportedEncodingException {
        return super.createPolicyPayload();

    }
}
