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

package org.onap.policy.clamp.models.acm.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;

class ParticipantReplicaTest {

    @Test
    void testParticipantLombok() {
        assertDoesNotThrow(() -> new ParticipantReplica());
        var p0 = new ParticipantReplica();

        assertThat(p0.toString()).contains("ParticipantReplica(");
        assertThat(p0.hashCode()).isNotZero();
        assertNotEquals(null, p0);

        var p1 = new ParticipantReplica();

        p1.setReplicaId(CommonTestData.getReplicaId());
        p1.setParticipantState(ParticipantState.ON_LINE);

        assertThat(p1.toString()).contains("ParticipantReplica(");
        assertNotEquals(0, p1.hashCode());
        assertNotEquals(p1, p0);
        assertNotEquals(null, p1);

        var p2 = new ParticipantReplica();
        assertThatThrownBy(() -> p2.setParticipantState(null)).isInstanceOf(NullPointerException.class);
        assertEquals(p2, p0);
    }

    @Test
    void testCopyConstructor() {
        var p0 = new ParticipantReplica();
        p0.setReplicaId(UUID.randomUUID());
        p0.setParticipantState(ParticipantState.ON_LINE);

        var p2 = new ParticipantReplica(p0);
        assertEquals(p2, p0);
    }
}
