/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.onap.clamp.clds.config.ClampProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The class implements the communication with the Policy Engine to retrieve
 * policy models (tosca). It mainly delegates the physical calls to Camel
 * engine.
 *
 */
@Component
public class PolicyEngineServices {
    private final CamelContext camelContext;

    private final ClampProperties refProp;

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyEngineServices.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    public static final String POLICY_RETRY_INTERVAL = "policy.retry.interval";
    public static final String POLICY_RETRY_LIMIT = "policy.retry.limit";

    @Autowired
    public PolicyEngineServices(CamelContext camelContext, ClampProperties refProp) {
        this.refProp = refProp;
        this.camelContext = camelContext;
    }

    private void downloadAllPolicies() {
        /*
         * Exchange myCamelExchange = ExchangeBuilder.anExchange(camelContext)
         * .withProperty("blueprintResourceId",
         * resourceUuid).withProperty("blueprintServiceId", serviceUuid)
         * .withProperty("blueprintName", artifactName).build();
         * metricsLogger.info("Attempt n°" + i + " to contact DCAE inventory");
         * 
         * Exchange exchangeResponse =
         * camelContext.createProducerTemplate().send("direct:get-all-policy-models",
         * myCamelExchange);
         */
    }

    /**
     * This method can be used to download a policy tosca model on the engine.
     * 
     * @param policyType    The policy type (id)
     * @param policyVersion The policy version
     * @return A string with the whole policy tosca model
     * @throws InterruptedException in case of issue when sleeping during the retry
     */
    public String downloadOnePolicy(String policyType, String policyVersion) throws InterruptedException {
        int retryInterval = 0;
        int retryLimit = 1;
        if (refProp.getStringValue(POLICY_RETRY_LIMIT) != null) {
            retryLimit = Integer.valueOf(refProp.getStringValue(POLICY_RETRY_LIMIT));
        }
        if (refProp.getStringValue(POLICY_RETRY_INTERVAL) != null) {
            retryInterval = Integer.valueOf(refProp.getStringValue(POLICY_RETRY_INTERVAL));
        }
        for (int i = 0; i < retryLimit; i++) {
            Exchange paramExchange = ExchangeBuilder.anExchange(camelContext)
                    .withProperty("policyModelName", policyType).withProperty("policyModelVersion", policyVersion)
                    .build();

            Exchange exchangeResponse = camelContext.createProducerTemplate().send("direct:get-policy-model",
                    paramExchange);

            if (Integer.valueOf(200).equals(exchangeResponse.getIn().getHeader("CamelHttpResponseCode"))) {
                return (String) exchangeResponse.getIn().getBody();
            } else {
                logger.info("Policy " + retryInterval + "ms before retrying ...");
                // wait for a while and try to connect to DCAE again
                Thread.sleep(retryInterval);
            }
        }
        return "";
    }

}
