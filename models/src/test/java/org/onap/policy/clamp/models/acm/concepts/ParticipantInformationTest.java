/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ParticipantInformationTest {

    @Test
    void testCopyConstructor() {
        var participant = new Participant();
        participant.setParticipantId(UUID.randomUUID());
        participant.setParticipantState(ParticipantState.ON_LINE);
        participant.setParticipantSupportedElementTypes(new HashMap<>());
        var participantInfo1 = new ParticipantInformation();
        participantInfo1.setParticipant(participant);
        participantInfo1.setAcElementInstanceMap(new HashMap<>());
        participantInfo1.setAcNodeTemplateStateDefinitionMap(new HashMap<>());

        var participantInfo2 = new ParticipantInformation(participantInfo1);
        assertEquals(participantInfo1, participantInfo2);
    }
}
