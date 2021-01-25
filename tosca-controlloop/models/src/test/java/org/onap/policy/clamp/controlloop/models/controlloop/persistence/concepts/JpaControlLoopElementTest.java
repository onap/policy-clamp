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

import java.io.File;
import java.util.UUID;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfReferenceKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaControlLoopElement} class.
 */
public class JpaControlLoopElementTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    public void testJpaControlLoopElementConstructor() {
        assertThatThrownBy(() -> {
            new JpaControlLoopElement((JpaControlLoopElement) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoopElement((PfReferenceKey) null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, null, null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, null, null, ControlLoopState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, null, new PfConceptKey("participant", "0.0.1"), null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, null, new PfConceptKey("participant", "0.0.1"),
                    ControlLoopState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, new PfConceptKey(), null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, new PfConceptKey(), null, ControlLoopState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, new PfConceptKey(), new PfConceptKey("participant", "0.0.1"), null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(null, new PfConceptKey(), new PfConceptKey("participant", "0.0.1"),
                    ControlLoopState.UNINITIALISED);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(new PfReferenceKey(), null, null, null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(new PfReferenceKey(), null, null, ControlLoopState.UNINITIALISED);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(new PfReferenceKey(), null, new PfConceptKey("participant", "0.0.1"), null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(new PfReferenceKey(), null, new PfConceptKey("participant", "0.0.1"),
                    ControlLoopState.UNINITIALISED);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(new PfReferenceKey(), new PfConceptKey(), null, null);
        }).hasMessageMatching("participantId is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(new PfReferenceKey(), new PfConceptKey(), null, ControlLoopState.UNINITIALISED);
        }).hasMessageMatching("participantId is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoopElement(new PfReferenceKey(), new PfConceptKey(),
                    new PfConceptKey("participant", "0.0.1"), null);
        }).hasMessageMatching("state is marked .*ull but is null");

        assertNotNull(new JpaControlLoopElement());
        assertNotNull(new JpaControlLoopElement((new PfReferenceKey())));
        assertNotNull(new JpaControlLoopElement(new PfReferenceKey(), new PfConceptKey(),
                new PfConceptKey("participant", "0.0.1"), ControlLoopState.UNINITIALISED));
    }

    @Test
    public void testJpaControlLoopElement() {
        JpaControlLoopElement testJpaControlLoopElement = createJpaControlLoopElementInstance();

        ControlLoopElement cle = createControlLoopElementInstance();
        assertEquals(cle, testJpaControlLoopElement.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaControlLoopElement.fromAuthorative(null);
        }).hasMessageMatching("element is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaControlLoopElement((JpaControlLoopElement) null))
                .isInstanceOf(NullPointerException.class);

        JpaControlLoopElement testJpaControlLoopElementFa = new JpaControlLoopElement();
        testJpaControlLoopElementFa.setKey(null);
        testJpaControlLoopElementFa.fromAuthorative(cle);
        assertEquals(testJpaControlLoopElement, testJpaControlLoopElementFa);
        testJpaControlLoopElementFa.setKey(PfReferenceKey.getNullKey());
        testJpaControlLoopElementFa.fromAuthorative(cle);
        assertEquals(testJpaControlLoopElement, testJpaControlLoopElementFa);
        testJpaControlLoopElementFa.setKey(new PfReferenceKey(PfKey.NULL_KEY_NAME, PfKey.NULL_KEY_VERSION,
                "a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        testJpaControlLoopElementFa.fromAuthorative(cle);
        assertEquals(testJpaControlLoopElement, testJpaControlLoopElementFa);

        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e", testJpaControlLoopElement.getKey().getLocalName());
        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e",
                new JpaControlLoopElement(createControlLoopElementInstance()).getKey().getLocalName());
        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e",
                ((PfReferenceKey) new JpaControlLoopElement(createControlLoopElementInstance()).getKeys().get(0))
                        .getLocalName());

        testJpaControlLoopElement.clean();
        assertEquals("a95757ba-b34a-4049-a2a8-46773abcbe5e", testJpaControlLoopElement.getKey().getLocalName());

        testJpaControlLoopElement.setDescription(" A Message ");
        testJpaControlLoopElement.clean();
        assertEquals("A Message", testJpaControlLoopElement.getDescription());

        JpaControlLoopElement testJpaControlLoopElement2 = new JpaControlLoopElement(testJpaControlLoopElement);
        assertEquals(testJpaControlLoopElement, testJpaControlLoopElement2);
    }

    @Test
    public void testJpaControlLoopElementOrderedState() throws CoderException {
        ControlLoopElement testControlLoopElement = createControlLoopElementInstance();
        JpaControlLoopElement testJpaControlLoopElement = createJpaControlLoopElementInstance();

        testJpaControlLoopElement.setOrderedState(null);
        assertEquals(testControlLoopElement, testJpaControlLoopElement.toAuthorative());
        testJpaControlLoopElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);

        ControlLoopElement noOrderedStateCle = new StandardCoder().decode(
                new File("src/test/resources/json/ControlLoopElementNoOrderedState.json"), ControlLoopElement.class);

        JpaControlLoopElement noOrderedStateJpaCle = new JpaControlLoopElement(noOrderedStateCle);
        assertEquals(testJpaControlLoopElement, noOrderedStateJpaCle);
    }

    @Test
    public void testJpaControlLoopElementValidation() {
        JpaControlLoopElement testJpaControlLoopElement = createJpaControlLoopElementInstance();

        assertThatThrownBy(() -> {
            testJpaControlLoopElement.validate(null);
        }).hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaControlLoopElement.validate("").isValid());
    }

    @Test
    public void testJpaControlLoopElementCompareTo() {
        JpaControlLoopElement testJpaControlLoopElement = createJpaControlLoopElementInstance();

        JpaControlLoopElement otherJpaControlLoopElement = new JpaControlLoopElement(testJpaControlLoopElement);
        assertEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));
        assertEquals(-1, testJpaControlLoopElement.compareTo(null));
        assertEquals(0, testJpaControlLoopElement.compareTo(testJpaControlLoopElement));
        assertNotEquals(0, testJpaControlLoopElement.compareTo(new DummyJpaControlLoopElementChild()));

        testJpaControlLoopElement
                .setKey(new PfReferenceKey("BadValue", "0.0.1", "a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        assertNotEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));
        testJpaControlLoopElement.setKey(new PfReferenceKey(PfKey.NULL_KEY_NAME, PfKey.NULL_KEY_VERSION,
                "a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        assertEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));

        testJpaControlLoopElement.setDefinition(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));
        testJpaControlLoopElement.setDefinition(new PfConceptKey("cleDef", "0.0.1"));
        assertEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));

        testJpaControlLoopElement.setDescription("Description");
        assertNotEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));
        testJpaControlLoopElement.setDescription(null);
        assertEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));

        testJpaControlLoopElement.setOrderedState(ControlLoopOrderedState.PASSIVE);
        assertNotEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));
        testJpaControlLoopElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        assertEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));

        testJpaControlLoopElement.setState(ControlLoopState.PASSIVE);
        assertNotEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));
        testJpaControlLoopElement.setState(ControlLoopState.UNINITIALISED);
        assertEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));

        testJpaControlLoopElement.setParticipantId(new PfConceptKey("dummy", "0.0.1"));
        assertNotEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));
        testJpaControlLoopElement.setParticipantId(new PfConceptKey("participant", "0.0.1"));
        assertEquals(0, testJpaControlLoopElement.compareTo(otherJpaControlLoopElement));

        assertEquals(testJpaControlLoopElement, new JpaControlLoopElement(testJpaControlLoopElement));
    }

    @Test
    public void testJpaControlLoopElementLombok() {
        assertNotNull(new Participant());
        JpaControlLoopElement p0 = new JpaControlLoopElement();

        assertThat(p0.toString()).contains("JpaControlLoopElement(");
        assertEquals(false, p0.hashCode() == 0);
        assertEquals(true, p0.equals(p0));
        assertEquals(false, p0.equals(null));


        JpaControlLoopElement p1 = new JpaControlLoopElement();

        p1.setDefinition(new PfConceptKey("defName", "0.0.1"));
        p1.setDescription("Description");
        p1.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        p1.setState(ControlLoopState.UNINITIALISED);
        p1.setParticipantId(new PfConceptKey("participant", "0.0.1"));

        assertThat(p1.toString()).contains("ControlLoopElement(");
        assertEquals(false, p1.hashCode() == 0);
        assertEquals(false, p1.equals(p0));
        assertEquals(false, p1.equals(null));

        assertNotEquals(p1, p0);

        JpaControlLoopElement p2 = new JpaControlLoopElement();
        assertEquals(p2, p0);
    }

    private JpaControlLoopElement createJpaControlLoopElementInstance() {
        ControlLoopElement testCle = createControlLoopElementInstance();
        JpaControlLoopElement testJpaControlLoopElement = new JpaControlLoopElement();
        testJpaControlLoopElement.setKey(null);
        testJpaControlLoopElement.fromAuthorative(testCle);
        testJpaControlLoopElement.setKey(PfReferenceKey.getNullKey());
        testJpaControlLoopElement.fromAuthorative(testCle);

        return testJpaControlLoopElement;
    }

    private ControlLoopElement createControlLoopElementInstance() {
        ControlLoopElement controlLoopElement = new ControlLoopElement();
        controlLoopElement.setId(UUID.fromString("a95757ba-b34a-4049-a2a8-46773abcbe5e"));
        controlLoopElement.setDefinition(new ToscaConceptIdentifier("cleDef", "0.0.1"));
        controlLoopElement.setParticipantId(new ToscaConceptIdentifier("participant", "0.0.1"));

        return controlLoopElement;
    }
}
