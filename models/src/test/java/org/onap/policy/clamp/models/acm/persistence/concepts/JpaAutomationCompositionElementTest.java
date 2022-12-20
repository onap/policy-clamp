/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
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

    @Test
    void testJpaAutomationCompositionElementConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement((JpaAutomationCompositionElement) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement("key", null);
        }).hasMessageMatching(NULL_INSTANCE_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, "key");
        }).hasMessageMatching(NULL_ELEMENT_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, null);
        }).hasMessageMatching(NULL_ELEMENT_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, null, null, null, null);
        }).hasMessageMatching(NULL_ELEMENT_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement("key", null, null, null, AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching(NULL_INSTANCE_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement("key", "key", null, new PfConceptKey("participant", "0.0.1"), null);
        }).hasMessageMatching("definition" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement("key", "key", new PfConceptKey(), null,
                    AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching("participantType" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement("key", "key", new PfConceptKey(), new PfConceptKey(), null);
        }).hasMessageMatching("state" + NULL_ERROR);

        assertNotNull(new JpaAutomationCompositionElement());
        assertNotNull(new JpaAutomationCompositionElement("key", "key"));
        assertNotNull(new JpaAutomationCompositionElement("key", "key", new PfConceptKey(),
                new PfConceptKey("participant", "0.0.1"), AutomationCompositionState.UNINITIALISED));
    }

    @Test
    void testJpaAutomationCompositionElement() {
        var testJpaAcElement = createJpaAutomationCompositionElementInstance();

        var ace = createAutomationCompositionElementInstance();
        assertEquals(ace, testJpaAcElement.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaAcElement.fromAuthorative(null);
        }).hasMessageMatching("element is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaAutomationCompositionElement((JpaAutomationCompositionElement) null))
                .isInstanceOf(NullPointerException.class);

        var testJpaAcElementFa =
                new JpaAutomationCompositionElement(ace.getId().toString(), testJpaAcElement.getInstanceId());
        testJpaAcElementFa.fromAuthorative(ace);
        assertEquals(testJpaAcElement, testJpaAcElementFa);

        assertEquals(ELEMENT_ID, testJpaAcElement.getElementId());

        var testJpaAutomationCompositionElement2 = new JpaAutomationCompositionElement(testJpaAcElement);
        assertEquals(testJpaAcElement, testJpaAutomationCompositionElement2);
    }

    @Test
    void testJpaAutomationCompositionElementOrderedState() throws CoderException {
        var testAutomationCompositionElement = createAutomationCompositionElementInstance();
        var testJpaAutomationCompositionElement = createJpaAutomationCompositionElementInstance();

        testJpaAutomationCompositionElement.setOrderedState(null);
        assertEquals(testAutomationCompositionElement, testJpaAutomationCompositionElement.toAuthorative());
        testJpaAutomationCompositionElement.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);

        var noOrderedStateAce = new StandardCoder().decode(
                new File("src/test/resources/json/AutomationCompositionElementNoOrderedState.json"),
                AutomationCompositionElement.class);

        var noOrderedStateJpaAce = new JpaAutomationCompositionElement(noOrderedStateAce);
        assertNull(noOrderedStateJpaAce.getOrderedState());
        noOrderedStateAce.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        noOrderedStateJpaAce = new JpaAutomationCompositionElement(noOrderedStateAce);
        noOrderedStateJpaAce.setInstanceId(testJpaAutomationCompositionElement.getInstanceId());
        noOrderedStateJpaAce.setElementId(testJpaAutomationCompositionElement.getElementId());
        assertEquals(testJpaAutomationCompositionElement, noOrderedStateJpaAce);
    }

    @Test
    void testJpaAutomationCompositionElementValidation() {
        var testJpaAutomationCompositionElement = createJpaAutomationCompositionElementInstance();

        assertThatThrownBy(() -> testJpaAutomationCompositionElement.validate(null))
                .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaAutomationCompositionElement.validate("").isValid());
    }

    @Test
    void testJpaAutomationCompositionElementCompareTo() {
        var testJpaAutomationCompositionElement = createJpaAutomationCompositionElementInstance();

        var otherJpaAutomationCompositionElement =
                new JpaAutomationCompositionElement(testJpaAutomationCompositionElement);
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        assertEquals(-1, testJpaAutomationCompositionElement.compareTo(null));
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(testJpaAutomationCompositionElement));
        assertNotEquals(0,
                testJpaAutomationCompositionElement.compareTo(new DummyJpaAutomationCompositionElementChild()));

        testJpaAutomationCompositionElement.setElementId("BadValue");
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setElementId(ELEMENT_ID);
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));

        testJpaAutomationCompositionElement.setInstanceId("BadValue");
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setInstanceId(INSTANCE_ID);
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));

        testJpaAutomationCompositionElement.setDefinition(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setDefinition(new PfConceptKey("aceDef", "0.0.1"));
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));

        testJpaAutomationCompositionElement.setDescription("Description");
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setDescription(null);
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));

        testJpaAutomationCompositionElement.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));

        testJpaAutomationCompositionElement.setState(AutomationCompositionState.PASSIVE);
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setState(AutomationCompositionState.UNINITIALISED);
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));

        testJpaAutomationCompositionElement.setParticipantType(new PfConceptKey("dummy", "0.0.1"));
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setParticipantType(new PfConceptKey("participantType", "0.0.1"));
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));

        assertEquals(testJpaAutomationCompositionElement,
                new JpaAutomationCompositionElement(testJpaAutomationCompositionElement));
    }

    @Test
    void testJpaAutomationCompositionElementLombok() {
        assertNotNull(new Participant());
        var ace0 = new JpaAutomationCompositionElement();

        assertThat(ace0.toString()).contains("JpaAutomationCompositionElement(");
        assertThat(ace0.hashCode()).isNotZero();
        assertEquals(ace0, ace0);
        assertNotEquals(null, ace0);

        var ace1 = new JpaAutomationCompositionElement(ace0.getElementId(), ace0.getInstanceId());

        ace1.setDefinition(new PfConceptKey("defName", "0.0.1"));
        ace1.setDescription("Description");
        ace1.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        ace1.setState(AutomationCompositionState.UNINITIALISED);
        ace1.setParticipantId(new PfConceptKey("participant", "0.0.1"));

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
        testJpaAcElement.setProperties(Map.of("key", "{}"));

        return testJpaAcElement;
    }

    private AutomationCompositionElement createAutomationCompositionElementInstance() {
        var automationCompositionElement = new AutomationCompositionElement();
        automationCompositionElement.setId(UUID.fromString(ELEMENT_ID));
        automationCompositionElement.setDefinition(new ToscaConceptIdentifier("aceDef", "0.0.1"));
        automationCompositionElement.setParticipantType(new ToscaConceptIdentifier("participantType", "0.0.1"));
        automationCompositionElement.setProperties(Map.of("key", "{}"));

        return automationCompositionElement;
    }
}
