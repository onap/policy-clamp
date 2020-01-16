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

package org.onap.clamp.loop.template;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.onap.clamp.loop.common.AuditEntity;
import org.onap.clamp.util.SemanticVersioning;

/**
 * This class represents the policy model tosca revision that we can have to a
 * specific microservice.
 */
@Entity
@Table(name = "policy_models")
@IdClass(PolicyModelId.class)
public class PolicyModel extends AuditEntity implements Serializable, Comparable<PolicyModel> {

    /**
     * The serial version id.
     */
    private static final long serialVersionUID = -286522705701376645L;

    /**
     * This variable is used to store the type mentioned in the micro-service
     * blueprint.
     */
    @Id
    @Expose
    @Column(nullable = false, name = "policy_model_type")
    private String policyModelType;

    /**
     * Semantic versioning on policy side.
     */
    @Id
    @Expose
    @Column(name = "version")
    private String version;

    @Column(columnDefinition = "MEDIUMTEXT", name = "policy_tosca")
    private String policyModelTosca;

    @Expose
    @Column(name = "policy_acronym")
    private String policyAcronym;

    @Expose
    @Column(name = "policy_variant")
    private String policyVariant;

    /**
     * policyModelTosca getter.
     * 
     * @return the policyModelTosca
     */
    public String getPolicyModelTosca() {
        return policyModelTosca;
    }

    /**
     * policyModelTosca setter.
     * 
     * @param policyModelTosca the policyModelTosca to set
     */
    public void setPolicyModelTosca(String policyModelTosca) {
        this.policyModelTosca = policyModelTosca;
    }

    /**
     * policyModelType getter.
     * 
     * @return the modelType
     */
    public String getPolicyModelType() {
        return policyModelType;
    }

    /**
     * policyModelType setter.
     * 
     * @param modelType the modelType to set
     */
    public void setPolicyModelType(String modelType) {
        this.policyModelType = modelType;
    }

    /**
     * version getter.
     * 
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * version setter.
     * 
     * @param version the version to set
     */
    public void setVersion(String version) {
        // Try to convert it before
        this.version = version;
    }

    /**
     * policyAcronym getter.
     * 
     * @return the policyAcronym value
     */
    public String getPolicyAcronym() {
        return policyAcronym;
    }

    /**
     * policyAcronym setter.
     * 
     * @param policyAcronym The policyAcronym to set
     */
    public void setPolicyAcronym(String policyAcronym) {
        this.policyAcronym = policyAcronym;
    }

    /**
     * policyVariant getter.
     * 
     * @return the policyVariant value
     */
    public String getPolicyVariant() {
        return policyVariant;
    }

    /**
     * policyVariant setter.
     * 
     * @param policyVariant The policyVariant to set
     */
    public void setPolicyVariant(String policyVariant) {
        this.policyVariant = policyVariant;
    }

    /**
     * Default constructor for serialization.
     */
    public PolicyModel() {
    }

    /**
     * Constructor.
     * 
     * @param policyType       The policyType (referenced in the blueprint)
     * @param policyModelTosca The policy tosca model in yaml
     * @param version          the version like 1.0.0
     * @param policyAcronym    Short policy name if it exists
     * @param policyVariant    Subtype for policy if it exists (could be used by UI)
     */
    public PolicyModel(String policyType, String policyModelTosca, String version, String policyAcronym,
            String policyVariant) {
        this.policyModelType = policyType;
        this.policyModelTosca = policyModelTosca;
        this.version = version;
        this.policyAcronym = policyAcronym;
        this.policyVariant = policyVariant;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((policyModelType == null) ? 0 : policyModelType.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        PolicyModel other = (PolicyModel) obj;
        if (policyModelType == null) {
            if (other.policyModelType != null) {
                return false;
            }
        } else if (!policyModelType.equals(other.policyModelType)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(PolicyModel arg0) {
        // Reverse it, so that by default we have the latest
        return SemanticVersioning.compare(arg0.getVersion(), this.version);
    }
}
