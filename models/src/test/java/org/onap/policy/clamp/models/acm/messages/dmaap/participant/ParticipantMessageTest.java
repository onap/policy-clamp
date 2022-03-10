/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ParticipantMessageTest {
    private ParticipantMessage message;

    private static final ToscaConceptIdentifier PTYPE_456 = new ToscaConceptIdentifier("PType", "4.5.6");
    private static final ToscaConceptIdentifier PTYPE_457 = new ToscaConceptIdentifier("PType", "4.5.7");
    private static final ToscaConceptIdentifier ID_123 = new ToscaConceptIdentifier("id", "1.2.3");
    private static final ToscaConceptIdentifier ID_124 = new ToscaConceptIdentifier("id", "1.2.4");

    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new ParticipantMessage((ParticipantMessage) null))
                .isInstanceOf(NullPointerException.class);

        // verify with null values
        message = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);
        ParticipantMessage newmsg = new ParticipantMessage(message);
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

        assertThatThrownBy(() -> message.appliesTo(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> message.appliesTo(PTYPE_456, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> message.appliesTo(null, ID_123)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAppliesTo_ParticipantIdMatches() {
        message = makeMessage();

        // ParticipantId matches
        assertTrue(message.appliesTo(PTYPE_456, ID_123));
        assertFalse(message.appliesTo(PTYPE_456, ID_124));
        assertFalse(message.appliesTo(PTYPE_457, ID_123));
    }

    @Test
    void testAppliesTo_ParticipantIdNoMatch() {
        message = makeMessage();

        // ParticipantId does not match
        ToscaConceptIdentifier id = new ToscaConceptIdentifier();
        id.setName("id1111");
        id.setVersion("3.2.1");
        assertFalse(message.appliesTo(id, id));
        message.setParticipantType(null);
        assertTrue(message.appliesTo(id, id));
    }

    private ParticipantMessage makeMessage() {
        ParticipantMessage msg = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);

        msg.setParticipantType(PTYPE_456);
        msg.setParticipantId(ID_123);
        msg.setMessageId(UUID.randomUUID());
        msg.setTimestamp(Instant.ofEpochMilli(3000));

        return msg;
    }
}
