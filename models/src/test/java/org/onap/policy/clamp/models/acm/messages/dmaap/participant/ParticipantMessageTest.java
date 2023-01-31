/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.assertSerializable;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;

class ParticipantMessageTest {
    private ParticipantMessage message;

    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new ParticipantMessage((ParticipantMessage) null))
                .isInstanceOf(NullPointerException.class);

        // verify with null values
        message = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);
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
        message = makeMessage();

        assertThatThrownBy(() -> message.appliesTo(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAppliesTo_ParticipantIdMatches() {
        message = makeMessage();

        // ParticipantId matches
        assertTrue(message.appliesTo(CommonTestData.getParticipantId()));
        assertFalse(message.appliesTo(CommonTestData.getRndParticipantId()));
    }

    @Test
    void testAppliesTo_ParticipantIdNoMatch() {
        message = makeMessage();
        assertFalse(message.appliesTo(CommonTestData.getRndParticipantId()));
        assertTrue(message.appliesTo(CommonTestData.getParticipantId()));
    }

    private ParticipantMessage makeMessage() {
        var msg = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);

        msg.setParticipantId(CommonTestData.getParticipantId());
        msg.setMessageId(UUID.randomUUID());
        msg.setTimestamp(Instant.ofEpochMilli(3000));

        return msg;
    }
}
