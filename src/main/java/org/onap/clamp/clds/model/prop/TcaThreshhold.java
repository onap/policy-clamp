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

/**
 * Parse Tca Threshhold json properties.
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
public class TcaThreshhold {

    protected static final EELFLogger       logger      = EELFManager.getInstance().getLogger(TcaThreshhold.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                  metric;
    private String                  fieldPath;
    private String                  operator;
    private Integer                 threshhold;

    /**
     * Parse Tca Threshhold given json node
     *
     * @param node
     */
    public TcaThreshhold(JsonNode node) {

        if (node.get(0) != null) {
            metric = node.get(0).asText();
        }
        if (node.get(1) != null) {
            operator = node.get(1).asText();
        }
        if (node.get(2) != null) {
            threshhold = Integer.valueOf(node.get(2).asText());
        }
        if (node.get(3) != null) {
            fieldPath = node.get(3).asText();
        }
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
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

    public Integer getThreshhold() {
        return threshhold;
    }

    public void setThreshhold(Integer threshhold) {
        this.threshhold = threshhold;
    }

}
