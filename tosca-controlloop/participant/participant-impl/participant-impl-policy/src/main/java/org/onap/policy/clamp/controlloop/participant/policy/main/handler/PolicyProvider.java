/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.policy.main.handler;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryFactory;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;

/**
 * Provider class for policy participant.
 */
public class PolicyProvider implements Closeable {
    @Getter
    private final ParticipantIntermediaryApi intermediaryApi;

    private final ControlLoopElementHandler controlLoopElementHandler;

    /**
     * Create a policy participant provider.
     *
     * @throws ControlLoopRuntimeException on errors creating the provider
     */
    public PolicyProvider(ParticipantIntermediaryParameters participantParameters)
                     throws ControlLoopRuntimeException {
        intermediaryApi = new ParticipantIntermediaryFactory().createApiImplementation();
        intermediaryApi.init(participantParameters);
        controlLoopElementHandler = new ControlLoopElementHandler();
        intermediaryApi.registerControlLoopElementListener(controlLoopElementHandler);
    }

    @Override
    public void close() throws IOException {
        intermediaryApi.close();
    }
}
