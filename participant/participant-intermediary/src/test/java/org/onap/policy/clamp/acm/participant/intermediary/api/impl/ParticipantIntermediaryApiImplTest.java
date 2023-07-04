/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionOutHandler;
import org.onap.policy.clamp.acm.participant.intermediary.handler.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.CoderException;

class ParticipantIntermediaryApiImplTest {

    private static final String USE_STATE = "useState";
    private static final String OPERATIONAL_STATE = "operationState";
    private static final Map<String, Object> MAP = Map.of("key", 1);

    @Test
    void mockParticipantIntermediaryApiImplTest() throws CoderException {
        var automationComposiitonHandler = mock(AutomationCompositionOutHandler.class);
        var cacheProvider = mock(CacheProvider.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler, cacheProvider);

        var uuid = UUID.randomUUID();
        var automationCompositionId = UUID.randomUUID();
        apiImpl.updateAutomationCompositionElementState(automationCompositionId, uuid, DeployState.UNDEPLOYED,
                LockState.NONE, StateChangeResult.NO_ERROR, null);
        verify(automationComposiitonHandler).updateAutomationCompositionElementState(automationCompositionId, uuid,
                DeployState.UNDEPLOYED, LockState.NONE, StateChangeResult.NO_ERROR, null);

        apiImpl.sendAcElementInfo(automationCompositionId, uuid, USE_STATE, OPERATIONAL_STATE, MAP);
        verify(automationComposiitonHandler).sendAcElementInfo(automationCompositionId, uuid, USE_STATE,
                OPERATIONAL_STATE, MAP);

        apiImpl.updateAutomationCompositionElementState(automationCompositionId, uuid, DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "");
        verify(automationComposiitonHandler).updateAutomationCompositionElementState(automationCompositionId, uuid,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "");

        var map = Map.of(automationCompositionId, new AutomationComposition());
        when(cacheProvider.getAutomationCompositions()).thenReturn(map);
        var acElement = new AutomationCompositionElement();
        acElement.setId(uuid);
        map.get(automationCompositionId).setElements(Map.of(uuid, acElement));

        var result = apiImpl.getAutomationCompositions();
        assertEquals(map, result);

        var element = apiImpl.getAutomationCompositionElement(UUID.randomUUID(), UUID.randomUUID());
        assertThat(element).isNull();

        element = apiImpl.getAutomationCompositionElement(automationCompositionId, UUID.randomUUID());
        assertThat(element).isNull();

        element = apiImpl.getAutomationCompositionElement(automationCompositionId, uuid);
        assertEquals(acElement, element);

        apiImpl.updateCompositionState(uuid, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "");
        verify(automationComposiitonHandler).updateCompositionState(uuid, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "");
    }
}
