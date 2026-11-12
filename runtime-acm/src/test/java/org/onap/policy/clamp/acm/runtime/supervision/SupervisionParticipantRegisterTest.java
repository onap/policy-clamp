/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@EmbeddedKafka(topics = {"policy-acruntime-participant", "acm-ppnt-sync"})
@ActiveProfiles({"test", "default"})
class SupervisionParticipantRegisterTest {

    @Autowired
    private SupervisionParticipantHandler supervisionParticipantHandler;

    @Autowired
    private ParticipantProvider participantProvider;

    @Test
    void testRegisterReplicas() {
        var participantId = UUID.randomUUID();
        var replica1Id = UUID.randomUUID();
        supervisionParticipantHandler
                .handleParticipantMessage(createRegisterMessage(participantId, replica1Id, "TypeRepl"));

        var replica2Id = UUID.randomUUID();
        supervisionParticipantHandler
                .handleParticipantMessage(createRegisterMessage(participantId, replica2Id, "TypeRepl"));

        var opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getReplicas()).hasSize(2);

        supervisionParticipantHandler
                .handleParticipantMessage(createDeregisterMessage(participantId, replica1Id));

        opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getReplicas()).hasSize(1);

        supervisionParticipantHandler
                .handleParticipantMessage(createDeregisterMessage(participantId, replica2Id));

        opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getReplicas()).isEmpty();
    }

    @Test
    void testRegisterParticipantSupportedElementType() {
        var participantId = UUID.randomUUID();
        var replicaId = UUID.randomUUID();

        // scenario register with 1 SupportedElementType
        var msg = createRegisterMessage(participantId, replicaId, "TypeSupp");
        supervisionParticipantHandler.handleParticipantMessage(msg);

        var opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getParticipantSupportedElementTypes()).hasSize(1);

        // scenario register with new SupportedElementTypes
        msg = createRegisterMessage(participantId, replicaId, "TypeSupp");
        var supportedElementType = CommonTestData.createParticipantSupportedElementType();
        supportedElementType.setTypeName("SecondType");
        msg.getParticipantSupportedElementType().add(supportedElementType);
        supervisionParticipantHandler.handleParticipantMessage(msg);

        opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getParticipantSupportedElementTypes()).hasSize(2);

        // scenario register with update SupportedElementType
        msg = createRegisterMessage(participantId, replicaId, "TypeSupp");
        supportedElementType.setTypeVersion("1.1.1");
        msg.getParticipantSupportedElementType().add(supportedElementType);
        supervisionParticipantHandler.handleParticipantMessage(msg);

        opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getParticipantSupportedElementTypes()).hasSize(2);

        // scenario register with 1 SupportedElementType
        msg = createRegisterMessage(participantId, replicaId, "TypeSupp");
        supervisionParticipantHandler.handleParticipantMessage(msg);

        opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getParticipantSupportedElementTypes()).hasSize(1);

        supervisionParticipantHandler
                .handleParticipantMessage(createDeregisterMessage(participantId, replicaId));

        opt = participantProvider.findParticipant(participantId);
        assertThat(opt).isPresent();
        assertThat(opt.get().getReplicas()).isEmpty();

    }

    private ParticipantRegister createRegisterMessage(UUID participantId, UUID replicaId, String typeName) {
        var msg = new ParticipantRegister();
        msg.setParticipantId(participantId);
        msg.setReplicaId(replicaId);
        msg.setParticipantSupportedElementType(new ArrayList<>());
        var supportedElementType = new ParticipantSupportedElementType();
        supportedElementType.setTypeName(typeName);
        supportedElementType.setTypeVersion("1.0.0");
        msg.getParticipantSupportedElementType().add(supportedElementType);
        return msg;
    }

    static ParticipantDeregister createDeregisterMessage(UUID participantId, UUID replicaId) {
        var msg = new ParticipantDeregister();
        msg.setParticipantId(participantId);
        msg.setReplicaId(replicaId);
        return msg;
    }

}
