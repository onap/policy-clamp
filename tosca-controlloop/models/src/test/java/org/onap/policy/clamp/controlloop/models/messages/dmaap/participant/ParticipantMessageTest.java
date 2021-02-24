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
import org.junit.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class ParticipantMessageTest {
    private ParticipantMessage message;

    @Test
    public void testCopyConstructor() {
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
    public void testAppliesTo_NullParticipantId() {
        message = makeMessage();

        assertThatThrownBy(() -> message.appliesTo(null))
                        .isInstanceOf(NullPointerException.class);

    }

    @Test
    public void testAppliesTo_ParticipantIdMatches() {
        message = makeMessage();

        // ParticipantId matches
        ToscaConceptIdentifier id = new ToscaConceptIdentifier();
        id.setName("id");
        id.setVersion("1.2.3");
        assertTrue(message.appliesTo(id));
    }

    @Test
    public void testAppliesTo_ParticipantIdNoMatch() {
        message = makeMessage();

        // ParticipantId doesnot match
        ToscaConceptIdentifier id = new ToscaConceptIdentifier();
        id.setName("id1111");
        id.setVersion("3.2.1");
        assertFalse(message.appliesTo(id));
        message.setParticipantId(null);
        assertTrue(message.appliesTo(id));
    }

    private ParticipantMessage makeMessage() {
        ParticipantMessage msg = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);

        ToscaConceptIdentifier id = new ToscaConceptIdentifier();
        id.setName("id");
        id.setVersion("1.2.3");
        msg.setControlLoopId(id);
        msg.setParticipantId(id);
        msg.setMessageId(UUID.randomUUID());
        msg.setTimestamp(Instant.ofEpochMilli(3000));

        return msg;
    }
}
