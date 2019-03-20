/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a CLDS Tosca model
 *
 */
public class CldsToscaModelDetails {

    private String id;
    private String toscaModelName;
    private String policyType;
    private List<CldsToscaModelRevision> toscaModelRevisions = new ArrayList<>();
    private String userId;
    private String lastUpdatedDate;

    /**
     * Get the id.
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id.
     * @param id
     *        the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the tosca model name.
     * @return the toscaModelName
     */
    public String getToscaModelName() {
        return toscaModelName;
    }

    /**
     * Set the tosca model name.
     * @param toscaModelName
     *        the toscaModelName to set
     */
    public void setToscaModelName(String toscaModelName) {
        this.toscaModelName = toscaModelName;
    }

    /**
     * Get the policy type.
     * @return the policyType
     */
    public String getPolicyType() {
        return policyType;
    }

    /**
     * Set the policy type.
     * @param policyType
     *        the policyType to set
     */
    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    /**
     * Get the list of tosca model revisions.
     * @return the toscaModelRevisions
     */
    public List<CldsToscaModelRevision> getToscaModelRevisions() {
        return toscaModelRevisions;
    }

    /**
     * Set the list of tosca model revisions.
     * @param toscaModelRevisions
     *        the toscaModelRevisions to set
     */
    public void setToscaModelRevisions(List<CldsToscaModelRevision> toscaModelRevisions) {
        this.toscaModelRevisions = toscaModelRevisions;
    }

    /**
     * Get the user id.
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Set the user id.
     * @param userId
     *        the userId to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Get the last updated date.
     * @return the lastUpdatedDate
     */
    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    /**
     * Set the last updated date.
     * @param lastUpdatedDate
     *        the lastUpdatedDate to set
     */
    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

}
