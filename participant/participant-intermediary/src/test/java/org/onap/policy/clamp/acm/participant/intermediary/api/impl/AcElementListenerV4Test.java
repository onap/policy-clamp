/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcElementListenerV4Test {

    private AcElementListenerV4 acElementListenerV4;
    private ParticipantIntermediaryApiImpl intermediaryApi;

    @BeforeEach
    void setup() {
        intermediaryApi = mock(ParticipantIntermediaryApiImpl.class);
        acElementListenerV4 = new AcElementListenerV4(intermediaryApi) {
            @Override
            public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
                throws PfModelException {
                // do nothing
            }

            @Override
            public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
                throws PfModelException {
                // do nothing
            }
        };
    }

    @Test
    void testPrepare() throws PfModelException {
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), Map.of(), Map.of());
        acElementListenerV4.prepare(compositionElement, instanceElement, 1);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), DeployState.UNDEPLOYED, null,
            StateChangeResult.NO_ERROR, "Prepare completed");
    }
}
