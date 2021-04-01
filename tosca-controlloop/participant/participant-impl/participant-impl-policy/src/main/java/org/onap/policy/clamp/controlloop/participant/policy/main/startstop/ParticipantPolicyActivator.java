/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.policy.main.startstop;

import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.participant.policy.main.handler.PolicyHandler;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.common.utils.services.ServiceManagerContainer;

/**
 * This class activates the policy participant.
 *
 */
public class ParticipantPolicyActivator extends ServiceManagerContainer {
    @Getter
    private final ParticipantPolicyParameters parameters;

    /**
     * Instantiate the activator for the policy participant.
     *
     * @param parameters the parameters for the policy participant
     */
    public ParticipantPolicyActivator(final ParticipantPolicyParameters parameters) {
        this.parameters = parameters;

        final AtomicReference<PolicyHandler> policyHandler = new AtomicReference<>();

        // @formatter:off
        addAction("Policy Handler",
            () -> policyHandler.set(new PolicyHandler(parameters)),
            () -> policyHandler.get().close());

        addAction("Policy Providers",
            () -> policyHandler.get().startProviders(),
            () -> policyHandler.get().stopProviders());
        // @formatter:on
    }
}
