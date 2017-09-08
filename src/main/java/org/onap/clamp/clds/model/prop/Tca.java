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
 * Parse Tca json properties.
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
public class Tca extends AbstractModelElement {

    protected static final EELFLogger logger      = EELFManager.getInstance().getLogger(Tca.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private List<TcaItem>             tcaItems;

    private static final String       TYPE_TCA    = "tca";

    /**
     * Parse Tca given json node
     *
     * @param modelProp
     * @param modelBpmn
     * @param modelJson
     */
    public Tca(ModelProperties modelProp, ModelBpmn modelBpmn, JsonNode modelJson) {
        super(TYPE_TCA, modelProp, modelBpmn, modelJson);

        // process Server_Configurations
        if (modelElementJsonNode != null) {
            Iterator<JsonNode> itr = modelElementJsonNode.elements();
            tcaItems = new ArrayList<>();
            while (itr.hasNext()) {
                tcaItems.add(new TcaItem(itr.next()));
            }
        }
    }

    public List<TcaItem> getTcaItems() {
        return tcaItems;
    }

    public static final String getType() {
        return TYPE_TCA;
    }

}
