/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageUtils.assertSerializable;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;

class ParticipantMessageTest {

    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new ParticipantMessage((ParticipantMessage) null))
                .isInstanceOf(NullPointerException.class);

        // verify with null values
        var message = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);
        var newmsg = new ParticipantMessage(message);
        newmsg.setMessageId(message.getMessageId());
        newmsg.setTimestamp(message.getTimestamp());
        assertEquals(message.toString(), newmsg.toString());

        // verify with all values
        message = makeMessage();
        newmsg = new ParticipantMessage(message);
        newmsg.setMessageId(message.getMessageId());
        newmsg.setTimestamp(message.getTimestamp());
        assertEquals(message.toString(), newmsg.toString());

        assertSerializable(message, ParticipantMessage.class);
    }

    @Test
    void testAppliesTo_NullParticipantId() {
        var message = makeMessage();
        var participantId = CommonTestData.getParticipantId();
        assertThatThrownBy(() -> message.appliesTo(participantId, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> message.appliesTo(null, participantId)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAppliesTo_ParticipantIdMatches() {
        var message = makeMessage();

        // ParticipantId matches
        assertTrue(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));

        message.setReplicaId(CommonTestData.getReplicaId());
        assertTrue(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));

        message.setParticipantIdList(Set.of());
        assertTrue(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));

        message.setParticipantIdList(Set.of(CommonTestData.getParticipantId()));
        assertTrue(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));

        message.setParticipantId(null);
        assertTrue(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));
    }

    @Test
    void testAppliesTo_ParticipantIdNoMatch() {
        var message = makeMessage();
        assertFalse(message.appliesTo(CommonTestData.getRndParticipantId(), CommonTestData.getReplicaId()));

        message.setReplicaId(CommonTestData.getReplicaId());
        assertFalse(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getRndParticipantId()));

        message.setReplicaId(null);
        message.setParticipantId(null);
        message.setParticipantIdList(
                Set.of(CommonTestData.getRndParticipantId(), CommonTestData.getRndParticipantId()));
        assertFalse(message.appliesTo(CommonTestData.getParticipantId(), CommonTestData.getReplicaId()));
    }

    private ParticipantMessage makeMessage() {
        var msg = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);

        msg.setParticipantId(CommonTestData.getParticipantId());
        msg.setMessageId(UUID.randomUUID());
        msg.setTimestamp(Instant.ofEpochMilli(3000));

        return msg;
    }
}
