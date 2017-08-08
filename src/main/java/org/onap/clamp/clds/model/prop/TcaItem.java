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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parse Tca Item json properties.
 *
 * Example json:
 * {"TCA_0lm6cix":{"Narra":[{"name":"tname","value":"Narra"},{"name":"tcaEnab",
 * "value":"on"},{"name":"tcaPol","value":"Polcicy1"},{"name":"tcaPolId","value"
 * :"1"},{"name":"tcaInt","value":"1"},{"name":"tcaSev","value":"Critical"},{
 * "name":"tcaVio","value":"1"},{"serviceConfigurations":[["FIELDPATH_test_1",
 * ">","4"],["FIELDPATH_test_1","=","5"]]}],"Srini":[{"name":"tname","value":
 * "Srini"},{"name":"tcaEnab","value":"on"},{"name":"tcaPol","value":"Policy1"},
 * {"name":"tcaPolId","value":"1"},{"name":"tcaInt","value":"1"},{"name":
 * "tcaSev","value":"Major"},{"name":"tcaVio","value":"1"},{
 * "serviceConfigurations":[["FIELDPATH_test_2","=","3"],["FIELDPATH_test_1",">"
 * ,"2"]]}]}}
 *
 *
 */
public class TcaItem {

    protected static final EELFLogger       logger      = EELFManager.getInstance().getLogger(TcaItem.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                  tcaName;
    private String                  tcaUuId;
    private String                  nfNamingCode;
    private String                  tcaEnable;
    private String                  policyId;
    private Integer                 interval;
    private String                  severity;
    private Integer                 violations;
    private List<TcaThreshhold>     tcaThreshholds;

    /**
     * Parse Tca Item given json node
     *
     * @param node
     */
    public TcaItem(JsonNode node) {

        tcaName = ModelElement.getValueByName(node, "tname");
        tcaUuId = ModelElement.getValueByName(node, "tuuid");
        nfNamingCode = ModelElement.getValueByName(node, "tnfc");
        tcaEnable = ModelElement.getValueByName(node, "tcaEnab");
        policyId = ModelElement.getValueByName(node, "tcaPolId");
        if (ModelElement.getValueByName(node, "tcaInt") != null) {
            interval = Integer.valueOf(ModelElement.getValueByName(node, "tcaInt"));
        }
        severity = ModelElement.getValueByName(node, "tcaSev");
        if (ModelElement.getValueByName(node, "tcaVio") != null) {
            violations = Integer.valueOf(ModelElement.getValueByName(node, "tcaVio"));
        }

        // process service Configurations
        JsonNode serviceConfigurationsNode = node.get(node.size() - 1).get("serviceConfigurations");
        Iterator<JsonNode> itr = serviceConfigurationsNode.elements();
        tcaThreshholds = new ArrayList<TcaThreshhold>();
        while (itr.hasNext()) {
            tcaThreshholds.add(new TcaThreshhold(itr.next()));
        }
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

    public String getNfNamingCode() {
        return nfNamingCode;
    }

    public void setNfNamingCode(String nfNamingCode) {
        this.nfNamingCode = nfNamingCode;
    }

    public String getTcaEnable() {
        return tcaEnable;
    }

    public void setTcaEnable(String tcaEnable) {
        this.tcaEnable = tcaEnable;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Integer getViolations() {
        return violations;
    }

    public void setViolations(Integer violations) {
        this.violations = violations;
    }

    public List<TcaThreshhold> getTcaThreshholds() {
        return tcaThreshholds;
    }

}
