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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaParticipant} class.
 */
class JpaParticipantTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    void testJpaParticipantConstructor() {
        assertThatThrownBy(() -> new JpaParticipant((JpaParticipant) null))
            .hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant((PfConceptKey) null)).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, null, null, null)).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, null, null, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, null, ParticipantState.ON_LINE, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(
            () -> new JpaParticipant(null, null, null, ParticipantState.ON_LINE, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, new PfConceptKey(), null, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, new PfConceptKey(), null, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, new PfConceptKey(), ParticipantState.ON_LINE, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, new PfConceptKey(), ParticipantState.ON_LINE, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), null, null, null))
            .hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), null, null, null))
            .hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), null, ParticipantState.ON_LINE, null))
            .hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), null, ParticipantState.ON_LINE, null
        )).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), new PfConceptKey(), null, null))
            .hasMessageMatching("participantState is marked .*ull but is null");

        assertThatThrownBy(
            () -> new JpaParticipant(null, new PfConceptKey(), new PfConceptKey(), null, null))
            .hasMessageMatching("participantState is marked .*ull but is null");

        assertNotNull(new JpaParticipant());
        assertNotNull(new JpaParticipant((new PfConceptKey())));
        assertNotNull(new JpaParticipant(null, new PfConceptKey(), new PfConceptKey(),
            ParticipantState.ON_LINE, new ArrayList<>()));
        assertNotNull(new JpaParticipant(UUID.randomUUID().toString(), new PfConceptKey(),
            new PfConceptKey(), ParticipantState.ON_LINE, new ArrayList<>()));
    }

    @Test
    void testJpaParticipant() {
        JpaParticipant testJpaParticipant = createJpaParticipantInstance();

        Participant participant = createParticipantInstance();

        participant.setParticipantId(testJpaParticipant.toAuthorative().getParticipantId());

        assertEquals(participant, testJpaParticipant.toAuthorative());

        assertThatThrownBy(() -> testJpaParticipant.fromAuthorative(null))
            .hasMessageMatching("participant is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant((JpaParticipant) null)).isInstanceOf(NullPointerException.class);

        JpaParticipant testJpaParticipantFa = new JpaParticipant();
        testJpaParticipantFa.setKey(null);
        testJpaParticipantFa.fromAuthorative(participant);
        testJpaParticipantFa.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(testJpaParticipant, testJpaParticipantFa);
        testJpaParticipantFa.setKey(PfConceptKey.getNullKey());
        testJpaParticipantFa.fromAuthorative(participant);
        assertEquals(testJpaParticipant, testJpaParticipantFa);
        testJpaParticipantFa.setKey(new PfConceptKey("participant", "0.0.1"));
        testJpaParticipantFa.fromAuthorative(participant);
        assertEquals(testJpaParticipant, testJpaParticipantFa);

        assertEquals("participant", testJpaParticipant.getKey().getName());
        assertEquals("participant", new JpaParticipant(createJpaParticipantInstance()).getKey().getName());
        assertEquals("participant",
            ((PfConceptKey) new JpaParticipant(createJpaParticipantInstance()).getKeys().get(0)).getName());

        testJpaParticipant.clean();
        assertEquals("participant", testJpaParticipant.getKey().getName());

        testJpaParticipant.setDescription("   A Message   ");
        testJpaParticipant.clean();
        assertEquals("A Message", testJpaParticipant.getDescription());

        JpaParticipant testJpaParticipant2 = new JpaParticipant(testJpaParticipant);
        testJpaParticipant2.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(testJpaParticipant, testJpaParticipant2);
    }

    @Test
    void testJpaParticipantValidation() {
        JpaParticipant testJpaParticipant = createJpaParticipantInstance();

        assertThatThrownBy(() -> testJpaParticipant.validate(null))
            .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaParticipant.validate("").isValid());
    }

    @Test
    void testJpaParticipantCompareTo() {
        JpaParticipant testJpaParticipant = createJpaParticipantInstance();

        JpaParticipant otherJpaParticipant = new JpaParticipant(testJpaParticipant);
        otherJpaParticipant.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        assertEquals(-1, testJpaParticipant.compareTo(null));
        assertEquals(0, testJpaParticipant.compareTo(testJpaParticipant));
        assertNotEquals(0, testJpaParticipant.compareTo(new DummyJpaParticipantChild()));

        testJpaParticipant.setKey(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        testJpaParticipant.setKey(new PfConceptKey("participant", "0.0.1"));
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));

        testJpaParticipant.setDefinition(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        testJpaParticipant.setDefinition(new PfConceptKey("participantDefinitionName", "0.0.1"));
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));

        testJpaParticipant.setParticipantState(ParticipantState.OFF_LINE);
        assertNotEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        testJpaParticipant.setParticipantState(ParticipantState.ON_LINE);
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        assertEquals(testJpaParticipant, new JpaParticipant(testJpaParticipant));

        JpaParticipant newJpaParticipant = new JpaParticipant(testJpaParticipant);
        newJpaParticipant.setParticipantId(testJpaParticipant.getParticipantId());
        assertEquals(testJpaParticipant, newJpaParticipant);
    }

    @Test
    void testJpaParticipantLombok() {
        assertNotNull(new Participant());
        JpaParticipant p0 = new JpaParticipant();

        assertThat(p0.toString()).contains("JpaParticipant(");
        assertThat(p0.hashCode()).isNotZero();
        assertEquals(p0, p0);
        assertNotEquals(null, p0);


        JpaParticipant p1 = new JpaParticipant();

        p1.setDefinition(new PfConceptKey("defName", "0.0.1"));
        p1.setDescription("Description");
        p1.setKey(new PfConceptKey("participant", "0.0.1"));
        p1.setParticipantState(ParticipantState.ON_LINE);

        assertThat(p1.toString()).contains("Participant(");
        assertNotEquals(0, p1.hashCode());
        assertNotEquals(p1, p0);
        assertNotEquals(null, p1);

        assertNotEquals(p1, p0);

        JpaParticipant p2 = new JpaParticipant();
        p2.setParticipantId(p0.getParticipantId());
        assertEquals(p2, p0);
    }

    private JpaParticipant createJpaParticipantInstance() {
        Participant testParticipant = createParticipantInstance();
        JpaParticipant testJpaParticipant = new JpaParticipant();
        testJpaParticipant.setKey(null);
        testParticipant.setParticipantId(UUID.fromString(testJpaParticipant.getParticipantId()));
        testJpaParticipant.fromAuthorative(testParticipant);
        testJpaParticipant.setKey(PfConceptKey.getNullKey());
        testJpaParticipant.fromAuthorative(testParticipant);

        return testJpaParticipant;
    }

    private Participant createParticipantInstance() {
        Participant testParticipant = new Participant();
        testParticipant.setName("participant");
        testParticipant.setVersion("0.0.1");
        testParticipant.setDefinition(new ToscaConceptIdentifier("participantDefinitionName", "0.0.1"));
        testParticipant.setParticipantType(new ToscaConceptIdentifier("participantTypeName", "0.0.1"));
        testParticipant.setParticipantSupportedElementTypes(new LinkedHashMap<>());

        return testParticipant;
    }
}
