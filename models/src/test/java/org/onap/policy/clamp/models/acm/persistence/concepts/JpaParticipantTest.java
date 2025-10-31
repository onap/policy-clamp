/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.Participant;

/**
 * Test the {@link JpaParticipant} class.
 */
class JpaParticipantTest {

    private static final String NULL_KEY_ERROR = "participantId is marked .*ull but is null";

    @Test
    void testJpaParticipantConstructor() {
        assertThatThrownBy(() -> new JpaParticipant((Participant) null))
            .hasMessageMatching("authorativeConcept is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant((JpaParticipant) null))
            .hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(null, new ArrayList<>(), new ArrayList<>()))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(UUID.randomUUID().toString(), null, new ArrayList<>()))
            .hasMessageMatching("supportedElements is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(UUID.randomUUID().toString(), new ArrayList<>(), null))
            .hasMessageMatching("replicas is marked .*ull but is null");

        assertDoesNotThrow(() -> new JpaParticipant(UUID.randomUUID().toString(),
                new ArrayList<>(), new ArrayList<>()));
    }

    @Test
    void testJpaParticipant() {
        var participant = createParticipantInstance();
        var testJpaParticipant = new JpaParticipant(participant);

        assertEquals(participant, testJpaParticipant.toAuthorative());

        assertThatThrownBy(() -> testJpaParticipant.fromAuthorative(null))
            .hasMessageMatching("participant is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant((JpaParticipant) null)).isInstanceOf(NullPointerException.class);

        var testJpaParticipantFa = new JpaParticipant();
        testJpaParticipantFa.fromAuthorative(participant);
        testJpaParticipantFa.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(testJpaParticipant, testJpaParticipantFa);

        var testJpaParticipant2 = new JpaParticipant(testJpaParticipant);
        testJpaParticipant2.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(testJpaParticipant, testJpaParticipant2);
    }

    @Test
    void testJpaParticipantValidation() {
        var testJpaParticipant = new JpaParticipant(createParticipantInstance());

        assertThatThrownBy(() -> testJpaParticipant.validate(null))
            .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaParticipant.validate("").isValid());
    }

    @Test
    void testJpaParticipantCompareTo() {
        var testJpaParticipant = new JpaParticipant(createParticipantInstance());

        var otherJpaParticipant = new JpaParticipant(testJpaParticipant);
        otherJpaParticipant.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        assertEquals(-1, testJpaParticipant.compareTo(null));
        assertEquals(0, testJpaParticipant.compareTo(testJpaParticipant));
        assertNotEquals(0, testJpaParticipant.compareTo(new DummyJpaParticipantChild()));

        var newJpaParticipant = new JpaParticipant(testJpaParticipant);
        newJpaParticipant.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(testJpaParticipant, newJpaParticipant);
    }

    @Test
    void testJpaParticipantLombok() {
        var p0 = new JpaParticipant();

        assertThat(p0.toString()).contains("JpaParticipant(");
        assertThat(p0.hashCode()).isNotZero();
        assertNotEquals(null, p0);


        var p1 = new JpaParticipant();
        assertThat(p1.toString()).contains("Participant(");
        assertNotEquals(0, p1.hashCode());
        assertNotEquals(p1, p0);
        assertNotEquals(null, p1);

        assertNotEquals(p1, p0);

        var p2 = new JpaParticipant();
        p2.setParticipantId(p0.getParticipantId());
        assertEquals(p2, p0);
    }

    private Participant createParticipantInstance() {
        var testParticipant = new Participant();
        testParticipant.setParticipantId(UUID.randomUUID());
        testParticipant.setParticipantSupportedElementTypes(new LinkedHashMap<>());
        testParticipant.setReplicas(new LinkedHashMap<>());

        return testParticipant;
    }
}
