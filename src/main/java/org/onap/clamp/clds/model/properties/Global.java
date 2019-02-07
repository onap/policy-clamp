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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import org.onap.clamp.clds.util.JsonUtils;

/**
 * Parse global json properties.
 * <p>
 * Example json:
 * "global":[{"name":"service","value":["vUSP"]},{"name":"vnf","value":["vCTS",
 * "v3CDB"]},{"name":"location","value":["san_diego","san_antonio","kansas_city"
 * ,"kings_mountain","Secaucus","lisle","concord","houston","akron"]}]
 */
public class Global {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(Global.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();
    private String service;
    private String actionSet;
    private List<String> resourceVf;
    private List<String> resourceVfc;
    private JsonObject deployParameters;
    private List<String> location;
    private String vnfScope;

    /**
     * Parse global given json node.
     *
     * @param modelJson
     */
    public Global(JsonObject modelJson) {
        JsonElement globalNode = modelJson.get("global");
        service = JsonUtils.getStringValueByName(globalNode, "service");
        actionSet = JsonUtils.getStringValueByName(globalNode, "actionSet");
        resourceVf = JsonUtils.getStringValuesByName(globalNode, "vf");
        resourceVfc = JsonUtils.getStringValuesByName(globalNode, "vfc");
        deployParameters = JsonUtils.getJsonObjectByName(globalNode, "deployParameters");
        location = JsonUtils.getStringValuesByName(globalNode, "location");
        vnfScope = JsonUtils.getStringValueByName(globalNode, "vnf");
    }

    /**
     * @return the service
     */
    public String getService() {
        return service;
    }

    /**
     * @param service
     *            the service to set
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * @return the actionSet
     */
    public String getActionSet() {
        return actionSet;
    }

    public void setActionSet(String actionSet) {
        this.actionSet = actionSet;
    }

    /**
     * @return the resourceVf
     */
    public List<String> getResourceVf() {
        return resourceVf;
    }

    /**
     * @param resourceVf
     *            the resourceVf to set
     */
    public void setResourceVf(List<String> resourceVf) {
        this.resourceVf = resourceVf;
    }

    /**
     * @return the resourceVfc
     */
    public List<String> getResourceVfc() {
        return resourceVfc;
    }

    /**
     * @param resourceVfc
     *            the resourceVfc to set
     */
    public void setResourceVfc(List<String> resourceVfc) {
        this.resourceVfc = resourceVfc;
    }

    /**
     * @return the location
     */
    public List<String> getLocation() {
        return location;
    }

    /**
     * @param location
     *            the location to set
     */
    public void setLocation(List<String> location) {
        this.location = location;
    }

    public JsonObject getDeployParameters() {
        return deployParameters;
    }

    public void setDeployParameters(JsonObject deployParameters) {
        this.deployParameters = deployParameters;
    }

    public String getVnfScope() {
        return vnfScope;
    }

}
