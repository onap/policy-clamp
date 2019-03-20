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
     * @param modelJson The model in json format.
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
     * Get the service.
     * @return the service
     */
    public String getService() {
        return service;
    }

    /**
     * Set the service.
     * @param service
     *            the service to set
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * Get the action set.
     * @return the actionSet
     */
    public String getActionSet() {
        return actionSet;
    }

    /**
     * Set tje actionSet.
     * @param actionSet
     *             The actionSet to set
     */
    public void setActionSet(String actionSet) {
        this.actionSet = actionSet;
    }

    /**
     * Get the resource vf.
     * @return the resourceVf
     */
    public List<String> getResourceVf() {
        return resourceVf;
    }

    /**
     * Set the resourceVf.
     * @param resourceVf
     *            the resourceVf to set
     */
    public void setResourceVf(List<String> resourceVf) {
        this.resourceVf = resourceVf;
    }

    /**
     * Get the resource Vfc.
     * @return the resourceVfc
     */
    public List<String> getResourceVfc() {
        return resourceVfc;
    }

    /**
     * Set tje respirce Vfc.
     * @param resourceVfc
     *            the resourceVfc to set
     */
    public void setResourceVfc(List<String> resourceVfc) {
        this.resourceVfc = resourceVfc;
    }

    /**
     * Get the list of locations.
     * @return the location
     */
    public List<String> getLocation() {
        return location;
    }

    /**
     * Set the list of locations.
     * @param location
     *            the location to set
     */
    public void setLocation(List<String> location) {
        this.location = location;
    }

    /**
     * Get the deploy parameters.
     * @return The deploy parameters
     */
    public JsonObject getDeployParameters() {
        return deployParameters;
    }

    /**
     * Set the deploy parameters.
     * @param deployParameters
     *         the deploy parameters to set
     */
    public void setDeployParameters(JsonObject deployParameters) {
        this.deployParameters = deployParameters;
    }

    /**
     * Get the vnf scope.
     * @return The vnf scope
     */
    public String getVnfScope() {
        return vnfScope;
    }

}
