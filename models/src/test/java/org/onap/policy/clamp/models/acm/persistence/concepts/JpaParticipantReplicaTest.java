/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;

class JpaParticipantReplicaTest {

    @Test
    void testJpaParticipantReplicaConstructor() {
        assertThatThrownBy(() -> new JpaParticipantReplica(UUID.randomUUID().toString(), null))
                .hasMessageMatching("participantId is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipantReplica(null, UUID.randomUUID().toString()))
                .hasMessageMatching("replicaId is marked .*ull but is null");

        assertDoesNotThrow(() -> new JpaParticipantReplica());
        assertDoesNotThrow(() -> new JpaParticipantReplica(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    }

    @Test
    void testJpaParticipantReplica() {
        var p0 = new JpaParticipantReplica();

        assertThat(p0.toString()).contains("JpaParticipantReplica(");
        assertThat(p0.hashCode()).isNotZero();
        assertNotEquals(null, p0);

        var p1 = new JpaParticipantReplica();
        p1.setParticipantState(ParticipantState.ON_LINE);

        assertThat(p1.toString()).contains("ParticipantReplica(");
        assertNotEquals(0, p1.hashCode());
        assertNotEquals(p1, p0);
        assertNotEquals(null, p1);

        var p2 = new JpaParticipantReplica();
        p2.setReplicaId(p0.getReplicaId());
        p2.setParticipantId(p0.getParticipantId());
        p2.setLastMsg(p0.getLastMsg());
        p2.setParticipantState(p0.getParticipantState());
        assertEquals(p2, p0);
    }
}
