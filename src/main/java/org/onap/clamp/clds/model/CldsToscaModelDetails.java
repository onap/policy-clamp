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
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *        the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the toscaModelName
     */
    public String getToscaModelName() {
        return toscaModelName;
    }

    /**
     * @param toscaModelName
     *        the toscaModelName to set
     */
    public void setToscaModelName(String toscaModelName) {
        this.toscaModelName = toscaModelName;
    }

    /**
     * @return the policyType
     */
    public String getPolicyType() {
        return policyType;
    }

    /**
     * @param policyType
     *        the policyType to set
     */
    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    /**
     * @return the toscaModelRevisions
     */
    public List<CldsToscaModelRevision> getToscaModelRevisions() {
        return toscaModelRevisions;
    }

    /**
     * @param toscaModelRevisions
     *        the toscaModelRevisions to set
     */
    public void setToscaModelRevisions(List<CldsToscaModelRevision> toscaModelRevisions) {
        this.toscaModelRevisions = toscaModelRevisions;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId
     *        the userId to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @return the lastUpdatedDate
     */
    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    /**
     * @param lastUpdatedDate
     *        the lastUpdatedDate to set
     */
    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

}
