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

/**
 * Model BPMN property entry.
 * Example json: {"policy":
 * [{"id":"Policy_0oxeocn", "from":"StartEvent_1"}]}
 */
public class ModelBpmnEntry {
    protected static final EELFLogger       logger      = EELFManager.getInstance().getLogger(ModelBpmnEntry.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                  type;
    private String                  id;
    private String                  fromId;

    /**
     * Parse the json so that the "id" and "from" fields can be retrieved on
     * demand.
     *
     * @param type The type
     * @param id The id
     * @param fromId The form id
     */
    public ModelBpmnEntry(String type, String id, String fromId) {
        this.type = type;
        this.id = id;
        this.fromId = fromId;
    }

    /**
     * Get the type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type.
     * @param type
     *            the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the id.
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id.
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the form id.
     * @return the fromId
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * Set the form id.
     * @param fromId
     *            the fromId to set
     */
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

}
