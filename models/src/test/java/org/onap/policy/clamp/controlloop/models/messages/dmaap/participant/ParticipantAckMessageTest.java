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

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParticipantAckMessageTest {
    private ParticipantAckMessage message;

    @Test
    void testCopyConstructor() {
        assertThatThrownBy(() -> new ParticipantAckMessage((ParticipantAckMessage) null))
                .isInstanceOf(NullPointerException.class);

        // verify with null values
        message = new ParticipantAckMessage(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);
        ParticipantAckMessage newmsg = new ParticipantAckMessage(message);
        newmsg.setResponseTo(message.getResponseTo());
        assertEquals(message.toString(), newmsg.toString());

        // verify with all values
        message = makeMessage();
        newmsg = new ParticipantAckMessage(message);
        newmsg.setResponseTo(message.getResponseTo());
        assertEquals(message.toString(), newmsg.toString());
    }

    private ParticipantAckMessage makeMessage() {
        ParticipantAckMessage msg = new ParticipantAckMessage(ParticipantMessageType.PARTICIPANT_DEREGISTER_ACK);

        msg.setMessage("Successfull Ack");
        msg.setResult(true);
        msg.setResponseTo(UUID.randomUUID());

        return msg;
    }
}
