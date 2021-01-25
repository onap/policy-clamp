/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaParticipantStatistics} class.
 */
public class JpaParticipantStatisticsTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    public void testJpaParticipantStatisticsConstructor() {
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
    public void testJpaParticipantStatistics() {
        JpaParticipantStatistics testJpaParticipantStatistics = createJpaParticipantStatisticsInstance();

        ParticipantStatistics cles = createParticipantStatisticsInstance();
        assertEquals(cles, testJpaParticipantStatistics.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaParticipantStatistics.fromAuthorative(null);
        }).hasMessageMatching("participantStatistics is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaParticipantStatistics((JpaParticipantStatistics) null))
                .isInstanceOf(NullPointerException.class);

        JpaParticipantStatistics testJpaParticipantStatisticsFa = new JpaParticipantStatistics();
        testJpaParticipantStatisticsFa.setKey(null);
        testJpaParticipantStatisticsFa.fromAuthorative(cles);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatisticsFa);
        testJpaParticipantStatisticsFa.setKey(PfTimestampKey.getNullKey());
        testJpaParticipantStatisticsFa.fromAuthorative(cles);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatisticsFa);
        testJpaParticipantStatisticsFa.setKey(new PfTimestampKey("participantName", "0.0.1", new Date(123456L)));
        testJpaParticipantStatisticsFa.fromAuthorative(cles);
        assertEquals(testJpaParticipantStatistics, testJpaParticipantStatisticsFa);

        testJpaParticipantStatisticsFa = new JpaParticipantStatistics(cles);
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
    public void testJpaParticipantStatisticsValidation() {
        JpaParticipantStatistics testJpaParticipantStatistics = createJpaParticipantStatisticsInstance();

        assertThatThrownBy(() -> {
            testJpaParticipantStatistics.validate(null);
        }).hasMessageMatching("fieldName is marked .*ull but is null");

        BeanValidationResult validationResult = testJpaParticipantStatistics.validate("");
        assertTrue(validationResult.isValid());
    }

    @Test
    public void testJpaParticipantStatisticsConmpareTo() {
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
    public void testJpaParticipantStatisticsLombok() {
        assertNotNull(new Participant());
        JpaParticipantStatistics p0 = new JpaParticipantStatistics();

        assertThat(p0.toString()).contains("JpaParticipantStatistics(");
        assertEquals(false, p0.hashCode() == 0);
        assertEquals(true, p0.equals(p0));
        assertEquals(false, p0.equals(null));


        JpaParticipantStatistics p1 = new JpaParticipantStatistics();

        p1.setState(ParticipantState.UNKNOWN);

        assertThat(p1.toString()).contains("JpaParticipantStatistics(");
        assertEquals(false, p1.hashCode() == 0);
        assertEquals(false, p1.equals(p0));
        assertEquals(false, p1.equals(null));

        assertNotEquals(p1, p0);

        JpaParticipantStatistics p2 = new JpaParticipantStatistics();
        assertEquals(p2, p0);
    }

    private JpaParticipantStatistics createJpaParticipantStatisticsInstance() {
        ParticipantStatistics testCles = createParticipantStatisticsInstance();
        JpaParticipantStatistics testJpaParticipantStatistics = new JpaParticipantStatistics();
        testJpaParticipantStatistics.setKey(null);
        testJpaParticipantStatistics.fromAuthorative(testCles);
        testJpaParticipantStatistics.setKey(PfTimestampKey.getNullKey());
        testJpaParticipantStatistics.fromAuthorative(testCles);

        return testJpaParticipantStatistics;
    }

    private ParticipantStatistics createParticipantStatisticsInstance() {
        ParticipantStatistics participantStatistics = new ParticipantStatistics();
        participantStatistics.setParticipantId(new ToscaConceptIdentifier("participantName", "0.0.1"));
        participantStatistics.setTimeStamp(new Date(123456L));
        participantStatistics.setState(ParticipantState.PASSIVE);
        participantStatistics.setHealthStatus(ParticipantHealthStatus.HEALTHY);

        return participantStatistics;
    }
}
