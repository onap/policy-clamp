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

/**
 * Represents a CLDS Tosca model revision
 *
 */
package org.onap.clamp.clds.model;

public class CldsToscaModelRevision {

    private String revisionId;
    private String toscaModelYaml;
    private double version;
    private String toscaModelJson;
    private String userId;
    private String createdDate;
    private String lastUpdatedDate;

    /**
     * @return the revisionId
     */
    public String getRevisionId() {
        return revisionId;
    }

    /**
     * @param revisionId
     *        the revisionId to set
     */
    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    /**
     * @return the toscaModelYaml
     */
    public String getToscaModelYaml() {
        return toscaModelYaml;
    }

    /**
     * @param toscaModelYaml
     *        the toscaModelYaml to set
     */
    public void setToscaModelYaml(String toscaModelYaml) {
        this.toscaModelYaml = toscaModelYaml;
    }

    /**
     * @return the version
     */
    public double getVersion() {
        return version;
    }

    /**
     * @param version
     *        the version to set
     */
    public void setVersion(double version) {
        this.version = version;
    }

    /**
     * @return the toscaModelJson
     */
    public String getToscaModelJson() {
        return toscaModelJson;
    }

    /**
     * @param toscaModelJson
     *        the toscaModelJson to set
     */
    public void setToscaModelJson(String toscaModelJson) {
        this.toscaModelJson = toscaModelJson;
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
     * @return the createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate
     *        the createdDate to set
     */
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
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
