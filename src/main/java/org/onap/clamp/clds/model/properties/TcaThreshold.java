/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model.properties;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonArray;

/**
 * Parse ONAP Tca Threshold json properties.
 *
 */
public class TcaThreshold {

    protected static final EELFLogger logger      = EELFManager.getInstance().getLogger(TcaThreshold.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                    fieldPath;
    private String                    operator;
    private Integer                   threshold;
    private String                    closedLoopEventStatus;

    /**
     * Parse Tca Threshhold given json node
     *
     * @param tcaTresholdConfiguration
     */
    public TcaThreshold(JsonArray tcaTresholdConfiguration) {

        if (tcaTresholdConfiguration.get(0) != null) {
            fieldPath = tcaTresholdConfiguration.get(0).getAsString();
        }
        if (tcaTresholdConfiguration.get(1) != null) {
            operator = tcaTresholdConfiguration.get(1).getAsString();
        }
        if (tcaTresholdConfiguration.get(2) != null) {
            threshold = Integer.valueOf(tcaTresholdConfiguration.get(2).getAsString());
        }
        if (tcaTresholdConfiguration.get(3) != null) {
            closedLoopEventStatus = tcaTresholdConfiguration.get(3).getAsString();
        }
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public String getClosedLoopEventStatus() {
        return closedLoopEventStatus;
    }

    public void setClosedLoopEventStatus(String closedLoopEventStatus) {
        this.closedLoopEventStatus = closedLoopEventStatus;
    }
}
