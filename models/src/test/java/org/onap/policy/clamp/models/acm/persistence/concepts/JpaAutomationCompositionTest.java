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
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfConceptKey;

/**
 * Test the{@link JpaAutomationCompositionTest} class.
 */
class JpaAutomationCompositionTest {

    private static final String NULL_INSTANCE_ID_ERROR = "instanceId is marked .*ull but is null";
    private static final String NULL_TEXT_ERROR = " is marked .*ull but is null";
    private static final String INSTANCE_ID = "709c62b3-8918-41b9-a747-d21eb79c6c20";
    private static final String COMPOSITION_ID = "709c62b3-8918-41b9-a747-e21eb79c6c41";

    @Test
    void testJpaAutomationCompositionConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationComposition((JpaAutomationComposition) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(null, null, null, null, null);
        }).hasMessageMatching(NULL_INSTANCE_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, null, null, null, new ArrayList<>());
        }).hasMessageMatching("key" + NULL_TEXT_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), null,
                    AutomationCompositionState.UNINITIALISED, null);
        }).hasMessageMatching("compositionId" + NULL_TEXT_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID.toString(), null, null);
        }).hasMessageMatching("state" + NULL_TEXT_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID.toString(),
                    AutomationCompositionState.UNINITIALISED, null);
        }).hasMessageMatching("elements" + NULL_TEXT_ERROR);

        assertNotNull(new JpaAutomationComposition());
        assertNotNull(new JpaAutomationComposition(INSTANCE_ID, new PfConceptKey(), COMPOSITION_ID.toString(),
                AutomationCompositionState.UNINITIALISED, new ArrayList<>()));
    }

    @Test
    void testJpaAutomationComposition() {
        var testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        var participant = createAutomationCompositionInstance();
        assertEquals(participant, testJpaAutomationComposition.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaAutomationComposition.fromAuthorative(null);
        }).hasMessageMatching("automationComposition is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaAutomationComposition((JpaAutomationComposition) null))
                .isInstanceOf(NullPointerException.class);

        var testJpaAutomationCompositionFa = new JpaAutomationComposition();
        testJpaAutomationCompositionFa.setInstanceId(null);
        testJpaAutomationCompositionFa.fromAuthorative(participant);
        assertEquals(testJpaAutomationComposition, testJpaAutomationCompositionFa);

        assertEquals("automation-composition", testJpaAutomationComposition.getName());
        assertEquals("automation-composition",
                new JpaAutomationComposition(createAutomationCompositionInstance()).getName());

        var testJpaAutomationComposition2 = new JpaAutomationComposition(testJpaAutomationComposition);
        assertEquals(testJpaAutomationComposition, testJpaAutomationComposition2);
    }

    @Test
    void testJpaAutomationCompositionElementOrderedState() throws CoderException {
        var testAutomationComposition = createAutomationCompositionInstance();
        var testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        testJpaAutomationComposition.setOrderedState(null);
        assertEquals(testAutomationComposition, testJpaAutomationComposition.toAuthorative());
        testJpaAutomationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);

        var noOrderedStateAc =
                new StandardCoder().decode(new File("src/test/resources/json/AutomationCompositionNoOrderedState.json"),
                        AutomationComposition.class);

        noOrderedStateAc.setInstanceId(UUID.fromString(INSTANCE_ID));
        var noOrderedStateJpaAc = new JpaAutomationComposition(noOrderedStateAc);
        assertNull(noOrderedStateJpaAc.getOrderedState());
        noOrderedStateAc.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        noOrderedStateJpaAc = new JpaAutomationComposition(noOrderedStateAc);
        assertEquals(testJpaAutomationComposition, noOrderedStateJpaAc);

        var acWithElements =
                new StandardCoder().decode(new File("src/test/resources/providers/TestAutomationCompositions.json"),
                        AutomationCompositions.class).getAutomationCompositionList().get(0);

        acWithElements.setInstanceId(UUID.fromString(INSTANCE_ID));
        var jpaAutomationCompositionWithElements = new JpaAutomationComposition(acWithElements);
        assertEquals(4, jpaAutomationCompositionWithElements.getElements().size());
        assertEquals(acWithElements, jpaAutomationCompositionWithElements.toAuthorative());
    }

    @Test
    void testJpaAutomationCompositionValidation() {
        var testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        assertThatThrownBy(() -> testJpaAutomationComposition.validate(null))
                .hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaAutomationComposition.validate("").isValid());
    }

    @Test
    void testJpaAutomationCompositionCompareTo() {
        var testJpaAutomationComposition = createJpaAutomationCompositionInstance();

        var otherJpaAutomationComposition = new JpaAutomationComposition(testJpaAutomationComposition);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        assertEquals(-1, testJpaAutomationComposition.compareTo(null));
        assertEquals(0, testJpaAutomationComposition.compareTo(testJpaAutomationComposition));
        assertNotEquals(0, testJpaAutomationComposition.compareTo(new DummyJpaAutomationCompositionChild()));

        testJpaAutomationComposition.setInstanceId("BadValue");
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setInstanceId(INSTANCE_ID);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setCompositionId(UUID.randomUUID().toString());
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setCompositionId(COMPOSITION_ID);
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setName("BadValue");
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setName("automation-composition");
        assertEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));

        testJpaAutomationComposition.setVersion("0.0.0");
        assertNotEquals(0, testJpaAutomationComposition.compareTo(otherJpaAutomationComposition));
        testJpaAutomationComposition.setVersion("0.0.1");
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
        var ac0 = new JpaAutomationComposition();
        ac0.setCompositionId(COMPOSITION_ID);

        assertThat(ac0.toString()).contains("JpaAutomationComposition(");
        assertThat(ac0.hashCode()).isNotZero();
        assertEquals(ac0, ac0);
        assertNotEquals(null, ac0);

        var ac1 = new JpaAutomationComposition();

        ac1.setCompositionId(UUID.randomUUID().toString());
        ac1.setDescription("Description");
        ac1.setElements(new ArrayList<>());
        ac1.setInstanceId(INSTANCE_ID);
        ac1.setState(AutomationCompositionState.UNINITIALISED);

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

    private JpaAutomationComposition createJpaAutomationCompositionInstance() {
        var testAutomationComposition = createAutomationCompositionInstance();
        var testJpaAutomationComposition = new JpaAutomationComposition();
        testJpaAutomationComposition.fromAuthorative(testAutomationComposition);

        return testJpaAutomationComposition;
    }

    private AutomationComposition createAutomationCompositionInstance() {
        var testAutomationComposition = new AutomationComposition();
        testAutomationComposition.setName("automation-composition");
        testAutomationComposition.setInstanceId(UUID.fromString(INSTANCE_ID));
        testAutomationComposition.setVersion("0.0.1");
        testAutomationComposition.setCompositionId(UUID.fromString(COMPOSITION_ID));
        testAutomationComposition.setElements(new LinkedHashMap<>());

        return testAutomationComposition;
    }
}
