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

package org.onap.clamp.clds.client;

import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.onap.clamp.clds.client.req.TcaMPolicyReq;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Tca;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Send Tca info to policy api.
 *
 *
 */
public class TcaPolicyDelegate implements JavaDelegate {
    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(TcaPolicyDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private RefProp                 refProp;

    @Autowired
    PolicyClient                    policyClient;

    /**
     * Perform activity. Send Tca info to policy api.
     *
     * @param execution
     */
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String tcaPolicyRequestUuid = UUID.randomUUID().toString();
        execution.setVariable("tcaPolicyRequestUuid", tcaPolicyRequestUuid);

        ModelProperties prop = ModelProperties.create(execution);
        Tca tca = prop.getType(Tca.class);
        if (tca.isFound()) {
            String policyJson = TcaMPolicyReq.formatTca(refProp, prop);
            String responseMessage = policyClient.sendMicroServiceInJson(policyJson, prop, tcaPolicyRequestUuid);
            if (responseMessage != null) {
                execution.setVariable("tcaPolicyResponseMessage", responseMessage.getBytes());
            }
        }
    }

}
