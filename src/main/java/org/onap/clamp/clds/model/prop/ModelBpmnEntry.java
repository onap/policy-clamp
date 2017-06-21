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

import java.util.logging.Logger;

/**
 * Model BPMN property entry
 * <p>
 * Example json: {"collector":[{"id":"Collector_11r50j1", "from":"StartEvent_1"}],"stringMatch":[{"id":"StringMatch_0h6cbdv"],"policy":[{"id":"Policy_0oxeocn", "from":"StringMatch_0h6cbdv"}]}
 */
public class ModelBpmnEntry {
    private static final Logger logger = Logger.getLogger(ModelBpmnEntry.class.getName());

    private String type;
    private String id;
    private String fromId;

    /**
     * Parse the json so that the "id" and "from" fields can be retrieved on demand.
     *
     * @param type
     * @param id
     * @param fromId
     */
    public ModelBpmnEntry(String type, String id, String fromId) {
        this.type = type;
        this.id = id;
        this.fromId = fromId;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the fromId
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * @param fromId the fromId to set
     */
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

}
