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
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parse ONAP Tca json properties.
 *
 */
public class Tca extends AbstractModelElement {

    protected static final EELFLogger logger      = EELFManager.getInstance().getLogger(Tca.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private TcaItem                   tcaItem;

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
            tcaItem = new TcaItem(modelElementJsonNode.elements().next());
        }
    }

    public TcaItem getTcaItem() {
        return tcaItem;
    }

    public static final String getType() {
        return TYPE_TCA;
    }

}
