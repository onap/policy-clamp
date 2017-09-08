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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.math.BigDecimal;

public class CldsSdcServiceInfo implements Comparable<CldsSdcServiceInfo> {

    protected static final EELFLogger logger      = EELFManager.getInstance().getLogger(CldsSdcServiceInfo.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                    uuid;
    private String                    invariantUUID;
    private String                    name;
    private String                    version;
    private String                    toscaModelURL;
    private String                    category;
    private String                    lifecycleState;
    private String                    lastUpdaterUserId;
    private String                    distributionStatus;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getInvariantUUID() {
        return invariantUUID;
    }

    public void setInvariantUUID(String invariantUUID) {
        this.invariantUUID = invariantUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getToscaModelURL() {
        return toscaModelURL;
    }

    public void setToscaModelURL(String toscaModelURL) {
        this.toscaModelURL = toscaModelURL;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(String lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public String getLastUpdaterUserId() {
        return lastUpdaterUserId;
    }

    public void setLastUpdaterUserId(String lastUpdaterUserId) {
        this.lastUpdaterUserId = lastUpdaterUserId;
    }

    public String getDistributionStatus() {
        return distributionStatus;
    }

    public void setDistributionStatus(String distributionStatus) {
        this.distributionStatus = distributionStatus;
    }

    /**
     * Compare using name and then version. Version is converted to a decimal.
     */
    @Override
    public int compareTo(CldsSdcServiceInfo in) {
        // Compares this object with the specified object for order.
        // Returns a negative integer, zero, or a positive integer as this
        // object is less than, equal to, or greater than the specified object.
        // first compare based on name
        int rtn = name.compareToIgnoreCase(in.name);

        if (rtn == 0) {
            BigDecimal myVersion = convertVersion(version);
            BigDecimal inVersion = convertVersion(in.version);
            rtn = myVersion.compareTo(inVersion);
        }

        return rtn;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CldsSdcServiceInfo other = (CldsSdcServiceInfo) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    /**
     * Convert version String into a BigDecimal
     *
     * @param versionText
     * @return
     */
    private BigDecimal convertVersion(String versionText) {
        try {
            return new BigDecimal(versionText);
        } catch (NumberFormatException nfe) {
            logger.warn("SDC version=" + versionText + " is not decimal for name=" + name);
        }
        return BigDecimal.valueOf(0.0);
    }

}
