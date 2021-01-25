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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the {@link JpaControlLoopTest} class.
 */
public class JpaControlLoopTest {

    private static final String NULL_KEY_ERROR = "key is marked .*ull but is null";

    @Test
    public void testJpaControlLoopConstructor() {
        assertThatThrownBy(() -> {
            new JpaControlLoop((JpaControlLoop) null);
        }).hasMessageMatching("copyConcept is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoop((PfConceptKey) null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, null, null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, null, null, new ArrayList<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, null, ControlLoopState.UNINITIALISED, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, null, ControlLoopState.UNINITIALISED, new ArrayList<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, new PfConceptKey(), null, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, new PfConceptKey(), null, new ArrayList<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, new PfConceptKey(), ControlLoopState.UNINITIALISED, null);
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(null, new PfConceptKey(), ControlLoopState.UNINITIALISED, new ArrayList<>());
        }).hasMessageMatching(NULL_KEY_ERROR);

        assertThatThrownBy(() -> {
            new JpaControlLoop(new PfConceptKey(), null, null, null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoop(new PfConceptKey(), null, null, new ArrayList<>());
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoop(new PfConceptKey(), null, ControlLoopState.UNINITIALISED, null);
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoop(new PfConceptKey(), null, ControlLoopState.UNINITIALISED, new ArrayList<>());
        }).hasMessageMatching("definition is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoop(new PfConceptKey(), new PfConceptKey(), null, null);
        }).hasMessageMatching("state is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoop(new PfConceptKey(), new PfConceptKey(), null, new ArrayList<>());
        }).hasMessageMatching("state is marked .*ull but is null");

        assertThatThrownBy(() -> {
            new JpaControlLoop(new PfConceptKey(), new PfConceptKey(), ControlLoopState.UNINITIALISED, null);
        }).hasMessageMatching("elements is marked .*ull but is null");

        assertNotNull(new JpaControlLoop());
        assertNotNull(new JpaControlLoop((new PfConceptKey())));
        assertNotNull(new JpaControlLoop(new PfConceptKey(), new PfConceptKey(), ControlLoopState.UNINITIALISED,
                new ArrayList<>()));
    }

    @Test
    public void testJpaControlLoop() {
        JpaControlLoop testJpaControlLoop = createJpaControlLoopInstance();

        ControlLoop participant = createControlLoopInstance();
        assertEquals(participant, testJpaControlLoop.toAuthorative());

        assertThatThrownBy(() -> {
            testJpaControlLoop.fromAuthorative(null);
        }).hasMessageMatching("controlLoop is marked .*ull but is null");

        assertThatThrownBy(() -> new JpaControlLoop((JpaControlLoop) null)).isInstanceOf(NullPointerException.class);

        JpaControlLoop testJpaControlLoopFa = new JpaControlLoop();
        testJpaControlLoopFa.setKey(null);
        testJpaControlLoopFa.fromAuthorative(participant);
        assertEquals(testJpaControlLoop, testJpaControlLoopFa);
        testJpaControlLoopFa.setKey(PfConceptKey.getNullKey());
        testJpaControlLoopFa.fromAuthorative(participant);
        assertEquals(testJpaControlLoop, testJpaControlLoopFa);
        testJpaControlLoopFa.setKey(new PfConceptKey("control-loop", "0.0.1"));
        testJpaControlLoopFa.fromAuthorative(participant);
        assertEquals(testJpaControlLoop, testJpaControlLoopFa);

        assertEquals("control-loop", testJpaControlLoop.getKey().getName());
        assertEquals("control-loop", new JpaControlLoop(createControlLoopInstance()).getKey().getName());
        assertEquals("control-loop",
                ((PfConceptKey) new JpaControlLoop(createControlLoopInstance()).getKeys().get(0)).getName());

        testJpaControlLoop.clean();
        assertEquals("control-loop", testJpaControlLoop.getKey().getName());

        testJpaControlLoop.setDescription("   A Message   ");
        testJpaControlLoop.clean();
        assertEquals("A Message", testJpaControlLoop.getDescription());

        JpaControlLoop testJpaControlLoop2 = new JpaControlLoop(testJpaControlLoop);
        assertEquals(testJpaControlLoop, testJpaControlLoop2);
    }

    @Test
    public void testJpaControlLoopElementOrderedState() throws CoderException {
        ControlLoop testControlLoop = createControlLoopInstance();
        JpaControlLoop testJpaControlLoop = createJpaControlLoopInstance();

        testJpaControlLoop.setOrderedState(null);
        assertEquals(testControlLoop, testJpaControlLoop.toAuthorative());
        testJpaControlLoop.setOrderedState(ControlLoopOrderedState.UNINITIALISED);

        ControlLoop noOrderedStateCl = new StandardCoder()
                .decode(new File("src/test/resources/json/ControlLoopNoOrderedState.json"), ControlLoop.class);

        JpaControlLoop noOrderedStateJpaCl = new JpaControlLoop(noOrderedStateCl);
        assertEquals(testJpaControlLoop, noOrderedStateJpaCl);

        ControlLoops controlLoopsWithElements = new StandardCoder()
                .decode(new File("src/test/resources/providers/TestControlLoops.json"), ControlLoops.class);

        JpaControlLoop jpaControlLoopWithElements =
                new JpaControlLoop(controlLoopsWithElements.getControlLoops().get(0));
        assertEquals(4, jpaControlLoopWithElements.getElements().size());
        assertEquals(14, jpaControlLoopWithElements.getKeys().size());
        assertThatCode(() -> jpaControlLoopWithElements.clean()).doesNotThrowAnyException();

        assertEquals(controlLoopsWithElements.getControlLoops().get(0), jpaControlLoopWithElements.toAuthorative());
    }

    @Test
    public void testJpaControlLoopValidation() {
        JpaControlLoop testJpaControlLoop = createJpaControlLoopInstance();

        assertThatThrownBy(() -> {
            testJpaControlLoop.validate(null);
        }).hasMessageMatching("fieldName is marked .*ull but is null");

        assertTrue(testJpaControlLoop.validate("").isValid());
    }

    @Test
    public void testJpaControlLoopCompareTo() {
        JpaControlLoop testJpaControlLoop = createJpaControlLoopInstance();

        JpaControlLoop otherJpaControlLoop = new JpaControlLoop(testJpaControlLoop);
        assertEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));
        assertEquals(-1, testJpaControlLoop.compareTo(null));
        assertEquals(0, testJpaControlLoop.compareTo(testJpaControlLoop));
        assertNotEquals(0, testJpaControlLoop.compareTo(new DummyJpaControlLoopChild()));

        testJpaControlLoop.setKey(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));
        testJpaControlLoop.setKey(new PfConceptKey("control-loop", "0.0.1"));
        assertEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));

        testJpaControlLoop.setDefinition(new PfConceptKey("BadValue", "0.0.1"));
        assertNotEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));
        testJpaControlLoop.setDefinition(new PfConceptKey("controlLoopDefinitionName", "0.0.1"));
        assertEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));

        testJpaControlLoop.setState(ControlLoopState.PASSIVE);
        assertNotEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));
        testJpaControlLoop.setState(ControlLoopState.UNINITIALISED);
        assertEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));

        testJpaControlLoop.setOrderedState(ControlLoopOrderedState.PASSIVE);
        assertNotEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));
        testJpaControlLoop.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        assertEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));

        testJpaControlLoop.setDescription("A description");
        assertNotEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));
        testJpaControlLoop.setDescription(null);
        assertEquals(0, testJpaControlLoop.compareTo(otherJpaControlLoop));

        assertEquals(testJpaControlLoop, new JpaControlLoop(testJpaControlLoop));
    }

    @Test
    public void testJpaControlLoopLombok() {
        assertNotNull(new ControlLoop());
        JpaControlLoop p0 = new JpaControlLoop();

        assertThat(p0.toString()).contains("JpaControlLoop(");
        assertEquals(false, p0.hashCode() == 0);
        assertEquals(true, p0.equals(p0));
        assertEquals(false, p0.equals(null));


        JpaControlLoop p1 = new JpaControlLoop();

        p1.setDefinition(new PfConceptKey("defName", "0.0.1"));
        p1.setDescription("Description");
        p1.setElements(new ArrayList<>());
        p1.setKey(new PfConceptKey("participant", "0.0.1"));
        p1.setState(ControlLoopState.UNINITIALISED);

        assertThat(p1.toString()).contains("ControlLoop(");
        assertEquals(false, p1.hashCode() == 0);
        assertEquals(false, p1.equals(p0));
        assertEquals(false, p1.equals(null));

        assertNotEquals(p1, p0);

        JpaControlLoop p2 = new JpaControlLoop();
        assertEquals(p2, p0);
    }

    private JpaControlLoop createJpaControlLoopInstance() {
        ControlLoop testControlLoop = createControlLoopInstance();
        JpaControlLoop testJpaControlLoop = new JpaControlLoop();
        testJpaControlLoop.setKey(null);
        testJpaControlLoop.fromAuthorative(testControlLoop);
        testJpaControlLoop.setKey(PfConceptKey.getNullKey());
        testJpaControlLoop.fromAuthorative(testControlLoop);

        return testJpaControlLoop;
    }

    private ControlLoop createControlLoopInstance() {
        ControlLoop testControlLoop = new ControlLoop();
        testControlLoop.setName("control-loop");
        testControlLoop.setVersion("0.0.1");
        testControlLoop.setDefinition(new ToscaConceptIdentifier("controlLoopDefinitionName", "0.0.1"));
        testControlLoop.setElements(new ArrayList<>());

        return testControlLoop;
    }
}
