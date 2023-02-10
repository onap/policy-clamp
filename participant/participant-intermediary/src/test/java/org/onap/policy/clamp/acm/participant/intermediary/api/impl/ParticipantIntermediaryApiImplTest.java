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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.common.utils.coder.CoderException;

class ParticipantIntermediaryApiImplTest {

    private final CommonTestData commonTestData = new CommonTestData();

    @Test
    void mockParticipantIntermediaryApiImplTest() throws CoderException {
        var uuid = UUID.randomUUID();
        var definition = CommonTestData.getDefinition();
        var participantId = CommonTestData.getParticipantId();
        var automationComposiitonHandler =
                commonTestData.setTestAutomationCompositionHandler(definition, uuid, participantId);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler);
        var acElementListener = Mockito.mock(AutomationCompositionElementListener.class);
        apiImpl.registerAutomationCompositionElementListener(acElementListener);

        apiImpl.updateAutomationCompositionElementState(UUID.randomUUID(), uuid, DeployState.UNDEPLOYED,
                LockState.NONE);
        var acElement = automationComposiitonHandler.getElementsOnThisParticipant().get(uuid);
        assertEquals(DeployState.UNDEPLOYED, acElement.getDeployState());
        assertEquals(uuid, acElement.getId());
    }
}
