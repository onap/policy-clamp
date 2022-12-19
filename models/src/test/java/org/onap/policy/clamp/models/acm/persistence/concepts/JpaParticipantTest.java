/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
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

        assertThatThrownBy(() -> new JpaParticipant(null, null, null, null)).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, null, ParticipantHealthStatus.HEALTHY))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, null, ParticipantState.ACTIVE, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(
            () -> new JpaParticipant(null, null, ParticipantState.ACTIVE, ParticipantHealthStatus.HEALTHY))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), null, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), null, ParticipantHealthStatus.HEALTHY))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), ParticipantState.ACTIVE, null))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(null, new PfConceptKey(), ParticipantState.ACTIVE,
            ParticipantHealthStatus.HEALTHY))
            .hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> new JpaParticipant(new PfConceptKey(), null, null, null))
            .hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(new PfConceptKey(), null, null, ParticipantHealthStatus.HEALTHY))
            .hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(new PfConceptKey(), null, ParticipantState.ACTIVE, null))
            .hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(new PfConceptKey(), null, ParticipantState.ACTIVE,
            ParticipantHealthStatus.HEALTHY)).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant(new PfConceptKey(), new PfConceptKey(), null, null))
            .hasMessageMatching("participantState is marked .*ull but is null");

        assertThatThrownBy(
            () -> new JpaParticipant(new PfConceptKey(), new PfConceptKey(), null, ParticipantHealthStatus.HEALTHY))
            .hasMessageMatching("participantState is marked .*ull but is null");

        assertThatThrownBy(
            () -> new JpaParticipant(new PfConceptKey(), new PfConceptKey(), ParticipantState.ACTIVE, null))
            .hasMessageMatching("healthStatus is marked .*ull but is null");

        assertNotNull(new JpaParticipant());
        assertNotNull(new JpaParticipant((new PfConceptKey())));
        assertNotNull(new JpaParticipant(new PfConceptKey(), new PfConceptKey(), ParticipantState.ACTIVE,
            ParticipantHealthStatus.HEALTHY));
    }

    @Test
    void testJpaParticipant() {
        JpaParticipant testJpaParticipant = createJpaParticipantInstance();

        Participant participant = createParticipantInstance();
        assertEquals(participant, testJpaParticipant.toAuthorative());

        assertThatThrownBy(() -> testJpaParticipant.fromAuthorative(null))
            .hasMessageMatching("participant is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipant((JpaParticipant) null)).isInstanceOf(NullPointerException.class);

        JpaParticipant testJpaParticipantFa = new JpaParticipant();
        testJpaParticipantFa.setKey(null);
        testJpaParticipantFa.fromAuthorative(participant);
        assertEquals(testJpaParticipant, testJpaParticipantFa);
        testJpaParticipantFa.setKey(PfConceptKey.getNullKey());
        testJpaParticipantFa.fromAuthorative(participant);
        assertEquals(testJpaParticipant, testJpaParticipantFa);
        testJpaParticipantFa.setKey(new PfConceptKey("participant", "0.0.1"));
        testJpaParticipantFa.fromAuthorative(participant);
        assertEquals(testJpaParticipant, testJpaParticipantFa);

        assertEquals("participant", testJpaParticipant.getKey().getName());
        assertEquals("participant", new JpaParticipant(createParticipantInstance()).getKey().getName());
        assertEquals("participant",
            ((PfConceptKey) new JpaParticipant(createParticipantInstance()).getKeys().get(0)).getName());

        testJpaParticipant.clean();
        assertEquals("participant", testJpaParticipant.getKey().getName());

        testJpaParticipant.setDescription("   A Message   ");
        testJpaParticipant.clean();
        assertEquals("A Message", testJpaParticipant.getDescription());

        JpaParticipant testJpaParticipant2 = new JpaParticipant(testJpaParticipant);
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

        testJpaParticipant.setParticipantState(ParticipantState.PASSIVE);
        assertNotEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        testJpaParticipant.setParticipantState(ParticipantState.UNKNOWN);
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));

        testJpaParticipant.setHealthStatus(ParticipantHealthStatus.NOT_HEALTHY);
        assertNotEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));
        testJpaParticipant.setHealthStatus(ParticipantHealthStatus.UNKNOWN);
        assertEquals(0, testJpaParticipant.compareTo(otherJpaParticipant));

        assertEquals(testJpaParticipant, new JpaParticipant(testJpaParticipant));
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
        p1.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        p1.setKey(new PfConceptKey("participant", "0.0.1"));
        p1.setParticipantState(ParticipantState.ACTIVE);

        assertThat(p1.toString()).contains("Participant(");
        assertNotEquals(0, p1.hashCode());
        assertNotEquals(p1, p0);
        assertNotEquals(null, p1);

        assertNotEquals(p1, p0);

        JpaParticipant p2 = new JpaParticipant();
        assertEquals(p2, p0);
    }

    private JpaParticipant createJpaParticipantInstance() {
        Participant testParticipant = createParticipantInstance();
        JpaParticipant testJpaParticipant = new JpaParticipant();
        testJpaParticipant.setKey(null);
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

        return testParticipant;
    }
}
