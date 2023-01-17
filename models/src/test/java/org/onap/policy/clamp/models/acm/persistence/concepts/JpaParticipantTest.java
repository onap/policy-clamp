/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaParticipant} class.
 */
class JpaParticipantTest {

    private static final String NULL_KEY_ERROR = "participantId is marked .*ull but is null";

    @Test
    void testJpaParticipantConstructor() {
        assertThatThrownBy(() -> new JpaParticipant((JpaParticipant) null))
            .hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(null, null, null)).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, ParticipantState.ON_LINE, new ArrayList<>()))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(UUID.randomUUID().toString(), null, new ArrayList<>()))
            .hasMessageMatching("participantState is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(UUID.randomUUID().toString(), ParticipantState.ON_LINE, null))
            .hasMessageMatching("supportedElements is marked .*ull but is null");

        assertNotNull(new JpaParticipant());
        assertNotNull(new JpaParticipant(UUID.randomUUID().toString(), ParticipantState.ON_LINE, new ArrayList<>()));

    }

    @Test
    void testJpaParticipant() {
        var testJpaParticipant = createJpaParticipantInstance();

        var participant = createParticipantInstance();

        participant.setParticipantId(testJpaParticipant.toAuthorative().getParticipantId());

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
        var testJpaParticipant = createJpaParticipantInstance();

        assertThatThrownBy(() -> testJpaParticipant.validate(null))
            .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaParticipant.validate("").isValid());
    }

    @Test
    void testJpaParticipantCompareTo() {
        var testJpaParticipant = createJpaParticipantInstance();

        var otherJpaParticipant = new JpaParticipant(testJpaParticipant);
        otherJpaParticipant.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        assertEquals(-1, testJpaParticipant.compareTo(null));
        assertEquals(0, testJpaParticipant.compareTo(testJpaParticipant));
        assertNotEquals(0, testJpaParticipant.compareTo(new DummyJpaParticipantChild()));

        testJpaParticipant.setParticipantState(ParticipantState.OFF_LINE);
        assertNotEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        testJpaParticipant.setParticipantState(ParticipantState.ON_LINE);
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        assertEquals(testJpaParticipant, new JpaParticipant(testJpaParticipant));

        var newJpaParticipant = new JpaParticipant(testJpaParticipant);
        newJpaParticipant.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(testJpaParticipant, newJpaParticipant);
    }

    @Test
    void testJpaParticipantLombok() {
        assertNotNull(new Participant());
        var p0 = new JpaParticipant();

        assertThat(p0.toString()).contains("JpaParticipant(");
        assertThat(p0.hashCode()).isNotZero();
        assertEquals(p0, p0);
        assertNotEquals(null, p0);


        var p1 = new JpaParticipant();
        p1.setParticipantState(ParticipantState.ON_LINE);

        assertThat(p1.toString()).contains("Participant(");
        assertNotEquals(0, p1.hashCode());
        assertNotEquals(p1, p0);
        assertNotEquals(null, p1);

        assertNotEquals(p1, p0);

        var p2 = new JpaParticipant();
        p2.setParticipantId(p0.getParticipantId());
        assertEquals(p2, p0);
    }

    private JpaParticipant createJpaParticipantInstance() {
        var testParticipant = createParticipantInstance();
        var testJpaParticipant = new JpaParticipant();
        testParticipant.setParticipantId(UUID.fromString(testJpaParticipant.getParticipantId()));
        testJpaParticipant.fromAuthorative(testParticipant);
        testJpaParticipant.fromAuthorative(testParticipant);

        return testJpaParticipant;
    }

    private Participant createParticipantInstance() {
        var testParticipant = new Participant();
        testParticipant.setParticipantId(UUID.randomUUID());
        testParticipant.setParticipantType(new ToscaConceptIdentifier("participantTypeName", "0.0.1"));
        testParticipant.setParticipantSupportedElementTypes(new LinkedHashMap<>());

        return testParticipant;
    }
}
