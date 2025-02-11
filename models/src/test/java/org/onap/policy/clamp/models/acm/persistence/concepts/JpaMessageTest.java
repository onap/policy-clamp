/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;

class JpaMessageTest {

    @Test
    void testJpaMessageConstructor() {
        assertThatThrownBy(() -> new JpaMessage(null, new DocMessage()))
                .hasMessageMatching("identificationId is marked .*ull but is null");
        assertThatThrownBy(() -> new JpaMessage(UUID.randomUUID().toString(), null))
                .hasMessageMatching("docMessage is marked .*ull but is null");
    }

    @Test
    void testJpaMessageValidation() {
        var docMessage = createDocMessage();
        var jpaMessage = new JpaMessage(docMessage.getInstanceId().toString(), docMessage);

        assertThatThrownBy(() -> jpaMessage.validate(null))
                .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(jpaMessage.validate("").isValid());

        jpaMessage.setLastMsg(null);
        assertFalse(jpaMessage.validate("").isValid());
    }

    @Test
    void testJpaMessage() {
        var docMessage = createDocMessage();
        var jpaMessage = new JpaMessage(docMessage.getInstanceId().toString(), docMessage);
        docMessage.setMessageId(jpaMessage.getMessageId());

        assertEquals(docMessage, jpaMessage.toAuthorative());

        assertThatThrownBy(() -> jpaMessage.fromAuthorative(null))
                .hasMessageMatching("docMessage is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaMessage((JpaMessage) null)).isInstanceOf(NullPointerException.class);

        var jpaMessageFa = new JpaMessage();
        jpaMessageFa.setIdentificationId(docMessage.getInstanceId().toString());
        jpaMessageFa.setLastMsg(jpaMessage.getLastMsg());
        jpaMessageFa.setMessageId(jpaMessage.getMessageId());
        jpaMessageFa.fromAuthorative(docMessage);
        assertEquals(jpaMessage, jpaMessageFa);

        var jpaMessage2 = new JpaMessage(jpaMessage);
        assertEquals(jpaMessage, jpaMessage2);
    }

    private DocMessage createDocMessage() {
        var docMessage = new DocMessage();
        docMessage.setMessageType(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
        docMessage.setCompositionId(UUID.randomUUID());
        docMessage.setInstanceId(UUID.randomUUID());
        docMessage.setDeployState(DeployState.DEPLOYED);
        return docMessage;
    }
}
