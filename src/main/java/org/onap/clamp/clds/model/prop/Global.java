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

package org.onap.clamp.clds.model.prop;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.logging.Logger;

/**
 * Parse global json properties.
 * <p>
 * Example json: "global":[{"name":"service","value":["vUSP"]},{"name":"vnf","value":["vCTS","v3CDB"]},{"name":"location","value":["san_diego","san_antonio","kansas_city","kings_mountain","Secaucus","lisle","concord","houston","akron"]}]
 */
public class Global {
    private static final Logger logger = Logger.getLogger(Global.class.getName());

    private String service;
    private List<String> resourceVf;
    private List<String> resourceVfc;
    private List<String> location;

    /**
     * Parse global given json node.
     *
     * @param modelJson
     */
    public Global(JsonNode modelJson) {
        JsonNode globalNode = modelJson.get("global");
        service = ModelElement.getValueByName(globalNode, "service");
        resourceVf = ModelElement.getValuesByName(globalNode, "vf");
        resourceVfc = ModelElement.getValuesByName(globalNode, "vfc");
        location = ModelElement.getValuesByName(globalNode, "location");
    }

    /**
     * @return the service
     */
    public String getService() {
        return service;
    }

    /**
     * @param service the service to set
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * @return the resourceVf
     */
    public List<String> getResourceVf() {
        return resourceVf;
    }

    /**
     * @param resourceVf the resourceVf to set
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
     * @param resourceVfc the resourceVfc to set
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
     * @param location the location to set
     */
    public void setLocation(List<String> location) {
        this.location = location;
    }

}
