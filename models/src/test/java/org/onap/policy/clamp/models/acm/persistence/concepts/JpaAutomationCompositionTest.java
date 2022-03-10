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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaAutomationCompositionTest} class.
 */
class JpaAutomationCompositionTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    void testJpaAutomationCompositionConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationComposition((JpaAutomationComposition) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition((PfConceptKey) null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, null, null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, null, null, new LinkedHashMap<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, null, AutomationCompositionState.UNINITIALISED, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, null, AutomationCompositionState.UNINITIALISED, new LinkedHashMap<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, new PfConceptKey(), null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, new PfConceptKey(), null, new LinkedHashMap<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, new PfConceptKey(), AutomationCompositionState.UNINITIALISED, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, new PfConceptKey(), AutomationCompositionState.UNINITIALISED,
                new LinkedHashMap<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(new PfConceptKey(), null, null, null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(new PfConceptKey(), null, null, new LinkedHashMap<>());
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(new PfConceptKey(), null, AutomationCompositionState.UNINITIALISED, null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(new PfConceptKey(), null, AutomationCompositionState.UNINITIALISED,
                new LinkedHashMap<>());
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(new PfConceptKey(), new PfConceptKey(), null, null);
        }).hasMessageMatching("state is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(new PfConceptKey(), new PfConceptKey(), null, new LinkedHashMap<>());
        }).hasMessageMatching("state is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(new PfConceptKey(), new PfConceptKey(),
                AutomationCompositionState.UNINITIALISED, null);
        }).hasMessageMatching("elements is marked .*ull but is null");

        assertNotNull(new JpaAutomationComposition());
        assertNotNull(new JpaAutomationComposition((new PfConceptKey())));
        assertNotNull(new JpaAutomationComposition(new PfConceptKey(), new PfConceptKey(),
            AutomationCompositionState.UNINITIALISED, new LinkedHashMap<>()));
    }

    @Test
    void testJpaAutomationComposition() {
        JpaAutomationComposition testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        AutomationComposition participant = createAutomationCompositionInstance();
        assertEquals(participant, testJpaAutomationComposition.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaAutomationComposition.fromAuthorative(null);
        }).hasMessageMatching("automationComposition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaAutomationComposition((JpaAutomationComposition) null))
            .isInstanceOf(NullPointerException.class);

        JpaAutomationComposition testJpaAutomationCompositionFa = new JpaAutomationComposition();
        testJpaAutomationCompositionFa.setKey(null);
        testJpaAutomationCompositionFa.fromAuthorative(participant);
        assertEquals(testJpaAutomationComposition, testJpaAutomationCompositionFa);
        testJpaAutomationCompositionFa.setKey(PfConceptKey.getNullKey());
        testJpaAutomationCompositionFa.fromAuthorative(participant);
        assertEquals(testJpaAutomationComposition, testJpaAutomationCompositionFa);
        testJpaAutomationCompositionFa.setKey(new PfConceptKey("automation-composition", "0.0.1"));
        testJpaAutomationCompositionFa.fromAuthorative(participant);
        assertEquals(testJpaAutomationComposition, testJpaAutomationCompositionFa);

        assertEquals("automation-composition", testJpaAutomationComposition.getKey().getName());
        assertEquals("automation-composition",
            new JpaAutomationComposition(createAutomationCompositionInstance()).getKey().getName());
        assertEquals("automation-composition",
            ((PfConceptKey) new JpaAutomationComposition(createAutomationCompositionInstance()).getKeys().get(0))
                .getName());

        testJpaAutomationComposition.clean();
        assertEquals("automation-composition", testJpaAutomationComposition.getKey().getName());

        testJpaAutomationComposition.setDescription("   A Message   ");
        testJpaAutomationComposition.clean();
        assertEquals("A Message", testJpaAutomationComposition.getDescription());

        JpaAutomationComposition testJpaAutomationComposition2 =
            new JpaAutomationComposition(testJpaAutomationComposition);
        assertEquals(testJpaAutomationComposition, testJpaAutomationComposition2);
    }

    @Test
    void testJpaAutomationCompositionElementOrderedState() throws CoderException {
        AutomationComposition testAutomationComposition = createAutomationCompositionInstance();
        JpaAutomationComposition testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        testJpaAutomationComposition.setOrderedState(null);
        assertEquals(testAutomationComposition, testJpaAutomationComposition.toAuthorative());
        testJpaAutomationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);

        AutomationComposition noOrderedStateAc = new StandardCoder().decode(
            new File("src/test/resources/json/AutomationCompositionNoOrderedState.json"), AutomationComposition.class);

        JpaAutomationComposition noOrderedStateJpaAc = new JpaAutomationComposition(noOrderedStateAc);
        assertNull(noOrderedStateJpaAc.getOrderedState());
        noOrderedStateAc.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        noOrderedStateJpaAc = new JpaAutomationComposition(noOrderedStateAc);
        assertEquals(testJpaAutomationComposition, noOrderedStateJpaAc);

        AutomationCompositions automationCompositionsWithElements = new StandardCoder().decode(
            new File("src/test/resources/providers/TestAutomationCompositions.json"), AutomationCompositions.class);

        JpaAutomationComposition jpaAutomationCompositionWithElements =
            new JpaAutomationComposition(automationCompositionsWithElements.getAutomationCompositionList().get(0));
        assertEquals(4, jpaAutomationCompositionWithElements.getElements().size());
        assertEquals(18, jpaAutomationCompositionWithElements.getKeys().size());
        assertThatCode(jpaAutomationCompositionWithElements::clean).doesNotThrowAnyException();

        assertEquals(automationCompositionsWithElements.getAutomationCompositionList().get(0),
            jpaAutomationCompositionWithElements.toAuthorative());
    }

    @Test
    void testJpaAutomationCompositionValidation() {
        JpaAutomationComposition testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        assertThatThrownBy(() -> testJpaAutomationComposition.validate(null))
            .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaAutomationComposition.validate("").isValid());
    }

    @Test
    void testJpaAutomationCompositionCompareTo() {
        JpaAutomationComposition testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        JpaAutomationComposition otherJpaAutomationComposition =
            new JpaAutomationComposition(testJpaAutomationComposition);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        assertEquals(-1, testJpaAutomationComposition.compareTo(null));
        assertEquals(0, testJpaAutomationComposition.compareTo(testJpaAutomationComposition));
        assertNotEquals(0, testJpaAutomationComposition.compareTo(new DummyJpaAutomationCompositionChild()));

        testJpaAutomationComposition.setKey(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setKey(new PfConceptKey("automation-composition", "0.0.1"));
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setDefinition(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setDefinition(new PfConceptKey("automationCompositionDefinitionName", "0.0.1"));
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setState(AutomationCompositionState.PASSIVE);
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setState(AutomationCompositionState.UNINITIALISED);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setDescription("A description");
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setDescription(null);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setPrimed(true);
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setPrimed(false);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        assertEquals(testJpaAutomationComposition, new JpaAutomationComposition(testJpaAutomationComposition));
    }

    @Test
    void testJpaAutomationCompositionLombok() {
        assertNotNull(new AutomationComposition());
        JpaAutomationComposition ac0 = new JpaAutomationComposition();

        assertThat(ac0.toString()).contains("JpaAutomationComposition(");
        assertThat(ac0.hashCode()).isNotZero();
        assertEquals(ac0, ac0);
        assertNotEquals(null, ac0);

        JpaAutomationComposition ac1 = new JpaAutomationComposition();

        ac1.setDefinition(new PfConceptKey("defName", "0.0.1"));
        ac1.setDescription("Description");
        ac1.setElements(new LinkedHashMap<>());
        ac1.setKey(new PfConceptKey("participant", "0.0.1"));
        ac1.setState(AutomationCompositionState.UNINITIALISED);

        assertThat(ac1.toString()).contains("AutomationComposition(");
        assertNotEquals(0, ac1.hashCode());
        assertNotEquals(ac1, ac0);
        assertNotEquals(null, ac1);

        assertNotEquals(ac1, ac0);

        JpaAutomationComposition ac2 = new JpaAutomationComposition();
        assertEquals(ac2, ac0);
    }

    private JpaAutomationComposition createJpaAutomationCompositionInstance() {
        AutomationComposition testAutomationComposition = createAutomationCompositionInstance();
        JpaAutomationComposition testJpaAutomationComposition = new JpaAutomationComposition();
        testJpaAutomationComposition.setKey(null);
        testJpaAutomationComposition.fromAuthorative(testAutomationComposition);
        testJpaAutomationComposition.setKey(PfConceptKey.getNullKey());
        testJpaAutomationComposition.fromAuthorative(testAutomationComposition);

        return testJpaAutomationComposition;
    }

    private AutomationComposition createAutomationCompositionInstance() {
        AutomationComposition testAutomationComposition = new AutomationComposition();
        testAutomationComposition.setName("automation-composition");
        testAutomationComposition.setVersion("0.0.1");
        testAutomationComposition
            .setDefinition(new ToscaConceptIdentifier("automationCompositionDefinitionName", "0.0.1"));
        testAutomationComposition.setElements(new LinkedHashMap<>());

        return testAutomationComposition;
    }
}
