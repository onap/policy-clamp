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
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfReferenceKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaAutomationCompositionElement} class.
 */
class JpaAutomationCompositionElementTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    void testJpaAutomationCompositionElementConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement((JpaAutomationCompositionElement) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement((PfReferenceKey) null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, null, null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, null, null, AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, null, new PfConceptKey("participant", "0.0.1"), null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, null, new PfConceptKey("participant", "0.0.1"),
                AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, new PfConceptKey(), null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, new PfConceptKey(), null,
                AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, new PfConceptKey(), new PfConceptKey("participant", "0.0.1"),
                null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(null, new PfConceptKey(), new PfConceptKey("participant", "0.0.1"),
                AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(new PfReferenceKey(), null, null, null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(new PfReferenceKey(), null, null,
                AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(new PfReferenceKey(), null, new PfConceptKey("participant", "0.0.1"),
                null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(new PfReferenceKey(), null, new PfConceptKey("participant", "0.0.1"),
                AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(new PfReferenceKey(), new PfConceptKey(), null, null);
        }).hasMessageMatching("participantType is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(new PfReferenceKey(), new PfConceptKey(), null,
                AutomationCompositionState.UNINITIALISED);
        }).hasMessageMatching("participantType is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionElement(new PfReferenceKey(), new PfConceptKey(),
                new PfConceptKey("participant", "0.0.1"), null);
        }).hasMessageMatching("state is marked .*ull but is null");

        assertNotNull(new JpaAutomationCompositionElement());
        assertNotNull(new JpaAutomationCompositionElement((new PfReferenceKey())));
        assertNotNull(new JpaAutomationCompositionElement(new PfReferenceKey(), new PfConceptKey(),
            new PfConceptKey("participant", "0.0.1"), AutomationCompositionState.UNINITIALISED));
    }

    @Test
    void testJpaAutomationCompositionElement() {
        var testJpaAutomationCompositionElement =
            createJpaAutomationCompositionElementInstance();

        var ace = createAutomationCompositionElementInstance();
        assertEquals(ace, testJpaAutomationCompositionElement.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaAutomationCompositionElement.fromAuthorative(null);
        }).hasMessageMatching("element is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaAutomationCompositionElement((JpaAutomationCompositionElement) null))
            .isInstanceOf(NullPointerException.class);

        var testJpaAutomationCompositionElementFa = new JpaAutomationCompositionElement();
        testJpaAutomationCompositionElementFa.setKey(null);
        testJpaAutomationCompositionElementFa.fromAuthorative(ace);
        assertEquals(testJpaAutomationCompositionElement, testJpaAutomationCompositionElementFa);
        testJpaAutomationCompositionElementFa.setKey(PfReferenceKey.getNullKey());
        testJpaAutomationCompositionElementFa.fromAuthorative(ace);
        assertEquals(testJpaAutomationCompositionElement, testJpaAutomationCompositionElementFa);
        testJpaAutomationCompositionElementFa.setKey(
            new PfReferenceKey(PfKey.NULL_KEY_NAME, PfKey.NULL_KEY_VERSION, "a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        testJpaAutomationCompositionElementFa.fromAuthorative(ace);
        assertEquals(testJpaAutomationCompositionElement, testJpaAutomationCompositionElementFa);

        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e",
            testJpaAutomationCompositionElement.getKey().getLocalName());
        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e",
            new JpaAutomationCompositionElement(createAutomationCompositionElementInstance()).getKey().getLocalName());
        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e",
            ((PfReferenceKey) new JpaAutomationCompositionElement(createAutomationCompositionElementInstance())
                .getKeys().get(0)).getLocalName());

        testJpaAutomationCompositionElement.clean();
        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e",
            testJpaAutomationCompositionElement.getKey().getLocalName());

        testJpaAutomationCompositionElement.setDescription(" A Message ");
        testJpaAutomationCompositionElement.clean();
        assertEquals("A Message", testJpaAutomationCompositionElement.getDescription());

        var testJpaAutomationCompositionElement2 =
            new JpaAutomationCompositionElement(testJpaAutomationCompositionElement);
        assertEquals(testJpaAutomationCompositionElement, testJpaAutomationCompositionElement2);
    }

    @Test
    void testJpaAutomationCompositionElementOrderedState() throws CoderException {
        var testAutomationCompositionElement = createAutomationCompositionElementInstance();
        var testJpaAutomationCompositionElement =
            createJpaAutomationCompositionElementInstance();

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
        assertEquals(testJpaAutomationCompositionElement, noOrderedStateJpaAce);
    }

    @Test
    void testJpaAutomationCompositionElementValidation() {
        var testJpaAutomationCompositionElement =
            createJpaAutomationCompositionElementInstance();

        assertThatThrownBy(() -> testJpaAutomationCompositionElement.validate(null))
            .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaAutomationCompositionElement.validate("").isValid());
    }

    @Test
    void testJpaAutomationCompositionElementCompareTo() {
        var testJpaAutomationCompositionElement =
            createJpaAutomationCompositionElementInstance();

        var otherJpaAutomationCompositionElement =
            new JpaAutomationCompositionElement(testJpaAutomationCompositionElement);
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        assertEquals(-1, testJpaAutomationCompositionElement.compareTo(null));
        assertEquals(0, testJpaAutomationCompositionElement.compareTo(testJpaAutomationCompositionElement));
        assertNotEquals(0,
            testJpaAutomationCompositionElement.compareTo(new DummyJpaAutomationCompositionElementChild()));

        testJpaAutomationCompositionElement
            .setKey(new PfReferenceKey("BadValue", "0.0.1", "a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        assertNotEquals(0, testJpaAutomationCompositionElement.compareTo(otherJpaAutomationCompositionElement));
        testJpaAutomationCompositionElement.setKey(
            new PfReferenceKey(PfKey.NULL_KEY_NAME, PfKey.NULL_KEY_VERSION, "a95757ba-b34a-4049-a2a8-46773abcbe5e"));
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

        var ace1 = new JpaAutomationCompositionElement();

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

        var ace2 = new JpaAutomationCompositionElement();
        assertEquals(ace2, ace0);
    }

    private JpaAutomationCompositionElement createJpaAutomationCompositionElementInstance() {
        var testAce = createAutomationCompositionElementInstance();
        var testJpaAutomationCompositionElement = new JpaAutomationCompositionElement();
        testJpaAutomationCompositionElement.setKey(null);
        testJpaAutomationCompositionElement.fromAuthorative(testAce);
        testJpaAutomationCompositionElement.setKey(PfReferenceKey.getNullKey());
        testJpaAutomationCompositionElement.fromAuthorative(testAce);
        testJpaAutomationCompositionElement.setProperties(Map.of("key", "{}"));

        return testJpaAutomationCompositionElement;
    }

    private AutomationCompositionElement createAutomationCompositionElementInstance() {
        var automationCompositionElement = new AutomationCompositionElement();
        automationCompositionElement.setId(UUID.fromString("a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        automationCompositionElement.setDefinition(new ToscaConceptIdentifier("aceDef", "0.0.1"));
        automationCompositionElement.setParticipantType(new ToscaConceptIdentifier("participantType", "0.0.1"));
        automationCompositionElement.setProperties(Map.of("key", "{}"));

        return automationCompositionElement;
    }
}
