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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.onap.clamp.clds.client.req.policy.OperationalPolicyReq;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Policy;
import org.onap.clamp.clds.model.prop.PolicyChain;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.policy.api.AttributeType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Send Operational Policy info to policy api. It uses the policy code to define
 * the model and communicate with it. See also the PolicyClient class.
 *
 */
public class OperationalPolicyDelegate implements JavaDelegate {
    protected static final EELFLogger logger        = EELFManager.getInstance()
            .getLogger(OperationalPolicyDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    /**
     * Automatically injected by Spring, define in CldsConfiguration as a bean.
     */
    @Autowired
    private PolicyClient              policyClient;
    /**
     * Automatically injected by Spring, define in CldsConfiguration as a bean.
     */
    @Autowired
    private RefProp                   refProp;

    /**
     * Perform activity. Send Operational Policy info to policy api.
     *
     * @param execution
     *            The DelegateExecution
     * @throws BuilderException
     *             In case of issues with OperationalPolicyReq
     * @throws UnsupportedEncodingException
     */
    @Override
    public void execute(DelegateExecution execution)
            throws BuilderException, UnsupportedEncodingException {
        String responseMessage = null;
        ModelProperties prop = ModelProperties.create(execution);
        Policy policy = prop.getType(Policy.class);
        if (policy.isFound()) {
            for (PolicyChain policyChain : prop.getType(Policy.class).getPolicyChains()) {
                String operationalPolicyRequestUuid = LoggingUtils.getRequestId();
                Map<AttributeType, Map<String, String>> attributes = OperationalPolicyReq.formatAttributes(refProp,
                        prop, prop.getType(Policy.class).getId(), policyChain);
                responseMessage = policyClient.sendBrmsPolicy(attributes, prop, operationalPolicyRequestUuid);
            }
            if (responseMessage != null) {
                execution.setVariable("operationalPolicyResponseMessage", responseMessage.getBytes());
            }
        }
    }
}
