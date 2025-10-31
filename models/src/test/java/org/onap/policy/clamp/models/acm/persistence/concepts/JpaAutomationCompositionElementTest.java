/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the{@link JpaAutomationCompositionElement} class.
 */
class JpaAutomationCompositionElementTest {

    private static final String NULL_INSTANCE_ID_ERROR = "instanceId is marked .*ull but is null";
    private static final String NULL_ELEMENT_ID_ERROR = "elementId is marked .*ull but is null";
    private static final String NULL_ERROR = " is marked .*ull but is null";
    private static final String ELEMENT_ID = "a95757ba-b34a-4049-a2a8-46773abcbe5e";
    private static final String INSTANCE_ID = "a78757co-b34a-8949-a2a8-46773abcbe2a";
    private static final String KEY = "key";
    private static final String BAD_VALUE = "BadValue";

    @Test
    void testJpaAutomationCompositionElementConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement((AutomationCompositionElement) null);
        }).hasMessageMatching("authorativeConcept" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement((JpaAutomationCompositionElement) null);
        }).hasMessageMatching("copyConcept" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(KEY, null);
        }).hasMessageMatching(NULL_INSTANCE_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, KEY);
        }).hasMessageMatching(NULL_ELEMENT_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, null);
        }).hasMessageMatching(NULL_ELEMENT_ID_ERROR);

        assertDoesNotThrow(() -> new JpaAutomationCompositionElement(KEY, KEY));
    }

    @Test
    void testJpaAutomationCompositionElement() {
        var testJpaAcElement = createJpaAutomationCompositionElementInstance();

        var ace = createAutomationCompositionElementInstance();
        assertEquals(ace, testJpaAcElement.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaAcElement.fromAuthorative(null);
        }).hasMessageMatching("element" + NULL_ERROR);

        assertThatThrownBy(() -> new JpaAutomationCompositionElement((JpaAutomationCompositionElement) null))
                .isInstanceOf(NullPointerException.class);

        var testJpaAcElementFa =
                new JpaAutomationCompositionElement(ace.getId().toString(), testJpaAcElement.getInstanceId());
        testJpaAcElementFa.fromAuthorative(ace);
        assertEquals(testJpaAcElement, testJpaAcElementFa);

        assertEquals(ELEMENT_ID, testJpaAcElement.getElementId());

        var testJpaAcElement2 = new JpaAutomationCompositionElement(testJpaAcElement);
        assertEquals(testJpaAcElement, testJpaAcElement2);

        testJpaAcElement2 = new JpaAutomationCompositionElement(testJpaAcElement.toAuthorative());
        testJpaAcElement2.setElementId(ELEMENT_ID);
        testJpaAcElement2.setInstanceId(INSTANCE_ID);
        assertEquals(testJpaAcElement, testJpaAcElement2);
    }

    @Test
    void testJpaAutomationCompositionElementValidation() {
        var testJpaAutomationCompositionElement = createJpaAutomationCompositionElementInstance();

        assertThatThrownBy(() -> testJpaAutomationCompositionElement.validate(null))
                .hasMessageMatching("fieldName" + NULL_ERROR);

        assertTrue(testJpaAutomationCompositionElement.validate("").isValid());
    }

    @Test
    void testJpaAcElementCompareTo() {
        var testJpaAcElement = createJpaAutomationCompositionElementInstance();

        var otherJpaAcElement =
                new JpaAutomationCompositionElement(testJpaAcElement);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        assertEquals(-1, testJpaAcElement.compareTo(null));
        assertEquals(0, testJpaAcElement.compareTo(testJpaAcElement));
        assertNotEquals(0,
                testJpaAcElement.compareTo(new DummyJpaAutomationCompositionElementChild()));

        assertEquals(testJpaAcElement, new JpaAutomationCompositionElement(testJpaAcElement));
    }

    @Test
    void testJpaAutomationCompositionElementCompareTo() {
        var testJpaAcElement = createJpaAutomationCompositionElementInstance();

        var otherJpaAcElement =
                new JpaAutomationCompositionElement(testJpaAcElement);

        testJpaAcElement.setElementId(BAD_VALUE);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setElementId(ELEMENT_ID);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setInstanceId(BAD_VALUE);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setInstanceId(INSTANCE_ID);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setDefinition(new PfConceptKey(BAD_VALUE, "0.0.1"));
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setDefinition(new PfConceptKey("aceDef", "0.0.1"));
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setDescription("Description");
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setDescription(null);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setDeployState(DeployState.DEPLOYED);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setDeployState(DeployState.UNDEPLOYED);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setLockState(LockState.UNLOCKED);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setLockState(LockState.LOCKED);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setSubState(SubState.PREPARING);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setSubState(SubState.NONE);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setMigrationState(MigrationState.REMOVED);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setMigrationState(MigrationState.DEFAULT);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setUseState(BAD_VALUE);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setUseState("IDLE");
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setOperationalState(BAD_VALUE);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setOperationalState("DEFAULT");
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setStage(1);
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setStage(null);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setMessage("Message");
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));
        testJpaAcElement.setMessage(null);
        assertEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

        testJpaAcElement.setParticipantId(UUID.randomUUID().toString());
        assertNotEquals(0, testJpaAcElement.compareTo(otherJpaAcElement));

    }

    @Test
    void testJpaAutomationCompositionElementLombok() {
        var ace0 = new JpaAutomationCompositionElement();

        assertThat(ace0.toString()).contains("JpaAutomationCompositionElement(");
        assertThat(ace0.hashCode()).isNotZero();
        assertNotEquals(null, ace0);

        var ace1 = new JpaAutomationCompositionElement(ace0.getElementId(), ace0.getInstanceId());

        ace1.setDefinition(new PfConceptKey("defName", "0.0.1"));
        ace1.setDescription("Description");
        ace1.setParticipantId(CommonTestData.getJpaParticipantId());

        assertThat(ace1.toString()).contains("AutomationCompositionElement(");
        assertNotEquals(0, ace1.hashCode());
        assertNotEquals(ace1, ace0);
        assertNotEquals(null, ace1);

        assertNotEquals(ace1, ace0);

        var ace2 = new JpaAutomationCompositionElement(ace0.getElementId(), ace0.getInstanceId());
        assertEquals(ace2, ace0);
    }

    private JpaAutomationCompositionElement createJpaAutomationCompositionElementInstance() {
        var testAce = createAutomationCompositionElementInstance();
        var testJpaAcElement =
                new JpaAutomationCompositionElement(testAce.getId().toString(), INSTANCE_ID);
        testJpaAcElement.fromAuthorative(testAce);
        testJpaAcElement.setProperties(Map.of(KEY, "{}"));

        return testJpaAcElement;
    }

    private AutomationCompositionElement createAutomationCompositionElementInstance() {
        var automationCompositionElement = new AutomationCompositionElement();
        automationCompositionElement.setId(UUID.fromString(ELEMENT_ID));
        automationCompositionElement.setDefinition(new ToscaConceptIdentifier("aceDef", "0.0.1"));
        automationCompositionElement.setParticipantId(CommonTestData.getParticipantId());
        automationCompositionElement.setProperties(Map.of(KEY, "{}"));
        automationCompositionElement.setUseState("IDLE");
        automationCompositionElement.setOperationalState("DEFAULT");

        return automationCompositionElement;
    }
}
