/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2023,2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.participant.policy.client;

import org.onap.policy.clamp.acm.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.stereotype.Component;

@Component
public class PolicyApiHttpClient extends AbstractHttpClient {

    private static final String POLICY_URI = "/policy/api/v1/";

    /**
     * Constructor.
     *
     * @param parameters the policy participant parameters
     */
    public PolicyApiHttpClient(ParticipantPolicyParameters parameters) {
        super(parameters.getPolicyApiParameters());
    }

    /**
     * Create Policy Types.
     *
     * @param toscaServiceTemplate the whole ToscaServiceTemplate
     * @return Response
     */
    public String createPolicyType(ToscaServiceTemplate toscaServiceTemplate) {
        return executePost(POLICY_URI + "policytypes", toscaServiceTemplate);
    }

    /**
     * Create Policies.
     *
     * @param toscaServiceTemplate the whole ToscaServiceTemplate
     * @return Response
     */
    public String createPolicy(final ToscaServiceTemplate toscaServiceTemplate) {
        return executePost(POLICY_URI + "policies", toscaServiceTemplate);
    }

    /**
     * Delete Policies.
     *
     * @param policyName the name of the policy to be deleted
     * @param policyVersion the version of the policy to be deleted
     */
    public void deletePolicy(final String policyName, final String policyVersion) {
        executeDelete(POLICY_URI + "policies/" + policyName + "/versions/" + policyVersion);
    }

    /**
     * Delete Policy types.
     *
     * @param policyTypeName the name of the policy to be deleted
     * @param policyTypeVersion the version of the policy to be deleted
     */
    public void deletePolicyType(final String policyTypeName, final String policyTypeVersion) {
        executeDelete(POLICY_URI + "policytypes/" + policyTypeName + "/versions/" + policyTypeVersion);
    }
}
