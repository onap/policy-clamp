/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.loop.components.external;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import org.apache.camel.Exchange;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;

public class PolicyComponent extends ExternalComponent {

    @Transient
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyComponent.class);

    public static final ExternalComponentState NOT_SENT = new ExternalComponentState("NOT_SENT",
        "The policies defined have NOT yet been created on the policy engine");
    public static final ExternalComponentState SENT = new ExternalComponentState("SENT",
        "The policies defined have been created but NOT deployed on the policy engine");
    public static final ExternalComponentState SENT_AND_DEPLOYED = new ExternalComponentState("SENT_AND_DEPLOYED",
        "The policies defined have been created and deployed on the policy engine");
    public static final ExternalComponentState IN_ERROR = new ExternalComponentState("IN_ERROR",
        "There was an error during the sending to policy, the policy engine may be corrupted or inconsistent");

    public PolicyComponent() {
        super(NOT_SENT);
    }

    @Override
    public String getComponentName() {
        return "POLICY";
    }

    /**
     * Generates the Json that must be sent to policy to add all policies to Active
     * PDP group.
     *
     * @return The json, payload to send
     */
    public static String createPoliciesPayloadPdpGroup(Loop loop) {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonObject.add("policies", jsonArray);

        for (String policyName : PolicyComponent.listPolicyNamesPdpGroup(loop)) {
            JsonObject policyNode = new JsonObject();
            jsonArray.add(policyNode);
            policyNode.addProperty("policy-id", policyName);
        }
        String payload = new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
        logger.info("PdpGroup policy payload: " + payload);
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
    }

    /**
     * Generates the list of policy names that must be send/remove to/from active
     * PDP group.
     *
     * @return A list of policy names
     */
    public static List<String> listPolicyNamesPdpGroup(Loop loop) {
        List<String> policyNamesList = new ArrayList<>();
        for (OperationalPolicy opPolicy : loop.getOperationalPolicies()) {
            policyNamesList.add(opPolicy.getName());
            for (String guardName : opPolicy.createGuardPolicyPayloads().keySet()) {
                policyNamesList.add(guardName);
            }
        }
        for (MicroServicePolicy microServicePolicy : loop.getMicroServicePolicies()) {
            policyNamesList.add(microServicePolicy.getName());
        }
        return policyNamesList;
    }

    @Override
    public ExternalComponentState computeState(Exchange camelExchange) {
        boolean oneNotFound = (boolean) camelExchange.getIn().getExchange().getProperty("atLeastOnePolicyNotFound");
        boolean oneNotDeployed = (boolean) camelExchange.getIn().getExchange()
            .getProperty("atLeastOnePolicyNotDeployed");

        if (oneNotFound && oneNotDeployed) {
            this.setState(NOT_SENT);
        } else if (!oneNotFound && oneNotDeployed) {
            this.setState(SENT);
        } else if (!oneNotFound && !oneNotDeployed) {
            this.setState(SENT_AND_DEPLOYED);
        } else {
            this.setState(IN_ERROR);
        }
        return this.getState();
    }
}
