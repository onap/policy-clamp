/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.loop.components.external;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import javax.persistence.Transient;
import org.apache.camel.Exchange;
import org.onap.policy.clamp.loop.Loop;
import org.onap.policy.clamp.policy.pdpgroup.PdpGroupPayload;

/**
 * This class represents the policy state according to all policies involved in the control loop.
 * It can compute it with all policy queries result.
 * It contains also the method to generate the PDP payload used for the policies deployment.
 */
public class PolicyComponent extends ExternalComponent {

    @Transient
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyComponent.class);

    public static final ExternalComponentState IN_ERROR = new ExternalComponentState("IN_ERROR",
            "There was an error during the sending to policy, the policy engine may be corrupted or inconsistent", 100);
    public static final ExternalComponentState NOT_SENT = new ExternalComponentState("NOT_SENT",
            "The policies defined have NOT yet been created on the policy engine", 90);
    public static final ExternalComponentState SENT = new ExternalComponentState("SENT",
            "The policies defined have been created but NOT deployed on the policy engine", 50);
    public static final ExternalComponentState SENT_AND_DEPLOYED = new ExternalComponentState("SENT_AND_DEPLOYED",
            "The policies defined have been created and deployed on the policy engine", 10);
    public static final ExternalComponentState UNKNOWN = new ExternalComponentState("UNKNOWN",
            "The current status is not clear. Need to refresh the status to get the current status.", 0);

    /**
     * Default constructor.
     */
    public PolicyComponent() {
        /*
         * We assume it's good by default as we will receive the state for each policy
         * on by one, each time we increase the level we can't decrease it anymore.
         * That's why it starts with the lowest one SENT_AND_DEPLOYED.
         */
        super(UNKNOWN);
    }

    @Override
    public String getComponentName() {
        return "POLICY";
    }

    /**
     * Generates the Json that must be sent to policy to add all policies to Active
     * PDP group.
     *
     * @param loop   the loop object
     * @param action POST (to add policy to group) or DELETE (to delete policy from group)
     * @return The json, payload to send
     */
    public static String createPoliciesPayloadPdpGroup(Loop loop, String action) {
        PdpGroupPayload pdpGroupPayload = new PdpGroupPayload();
        loop.getOperationalPolicies().stream().forEach(opPolicy -> pdpGroupPayload
                .updatePdpGroupMap(opPolicy.getPdpGroup(), opPolicy.getPdpSubgroup(), opPolicy.getName(), "1.0.0",
                        action));

        loop.getMicroServicePolicies().stream().forEach(msPolicy -> pdpGroupPayload
                .updatePdpGroupMap(msPolicy.getPdpGroup(), msPolicy.getPdpSubgroup(), msPolicy.getName(), "1.0.0",
                        action));
        return pdpGroupPayload.generatePdpGroupPayload();
    }

    private static ExternalComponentState findNewState(boolean found, boolean deployed) {

        ExternalComponentState newState = NOT_SENT;
        if (found && deployed) {
            newState = SENT_AND_DEPLOYED;
        } else {
            if (found) {
                newState = SENT;
            } else {
                if (deployed) {
                    newState = IN_ERROR;
                }
            }
        }
        return newState;
    }

    private static ExternalComponentState mergeStates(ExternalComponentState oldState,
                                                      ExternalComponentState newState) {
        return (oldState.compareTo(newState) < 0) ? newState : oldState;
    }

    /**
     * This is a method that expect the results of the queries getPolicy and
     * getPolicyDeployed for a unique policy (op, config, etc ...). It
     * re-computes the global policy state for each policy results given. Therefore
     * this method is called multiple times from the camel route and must be reset
     * for a new global policy state retrieval. The state to compute the global
     * policy state is stored in this class.
     */
    @Override
    public ExternalComponentState computeState(Exchange camelExchange) {
        this.setState(mergeStates(this.getState(),
                findNewState((boolean) camelExchange.getIn().getExchange().getProperty("policyFound"),
                        (boolean) camelExchange.getIn().getExchange().getProperty("policyDeployed"))));
        return this.getState();
    }
}
