/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.base.PfConceptKey;

/**
 * Test the{@link JpaAutomationCompositionTest} class.
 */
class JpaAutomationCompositionTest {

    private static final String NULL_INSTANCE_ID_ERROR = "instanceId is marked .*ull but is null";
    private static final String NULL_ERROR = " is marked .*ull but is null";
    private static final String INSTANCE_ID = "709c62b3-8918-41b9-a747-d21eb79c6c20";
    private static final String COMPOSITION_ID = "709c62b3-8918-41b9-a747-e21eb79c6c41";

    @Test
    void testJpaAutomationCompositionConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationComposition((JpaAutomationComposition) null);
        }).hasMessageMatching("copyConcept" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition((AutomationComposition) null);
        }).hasMessageMatching("authorativeConcept" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, null, null, null, null, null, null);
        }).hasMessageMatching(NULL_INSTANCE_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, null, null, new ArrayList<>(), DeployState.UNDEPLOYED,
                    LockState.LOCKED, SubState.NONE);
        }).hasMessageMatching("key" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), null, new ArrayList<>(),
                    DeployState.UNDEPLOYED, LockState.LOCKED, SubState.NONE);
        }).hasMessageMatching("compositionId" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID, null,
                    DeployState.UNDEPLOYED, LockState.LOCKED, SubState.NONE);
        }).hasMessageMatching("elements" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID, new ArrayList<>(),
                    null, LockState.LOCKED, SubState.NONE);
        }).hasMessageMatching("deployState" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID, new ArrayList<>(),
                    DeployState.UNDEPLOYED, null, SubState.NONE);
        }).hasMessageMatching("lockState" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID, new ArrayList<>(),
                    DeployState.UNDEPLOYED, LockState.NONE, null);
        }).hasMessageMatching("subState" + NULL_ERROR);

        assertDoesNotThrow(() -> new JpaAutomationComposition());
        assertDoesNotThrow(() -> new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID,
                new ArrayList<>(), DeployState.UNDEPLOYED, LockState.LOCKED, SubState.NONE));
    }

    @Test
    void testJpaAutomationComposition() {
        var automationComposition = createAutomationCompositionInstance();
        var jpaAutomationComposition = new JpaAutomationComposition(automationComposition);

        assertEquals(automationComposition, jpaAutomationComposition.toAuthorative());

        var target = UUID.randomUUID();
        jpaAutomationComposition.setCompositionTargetId(target.toString());
        automationComposition.setCompositionTargetId(target);
        assertEquals(automationComposition, jpaAutomationComposition.toAuthorative());

        assertThatThrownBy(() -> {
            jpaAutomationComposition.fromAuthorative(null);
        }).hasMessageMatching("automationComposition" + NULL_ERROR);

        assertThatThrownBy(() -> new JpaAutomationComposition((JpaAutomationComposition) null))
                .isInstanceOf(NullPointerException.class);

        var jpaAutomationCompositionFa = new JpaAutomationComposition();
        jpaAutomationCompositionFa.setInstanceId(null);
        jpaAutomationCompositionFa.fromAuthorative(automationComposition);
        assertEquals(jpaAutomationComposition, jpaAutomationCompositionFa);

        assertEquals("automation-composition", jpaAutomationComposition.getName());
        assertEquals("automation-composition",
                new JpaAutomationComposition(createAutomationCompositionInstance()).getName());

        var jpaAutomationComposition2 = new JpaAutomationComposition(jpaAutomationComposition);
        assertEquals(jpaAutomationComposition, jpaAutomationComposition2);
    }

    @Test
    void testJpaAutomationCompositionValidation() {
        var testJpaAutomationComposition = new JpaAutomationComposition(createAutomationCompositionInstance());

        assertThatThrownBy(() -> testJpaAutomationComposition.validate(null))
                .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaAutomationComposition.validate("").isValid());
    }

    @Test
    void testJpaAutomationCompositionCompareTo1() {
        var jpaAutomationComposition = new JpaAutomationComposition(createAutomationCompositionInstance());

        var otherJpaAutomationComposition = new JpaAutomationComposition(jpaAutomationComposition);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        assertEquals(-1, jpaAutomationComposition.compareTo(null));
        assertEquals(0, jpaAutomationComposition.compareTo(jpaAutomationComposition));
        assertNotEquals(0, jpaAutomationComposition.compareTo(new DummyJpaAutomationCompositionChild()));

        jpaAutomationComposition.setInstanceId("BadValue");
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setInstanceId(INSTANCE_ID);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setCompositionId(UUID.randomUUID().toString());
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setCompositionId(COMPOSITION_ID);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setCompositionTargetId(UUID.randomUUID().toString());
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setCompositionTargetId(null);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setName("BadValue");
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setName("automation-composition");
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setVersion("0.0.0");
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setVersion("0.0.1");
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setLastMsg(Timestamp.from(Instant.EPOCH));
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setLastMsg(otherJpaAutomationComposition.getLastMsg());
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setPhase(0);
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setPhase(null);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
    }

    @Test
    void testJpaAutomationCompositionCompareTo2() {
        var jpaAutomationComposition = new JpaAutomationComposition(createAutomationCompositionInstance());
        var otherJpaAutomationComposition = new JpaAutomationComposition(jpaAutomationComposition);

        jpaAutomationComposition.setDeployState(DeployState.DEPLOYED);
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setDeployState(DeployState.UNDEPLOYED);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setLockState(LockState.UNLOCKED);
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setLockState(LockState.NONE);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setSubState(SubState.PREPARING);
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setSubState(SubState.NONE);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setDescription("A description");
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setDescription(null);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setRestarting(true);
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setRestarting(null);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        jpaAutomationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        assertNotEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        jpaAutomationComposition.setStateChangeResult(null);
        assertEquals(0, jpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        assertEquals(jpaAutomationComposition, new JpaAutomationComposition(jpaAutomationComposition));
    }

    @Test
    void testJpaAutomationCompositionLombok() {
        var ac0 = new JpaAutomationComposition();
        ac0.setCompositionId(COMPOSITION_ID);

        assertThat(ac0.toString()).contains("JpaAutomationComposition(");
        assertThat(ac0.hashCode()).isNotZero();
        assertNotEquals(null, ac0);

        var ac1 = new JpaAutomationComposition();

        ac1.setCompositionId(UUID.randomUUID().toString());
        ac1.setDescription("Description");
        ac1.setElements(new ArrayList<>());
        ac1.setInstanceId(INSTANCE_ID);

        assertThat(ac1.toString()).contains("AutomationComposition(");
        assertNotEquals(0, ac1.hashCode());
        assertNotEquals(ac1, ac0);
        assertNotEquals(null, ac1);

        assertNotEquals(ac1, ac0);

        var ac2 = new JpaAutomationComposition();
        ac2.setCompositionId(COMPOSITION_ID);
        ac2.setInstanceId(ac0.getInstanceId());
        assertEquals(ac2, ac0);
    }

    private AutomationComposition createAutomationCompositionInstance() {
        var testAutomationComposition = new AutomationComposition();
        testAutomationComposition.setName("automation-composition");
        testAutomationComposition.setInstanceId(UUID.fromString(INSTANCE_ID));
        testAutomationComposition.setVersion("0.0.1");
        testAutomationComposition.setLastMsg(TimestampHelper.now());
        testAutomationComposition.setCompositionId(UUID.fromString(COMPOSITION_ID));
        testAutomationComposition.setElements(new LinkedHashMap<>());

        return testAutomationComposition;
    }
}
