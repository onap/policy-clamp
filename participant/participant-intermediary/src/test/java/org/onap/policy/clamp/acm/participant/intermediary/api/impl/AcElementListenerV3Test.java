/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcElementListenerV3Test {

    @Test
    void prepareTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV3(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), Map.of(), Map.of());
        acElementListener.prepare(compositionElement, instanceElement, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null,
                StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    private AcElementListenerV3 createAcElementListenerV3(ParticipantIntermediaryApi intermediaryApi) {
        return new AcElementListenerV3(intermediaryApi) {
            @Override
            public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
                // dummy implementation
            }

            @Override
            public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
                // dummy implementation
            }
        };
    }
}
