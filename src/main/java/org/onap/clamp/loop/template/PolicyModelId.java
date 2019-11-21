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

public class PolicyModelId implements Serializable {

    /**
     * Serial Id.
     */
    private static final long serialVersionUID = -2846526482064334745L;

    @Expose
    private String policyModelType;

    @Expose
    private String version;

    /**
     * Default constructor for serialization.
     */
    public PolicyModelId() {

    }

    /**
     * Constructor.
     */
    public PolicyModelId(String policyModelType, String version) {
        this.policyModelType = policyModelType;
        this.version = version;
    }

    /**
     * policyModelType getter.
     * 
     * @return the policyModelType
     */
    public String getPolicyModelType() {
        return policyModelType;
    }

    /**
     * policyModelType setter.
     * 
     * @param policyModelType the policyModelType to set
     */
    public void setPolicyModelType(String policyModelType) {
        this.policyModelType = policyModelType;
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
        this.version = version;
    }
}
