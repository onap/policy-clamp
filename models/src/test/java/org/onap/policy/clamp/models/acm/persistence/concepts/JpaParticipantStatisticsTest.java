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

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatistics;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaParticipantStatistics} class.
 */
class JpaParticipantStatisticsTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    void testJpaParticipantStatisticsConstructor() {
        assertThatThrownBy(() -> {
            new JpaParticipantStatistics((JpaParticipantStatistics) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaParticipantStatistics((PfTimestampKey) null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantStatistics(null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantStatistics(null, new PfConceptKey());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaParticipantStatistics(new PfTimestampKey(), null);
        }).hasMessageMatching("participantId is marked .*ull but is null");

        assertNotNull(new JpaParticipantStatistics());
        assertNotNull(new JpaParticipantStatistics((new PfTimestampKey())));
        assertNotNull(new JpaParticipantStatistics(new PfTimestampKey(), new PfConceptKey()));
    }

    @Test
    void testJpaParticipantStatistics() {
        JpaParticipantStatistics testJpaParticipantStatistics = createJpaParticipantStatisticsInstance();

        ParticipantStatistics aces = createParticipantStatisticsInstance();
        assertEquals(aces, testJpaParticipantStatistics.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaParticipantStatistics.fromAuthorative(null);
        }).hasMessageMatching("participantStatistics is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipantStatistics((JpaParticipantStatistics) null))
                .isInstanceOf(NullPointerException.class);

        JpaParticipantStatistics testJpaParticipantStatisticsFa = new JpaParticipantStatistics();
        testJpaParticipantStatisticsFa.setKey(null);
        testJpaParticipantStatisticsFa.fromAuthorative(aces);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatisticsFa);
        testJpaParticipantStatisticsFa.setKey(PfTimestampKey.getNullKey());
        testJpaParticipantStatisticsFa.fromAuthorative(aces);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatisticsFa);
        testJpaParticipantStatisticsFa
                .setKey(new PfTimestampKey("participantName", "0.0.1", Instant.ofEpochMilli(123456L)));
        testJpaParticipantStatisticsFa.fromAuthorative(aces);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatisticsFa);

        testJpaParticipantStatisticsFa = new JpaParticipantStatistics(aces);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatisticsFa);

        assertEquals(2, testJpaParticipantStatistics.getKeys().size());

        assertEquals("participantName", testJpaParticipantStatistics.getKey().getName());

        testJpaParticipantStatistics.clean();
        assertEquals("participantName", testJpaParticipantStatistics.getKey().getName());

        JpaParticipantStatistics testJpaParticipantStatistics2 =
                new JpaParticipantStatistics(testJpaParticipantStatistics);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatistics2);
    }

    @Test
    void testJpaParticipantStatisticsValidation() {
        JpaParticipantStatistics testJpaParticipantStatistics = createJpaParticipantStatisticsInstance();

        assertThatThrownBy(() -> {
            testJpaParticipantStatistics.validate(null);
        }).hasMessageMatching("fieldName is marked .*ull but is null");

        BeanValidationResult validationResult = testJpaParticipantStatistics.validate("");
        assertTrue(validationResult.isValid());
    }

    @Test
    void testJpaParticipantStatisticsConmpareTo() {
        JpaParticipantStatistics testJpaParticipantStatistics = createJpaParticipantStatisticsInstance();

        JpaParticipantStatistics otherJpaParticipantStatistics =
                new JpaParticipantStatistics(testJpaParticipantStatistics);
        assertEquals(0, testJpaParticipantStatistics.compareTo(otherJpaParticipantStatistics));
        assertEquals(-1, testJpaParticipantStatistics.compareTo(null));
        assertEquals(0, testJpaParticipantStatistics.compareTo(testJpaParticipantStatistics));
        assertNotEquals(0, testJpaParticipantStatistics.compareTo(new DummyJpaParticipantStatisticsChild()));

        testJpaParticipantStatistics.setState(ParticipantState.UNKNOWN);
        assertNotEquals(0, testJpaParticipantStatistics.compareTo(otherJpaParticipantStatistics));
        testJpaParticipantStatistics.setState(ParticipantState.PASSIVE);
        assertEquals(0, testJpaParticipantStatistics.compareTo(otherJpaParticipantStatistics));

        assertEquals(testJpaParticipantStatistics, new JpaParticipantStatistics(testJpaParticipantStatistics));
    }

    @Test
    void testJpaParticipantStatisticsLombok() {
        assertNotNull(new Participant());
        JpaParticipantStatistics ps0 = new JpaParticipantStatistics();

        assertThat(ps0.toString()).contains("JpaParticipantStatistics(");
        assertThat(ps0.hashCode()).isNotZero();
        assertEquals(ps0, ps0);
        assertNotEquals(null, ps0);

        JpaParticipantStatistics ps1 = new JpaParticipantStatistics();

        ps1.setState(ParticipantState.UNKNOWN);

        assertThat(ps1.toString()).contains("JpaParticipantStatistics(");
        assertNotEquals(0, ps1.hashCode());
        assertNotEquals(ps1, ps0);
        assertNotEquals(null, ps1);

        assertNotEquals(ps1, ps0);

        JpaParticipantStatistics ps2 = new JpaParticipantStatistics();
        assertEquals(ps2, ps0);
    }

    private JpaParticipantStatistics createJpaParticipantStatisticsInstance() {
        ParticipantStatistics testAces = createParticipantStatisticsInstance();
        JpaParticipantStatistics testJpaParticipantStatistics = new JpaParticipantStatistics();
        testJpaParticipantStatistics.setKey(null);
        testJpaParticipantStatistics.fromAuthorative(testAces);
        testJpaParticipantStatistics.setKey(PfTimestampKey.getNullKey());
        testJpaParticipantStatistics.fromAuthorative(testAces);

        return testJpaParticipantStatistics;
    }

    private ParticipantStatistics createParticipantStatisticsInstance() {
        ParticipantStatistics participantStatistics = new ParticipantStatistics();
        participantStatistics.setParticipantId(new ToscaConceptIdentifier("participantName", "0.0.1"));
        participantStatistics.setTimeStamp(Instant.ofEpochMilli(123456L));
        participantStatistics.setState(ParticipantState.PASSIVE);
        participantStatistics.setHealthStatus(ParticipantHealthStatus.HEALTHY);

        return participantStatistics;
    }
}
