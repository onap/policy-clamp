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
 * 
 */

package org.onap.policy.clamp.clds.model;

import com.google.gson.annotations.Expose;

public class CldsHealthCheck {
    @Expose
    private String healthCheckComponent;
    @Expose
    private String healthCheckStatus;
    @Expose
    private String description;

    public String getHealthCheckComponent() {
        return healthCheckComponent;
    }

    public void setHealthCheckComponent(String healthCheckComponent) {
        this.healthCheckComponent = healthCheckComponent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHealthCheckStatus() {
        return healthCheckStatus;
    }

    public void setHealthCheckStatus(String healthCheckStatus) {
        this.healthCheckStatus = healthCheckStatus;
    }

}
