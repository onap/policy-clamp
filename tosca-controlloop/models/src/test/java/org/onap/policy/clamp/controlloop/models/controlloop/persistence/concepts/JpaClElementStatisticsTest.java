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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaClElementStatistics} class.
 */
public class JpaClElementStatisticsTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    public void testJpaClElementStatisticsConstructor() {
        assertThatThrownBy(() -> {
            new JpaClElementStatistics((JpaClElementStatistics) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaClElementStatistics((PfTimestampKey) null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaClElementStatistics(null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaClElementStatistics(null, new PfConceptKey());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaClElementStatistics(new PfTimestampKey(), null);
        }).hasMessageMatching("clElementId is marked .*ull but is null");

        assertNotNull(new JpaClElementStatistics());
        assertNotNull(new JpaClElementStatistics((new PfTimestampKey())));
        assertNotNull(new JpaClElementStatistics(new PfTimestampKey(), new PfConceptKey()));
    }

    @Test
    public void testJpaClElementStatistics() {
        JpaClElementStatistics testJpaClElementStatistics = createJpaClElementStatisticsInstance();

        ClElementStatistics cles = createClElementStatisticsInstance();
        assertEquals(cles, testJpaClElementStatistics.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaClElementStatistics.fromAuthorative(null);
        }).hasMessageMatching("clElementStatistics is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaClElementStatistics((JpaClElementStatistics) null))
                .isInstanceOf(NullPointerException.class);

        JpaClElementStatistics testJpaClElementStatisticsFa = new JpaClElementStatistics();
        testJpaClElementStatisticsFa.setKey(null);
        testJpaClElementStatisticsFa.fromAuthorative(cles);
        assertEquals(testJpaClElementStatistics, testJpaClElementStatisticsFa);
        testJpaClElementStatisticsFa.setKey(PfTimestampKey.getNullKey());
        testJpaClElementStatisticsFa.fromAuthorative(cles);
        assertEquals(testJpaClElementStatistics, testJpaClElementStatisticsFa);
        testJpaClElementStatisticsFa.setKey(new PfTimestampKey("elementName", "0.0.1", new Date(123456L)));
        testJpaClElementStatisticsFa.fromAuthorative(cles);
        assertEquals(testJpaClElementStatistics, testJpaClElementStatisticsFa);

        testJpaClElementStatisticsFa = new JpaClElementStatistics(cles);
        assertEquals(testJpaClElementStatistics, testJpaClElementStatisticsFa);

        assertEquals(1, testJpaClElementStatistics.getKeys().size());

        assertEquals("elementName", testJpaClElementStatistics.getKey().getName());

        testJpaClElementStatistics.clean();
        assertEquals("elementName", testJpaClElementStatistics.getKey().getName());

        JpaClElementStatistics testJpaClElementStatistics2 = new JpaClElementStatistics(testJpaClElementStatistics);
        assertEquals(testJpaClElementStatistics, testJpaClElementStatistics2);
    }

    @Test
    public void testJpaClElementStatisticsValidation() {
        JpaClElementStatistics testJpaClElementStatistics = createJpaClElementStatisticsInstance();

        assertThatThrownBy(() -> {
            testJpaClElementStatistics.validate(null);
        }).hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaClElementStatistics.validate("").isValid());
    }

    @Test
    public void testJpaClElementStatisticsConmpareTo() {
        JpaClElementStatistics testJpaClElementStatistics = createJpaClElementStatisticsInstance();

        JpaClElementStatistics otherJpaClElementStatistics = new JpaClElementStatistics(testJpaClElementStatistics);
        assertEquals(0, testJpaClElementStatistics.compareTo(otherJpaClElementStatistics));
        assertEquals(-1, testJpaClElementStatistics.compareTo(null));
        assertEquals(0, testJpaClElementStatistics.compareTo(testJpaClElementStatistics));
        assertNotEquals(0, testJpaClElementStatistics.compareTo(new DummyJpaClElementStatisticsChild()));

        testJpaClElementStatistics.setState(ControlLoopState.PASSIVE);
        assertNotEquals(0, testJpaClElementStatistics.compareTo(otherJpaClElementStatistics));
        testJpaClElementStatistics.setState(ControlLoopState.UNINITIALISED);
        assertEquals(0, testJpaClElementStatistics.compareTo(otherJpaClElementStatistics));

        assertEquals(testJpaClElementStatistics, new JpaClElementStatistics(testJpaClElementStatistics));
    }

    @Test
    public void testJpaClElementStatisticsLombok() {
        assertNotNull(new Participant());
        JpaClElementStatistics p0 = new JpaClElementStatistics();

        assertThat(p0.toString()).contains("JpaClElementStatistics(");
        assertEquals(false, p0.hashCode() == 0);
        assertEquals(true, p0.equals(p0));
        assertEquals(false, p0.equals(null));


        JpaClElementStatistics p1 = new JpaClElementStatistics();

        p1.setState(ControlLoopState.UNINITIALISED);

        assertThat(p1.toString()).contains("JpaClElementStatistics(");
        assertEquals(false, p1.hashCode() == 0);
        assertEquals(false, p1.equals(p0));
        assertEquals(false, p1.equals(null));

        assertNotEquals(p1, p0);

        JpaClElementStatistics p2 = new JpaClElementStatistics();
        assertEquals(p2, p0);
    }

    private JpaClElementStatistics createJpaClElementStatisticsInstance() {
        ClElementStatistics testCles = createClElementStatisticsInstance();
        JpaClElementStatistics testJpaClElementStatistics = new JpaClElementStatistics();
        testJpaClElementStatistics.setKey(null);
        testJpaClElementStatistics.fromAuthorative(testCles);
        testJpaClElementStatistics.setKey(PfTimestampKey.getNullKey());
        testJpaClElementStatistics.fromAuthorative(testCles);

        return testJpaClElementStatistics;
    }

    private ClElementStatistics createClElementStatisticsInstance() {
        ClElementStatistics clElementStatistics = new ClElementStatistics();
        clElementStatistics.setControlLoopElementId(new ToscaConceptIdentifier("elementName", "0.0.1"));
        clElementStatistics.setTimeStamp(new Date(123456L));
        clElementStatistics.setControlLoopState(ControlLoopState.UNINITIALISED);

        return clElementStatistics;
    }
}
