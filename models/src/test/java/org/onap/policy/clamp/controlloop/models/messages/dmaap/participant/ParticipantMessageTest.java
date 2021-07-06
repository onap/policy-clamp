/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ParticipantMessageTest {
    private ParticipantMessage message;

    @Test
    void testCopyConstructor() {
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
    }

    @Test
    void testAppliesTo_NullParticipantId() {
        message = makeMessage();

        assertThatThrownBy(() -> message.appliesTo(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> message.appliesTo(new ToscaConceptIdentifier("PType", "4.5.6"), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> message.appliesTo(null, new ToscaConceptIdentifier("id", "1.2.3")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAppliesTo_ParticipantIdMatches() {
        message = makeMessage();

        // ParticipantId matches
        assertTrue(message.appliesTo(new ToscaConceptIdentifier("PType", "4.5.6"),
                new ToscaConceptIdentifier("id", "1.2.3")));
        assertFalse(message.appliesTo(new ToscaConceptIdentifier("PType", "4.5.6"),
                new ToscaConceptIdentifier("id", "1.2.4")));
        assertFalse(message.appliesTo(new ToscaConceptIdentifier("PType", "4.5.7"),
                new ToscaConceptIdentifier("id", "1.2.3")));
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

        msg.setParticipantType(new ToscaConceptIdentifier("PType", "4.5.6"));
        msg.setParticipantId(new ToscaConceptIdentifier("id", "1.2.3"));
        msg.setMessageId(UUID.randomUUID());
        msg.setTimestamp(Instant.ofEpochMilli(3000));

        return msg;
    }
}
