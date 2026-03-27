/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.messages.kafka.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;

class ParticipantAckMessageTest {

    @Test
    void testAppliesTo_NullParticipantId() {
        var message = makeMessage();
        var participantId = CommonTestData.getRndParticipantId();
        assertThatThrownBy(() -> message.appliesTo(participantId, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> message.appliesTo(null, participantId)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAppliesTo_ParticipantIdMatches() {
        var message = makeMessage();

        // ParticipantId matches
        assertTrue(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));
        assertFalse(message.appliesTo(CommonTestData.getRndParticipantId(), CommonTestData.getReplicaId()));
    }

    @Test
    void testAppliesTo_ParticipantIdNoMatch() {
        var message = makeMessage();

        // ParticipantId does not match
        assertFalse(message.appliesTo(CommonTestData.getRndParticipantId(), CommonTestData.getReplicaId()));
        assertTrue(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));
    }

    private ParticipantAckMessage makeMessage() {
        var msg = new ParticipantDeregisterAck();

        msg.setParticipantId(CommonTestData.getParticipantId());
        msg.setMessage("Successfull Ack");
        msg.setResult(true);
        msg.setResponseTo(UUID.randomUUID());

        return msg;
    }
}
