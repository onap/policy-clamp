/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.policy.pdpgroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * This class maps the get Pdp Group response to a nice pojo.
 */
public class PdpGroup {

    @Expose
    private String name;

    @Expose
    private String pdpGroupState;

    @Expose
    private List<PdpSubgroup> pdpSubgroups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPdpGroupState() {
        return pdpGroupState;
    }

    public void setPdpGroupState(String pdpGroupState) {
        this.pdpGroupState = pdpGroupState;
    }

    public List<PdpSubgroup> getPdpSubgroups() {
        return pdpSubgroups;
    }

    public void setPdpSubgroups(List<PdpSubgroup> pdpSubgroups) {
        this.pdpSubgroups = pdpSubgroups;
    }

    /**
     * Get supported subGroups based on the defined policy type and version.
     * @param policyType The policy type
     * @param version The version
     * @return The supported subGroup list in Json format
     */
    public JsonObject getSupportedSubgroups(String policyType, String version) {
        if (!pdpGroupState.equalsIgnoreCase("ACTIVE")) {
            return null;
        }
        JsonArray supportedSubgroups =  new JsonArray();
        for (PdpSubgroup subGroup : pdpSubgroups) {
            if (subGroup.getSupportedPolicyTypes().contains(new PolicyModelKey(policyType, version))) {
                supportedSubgroups.add(subGroup.getPdpType());
            }
        }
        if (supportedSubgroups.size() > 0) {
            JsonObject supportedPdpGroup = new JsonObject();
            supportedPdpGroup.add(this.name, supportedSubgroups);
            return supportedPdpGroup;
        }
        return null;
    }
}
