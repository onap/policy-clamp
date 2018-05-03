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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.client.req.tca.TcaRequestFormatter;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Tca;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Send Tca info to policy api.
 */
@Component
public class TcaPolicyDelegate {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(TcaPolicyDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    @Autowired
    private ClampProperties refProp;
    @Autowired
    private PolicyClient policyClient;
    @Autowired
    private CldsDao cldsDao;

    /**
     * Perform activity. Send Tca info to policy api.
     *
     * @param camelExchange
     *            The Camel Exchange object containing the properties
     */
    @Handler
    public void execute(Exchange camelExchange) {
        String tcaPolicyRequestUuid = UUID.randomUUID().toString();
        camelExchange.setProperty("tcaPolicyRequestUuid", tcaPolicyRequestUuid);
        ModelProperties prop = ModelProperties.create(camelExchange);
        Tca tca = prop.getType(Tca.class);
        if (tca.isFound()) {
            String policyJson = TcaRequestFormatter.createPolicyJson(refProp, prop);
            String responseMessage = policyClient.sendMicroServiceInOther(policyJson, prop);
            if (responseMessage != null) {
                camelExchange.setProperty("tcaPolicyResponseMessage", responseMessage.getBytes());
            }
            CldsModel cldsModel = CldsModel.retrieve(cldsDao, (String) camelExchange.getProperty("modelName"), false);
            cldsModel.setPropText(cldsModel.getPropText().replaceAll("AUTO_GENERATED_POLICY_ID_AT_SUBMIT",
                    prop.getPolicyNameForDcaeDeploy(refProp)));
            cldsModel.save(cldsDao, (String) camelExchange.getProperty("userid"));
        }
    }
}
