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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfReferenceTimestampKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaAcElementStatistics} class.
 */
class JpaAcElementStatisticsTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    void testJpaAcElementStatisticsConstructor() {
        assertThatThrownBy(() -> {
            new JpaAcElementStatistics((JpaAcElementStatistics) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAcElementStatistics((PfReferenceTimestampKey) null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAcElementStatistics(null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAcElementStatistics(null, new PfConceptKey());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAcElementStatistics(new PfReferenceTimestampKey(), null);
        }).hasMessageMatching("participantId is marked .*ull but is null");

        assertNotNull(new JpaAcElementStatistics());
        assertNotNull(new JpaAcElementStatistics((new PfReferenceTimestampKey())));
        assertNotNull(new JpaAcElementStatistics(new PfReferenceTimestampKey(), new PfConceptKey()));
    }

    @Test
    void testJpaAcElementStatistics() {
        JpaAcElementStatistics testJpaAcElementStatistics = createJpaAcElementStatisticsInstance();

        AcElementStatistics aces = createAcElementStatisticsInstance();
        assertEquals(aces, testJpaAcElementStatistics.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaAcElementStatistics.fromAuthorative(null);
        }).hasMessageMatching("acElementStatistics is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaAcElementStatistics((JpaAcElementStatistics) null))
                .isInstanceOf(NullPointerException.class);

        JpaAcElementStatistics testJpaAcElementStatisticsFa = new JpaAcElementStatistics();
        testJpaAcElementStatisticsFa.setKey(null);
        testJpaAcElementStatisticsFa.fromAuthorative(aces);
        assertEquals(testJpaAcElementStatistics, testJpaAcElementStatisticsFa);
        testJpaAcElementStatisticsFa.setKey(PfReferenceTimestampKey.getNullKey());
        testJpaAcElementStatisticsFa.fromAuthorative(aces);
        assertEquals(testJpaAcElementStatistics, testJpaAcElementStatisticsFa);
        testJpaAcElementStatisticsFa.setKey(new PfReferenceTimestampKey("elementName", "0.0.1",
            "a95757ba-b34a-4049-a2a8-46773abcbe5e", Instant.ofEpochSecond(123456L)));
        testJpaAcElementStatisticsFa.fromAuthorative(aces);
        assertEquals(testJpaAcElementStatistics, testJpaAcElementStatisticsFa);

        testJpaAcElementStatisticsFa = new JpaAcElementStatistics(aces);
        assertEquals(testJpaAcElementStatistics, testJpaAcElementStatisticsFa);

        assertEquals(1, testJpaAcElementStatistics.getKeys().size());

        assertEquals("elementName", testJpaAcElementStatistics.getKey().getReferenceKey().getParentKeyName());

        testJpaAcElementStatistics.clean();
        assertEquals("elementName", testJpaAcElementStatistics.getKey().getReferenceKey().getParentKeyName());

        JpaAcElementStatistics testJpaAcElementStatistics2 = new JpaAcElementStatistics(testJpaAcElementStatistics);
        assertEquals(testJpaAcElementStatistics, testJpaAcElementStatistics2);
    }

    @Test
    void testJpaAcElementStatisticsValidation() {
        JpaAcElementStatistics testJpaAcElementStatistics = createJpaAcElementStatisticsInstance();

        assertThatThrownBy(() -> {
            testJpaAcElementStatistics.validate(null);
        }).hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaAcElementStatistics.validate("").isValid());
    }

    @Test
    void testJpaAcElementStatisticsCompareTo() {
        JpaAcElementStatistics testJpaAcElementStatistics = createJpaAcElementStatisticsInstance();

        JpaAcElementStatistics otherJpaAcElementStatistics = new JpaAcElementStatistics(testJpaAcElementStatistics);
        assertEquals(0, testJpaAcElementStatistics.compareTo(otherJpaAcElementStatistics));
        assertEquals(-1, testJpaAcElementStatistics.compareTo(null));
        assertEquals(0, testJpaAcElementStatistics.compareTo(testJpaAcElementStatistics));
        assertNotEquals(0, testJpaAcElementStatistics.compareTo(new DummyJpaAcElementStatisticsChild()));

        testJpaAcElementStatistics.setState(AutomationCompositionState.PASSIVE);
        assertNotEquals(0, testJpaAcElementStatistics.compareTo(otherJpaAcElementStatistics));
        testJpaAcElementStatistics.setState(AutomationCompositionState.UNINITIALISED);
        assertEquals(0, testJpaAcElementStatistics.compareTo(otherJpaAcElementStatistics));

        assertEquals(testJpaAcElementStatistics, new JpaAcElementStatistics(testJpaAcElementStatistics));
    }

    @Test
    void testJpaAcElementStatisticsLombok() {
        assertNotNull(new Participant());
        JpaAcElementStatistics aces0 = new JpaAcElementStatistics();

        assertThat(aces0.toString()).contains("JpaAcElementStatistics(");
        assertThat(aces0.hashCode()).isNotZero();
        assertEquals(aces0, aces0);
        assertNotEquals(null, aces0);


        JpaAcElementStatistics aces11 = new JpaAcElementStatistics();

        aces11.setState(AutomationCompositionState.UNINITIALISED);

        assertThat(aces11.toString()).contains("JpaAcElementStatistics(");
        assertNotEquals(0, aces11.hashCode());
        assertNotEquals(aces11, aces0);
        assertNotEquals(null, aces11);

        assertNotEquals(aces11, aces0);

        JpaAcElementStatistics aces2 = new JpaAcElementStatistics();
        assertEquals(aces2, aces0);
    }

    private JpaAcElementStatistics createJpaAcElementStatisticsInstance() {
        AcElementStatistics testAces = createAcElementStatisticsInstance();
        JpaAcElementStatistics testJpaAcElementStatistics = new JpaAcElementStatistics();
        testJpaAcElementStatistics.setKey(null);
        testJpaAcElementStatistics.fromAuthorative(testAces);
        testJpaAcElementStatistics.setKey(PfReferenceTimestampKey.getNullKey());
        testJpaAcElementStatistics.fromAuthorative(testAces);

        return testJpaAcElementStatistics;
    }

    private AcElementStatistics createAcElementStatisticsInstance() {
        AcElementStatistics acElementStatistics = new AcElementStatistics();
        acElementStatistics.setParticipantId(new ToscaConceptIdentifier("elementName", "0.0.1"));
        acElementStatistics.setId(UUID.fromString("a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        acElementStatistics.setTimeStamp(Instant.ofEpochSecond(123456L));
        acElementStatistics.setState(AutomationCompositionState.UNINITIALISED);

        return acElementStatistics;
    }
}
