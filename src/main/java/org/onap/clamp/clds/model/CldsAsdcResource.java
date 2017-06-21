/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CldsAsdcResource implements Comparable<CldsAsdcResource> {
    private static final Logger logger = Logger.getLogger(CldsAsdcServiceInfo.class.getName());

    private String resourceInstanceName;
    private String resourceName;
    private String resourceInvariantUUID;
    private String resourceVersion;
    private String resoucreType;
    private String resourceUUID;
    private List<CldsAsdcArtifact> artifacts;

    public String getResourceInstanceName() {
        return resourceInstanceName;
    }

    public void setResourceInstanceName(String resourceInstanceName) {
        this.resourceInstanceName = resourceInstanceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceInvariantUUID() {
        return resourceInvariantUUID;
    }

    public void setResourceInvariantUUID(String resourceInvariantUUID) {
        this.resourceInvariantUUID = resourceInvariantUUID;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public String getResoucreType() {
        return resoucreType;
    }

    public void setResoucreType(String resoucreType) {
        this.resoucreType = resoucreType;
    }

    public String getResourceUUID() {
        return resourceUUID;
    }

    public void setResourceUUID(String resourceUUID) {
        this.resourceUUID = resourceUUID;
    }

    public List<CldsAsdcArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<CldsAsdcArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    @Override
    public int compareTo(CldsAsdcResource in) {
        // Compares this object with the specified object for order.
        // Returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.

        // first compare based on name
        int rtn = resourceInstanceName.compareToIgnoreCase(in.resourceInstanceName);

        if (rtn == 0) {
            BigDecimal myVersion = convertVersion(resourceVersion);
            BigDecimal inVersion = convertVersion(in.resourceVersion);
            rtn = myVersion.compareTo(inVersion);
        }
        return rtn;
    }

    /**
     * Convert version String into a BigDecimal
     *
     * @param versionText
     * @return
     */
    private BigDecimal convertVersion(String versionText) {
        BigDecimal rtn = new BigDecimal(0.0);
        try {
            rtn = new BigDecimal(versionText);
        } catch (NumberFormatException nfe) {
            logger.warning("ASDC version=" + versionText + " is not decimal for name=" + resourceInstanceName);
        }
        return rtn;
    }
}
