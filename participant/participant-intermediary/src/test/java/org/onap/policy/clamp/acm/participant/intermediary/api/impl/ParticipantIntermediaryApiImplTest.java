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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionHandler;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.common.utils.coder.CoderException;

class ParticipantIntermediaryApiImplTest {

    private static final String USE_STATE = "useState";
    private static final String OPERATIONAL_STATE = "operationState";
    private static final Map<String, Object> MAP = Map.of("key", 1);

    @Test
    void mockParticipantIntermediaryApiImplTest() throws CoderException {
        var automationComposiitonHandler = mock(AutomationCompositionHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler);

        var acElementListener = mock(AutomationCompositionElementListener.class);
        apiImpl.registerAutomationCompositionElementListener(acElementListener);
        verify(automationComposiitonHandler).registerAutomationCompositionElementListener(acElementListener);

        var uuid = UUID.randomUUID();
        var automationCompositionId = UUID.randomUUID();
        apiImpl.updateAutomationCompositionElementState(automationCompositionId, uuid, DeployState.UNDEPLOYED,
                LockState.NONE, null);
        verify(automationComposiitonHandler).updateAutomationCompositionElementState(automationCompositionId, uuid,
                DeployState.UNDEPLOYED, LockState.NONE, null);

        apiImpl.sendAcElementInfo(automationCompositionId, uuid, USE_STATE, OPERATIONAL_STATE, MAP);
        verify(automationComposiitonHandler).sendAcElementInfo(automationCompositionId, uuid, USE_STATE,
                OPERATIONAL_STATE, MAP);
    }
}
