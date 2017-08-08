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

import java.math.BigDecimal;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CldsSdcResourceBasicInfo implements Comparable<CldsSdcResourceBasicInfo> {

    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(CldsSdcResourceBasicInfo.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    private String                  uuid;
    private String                  invariantUUID;
    private String                  name;
    private String                  version;
    private String                  toscaModelURL;
    private String                  category;
    private String                  subCategory;
    private String                  resourceType;
    private String                  lifecycleState;
    private String                  lastUpdaterUserId;

    @Override
    public int compareTo(CldsSdcResourceBasicInfo in) {
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

    /**
     * Convert version String into a BigDecimal
     *
     * @param version
     * @return
     */
    private BigDecimal convertVersion(String version) {
        BigDecimal rtn = new BigDecimal(0.0);
        try {
            rtn = new BigDecimal(version);
        } catch (NumberFormatException nfe) {
            logger.warn("SDC version=" + version + " is not decimal for name=" + name);
        }
        return rtn;
    }

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

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
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

    public EELFLogger getLOGGER() {
        return logger;
    }
}
