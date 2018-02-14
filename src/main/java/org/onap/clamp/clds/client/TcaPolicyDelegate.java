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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.client.req.tca.TcaRequestFormatter;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Tca;
import org.onap.clamp.clds.model.refprop.RefProp;
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
    private RefProp refProp;
    @Autowired
    private PolicyClient policyClient;

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
        }
    }
}
