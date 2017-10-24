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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parse ONAP Tca Item json properties.
 *
 */
public class TcaItem {

    protected static final EELFLogger logger      = EELFManager.getInstance().getLogger(TcaItem.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                    tcaName;
    private String                    tcaUuId;
    private String                    policyId;
    private String                    eventName;
    private String                    controlLoopSchemaType;
    private List<TcaThreshold>        tcaThresholds;

    /**
     * Parse Tca Item given json node
     *
     * @param node
     */
    public TcaItem(JsonNode node) {

        tcaName = AbstractModelElement.getValueByName(node, "tname");
        tcaUuId = AbstractModelElement.getValueByName(node, "tuuid");
        policyId = AbstractModelElement.getValueByName(node, "tcaPolId");
        eventName = AbstractModelElement.getValueByName(node, "eventName");
        controlLoopSchemaType = AbstractModelElement.getValueByName(node, "controlLoopSchemaType");
        // process service Configurations
        JsonNode serviceConfigurationsNode = node.get(node.size() - 1).get("serviceConfigurations");
        Iterator<JsonNode> itr = serviceConfigurationsNode.elements();
        tcaThresholds = new ArrayList<>();
        while (itr.hasNext()) {
            tcaThresholds.add(new TcaThreshold(itr.next()));
        }
    }

    public String getControlLoopSchemaType() {
        return controlLoopSchemaType;
    }

    public void setControlLoopSchemaType(String controlLoopSchemaType) {
        this.controlLoopSchemaType = controlLoopSchemaType;
    }

    public String getTcaName() {
        return tcaName;
    }

    public void setTcaName(String tcaName) {
        this.tcaName = tcaName;
    }

    public String getTcaUuId() {
        return tcaUuId;
    }

    public void setTcaUuId(String tcaUuId) {
        this.tcaUuId = tcaUuId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public List<TcaThreshold> getTcaThresholds() {
        return tcaThresholds;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

}
