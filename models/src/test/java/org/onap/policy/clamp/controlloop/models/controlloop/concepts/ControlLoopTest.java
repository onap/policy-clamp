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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ControlLoopTest {
    @Test
    void testControlLoop() {
        var cl0 = new ControlLoop();
        cl0.setDefinition(new ToscaConceptIdentifier("dfName", "1.2.3"));
        assertEquals("dfName", cl0.getType());
        assertEquals("1.2.3", cl0.getTypeVersion());

        var cl1 = new ControlLoop(cl0);
        assertEquals(cl0, cl1);

        assertEquals(0, cl0.compareTo(cl1));
    }

    @Test
    void testControlLoopLombok() {
        assertNotNull(new ControlLoop());
        var cl0 = new ControlLoop();
        cl0.setElements(new LinkedHashMap<>());

        assertThat(cl0.toString()).contains("ControlLoop(");
        assertThat(cl0.hashCode()).isNotZero();
        assertEquals(true, cl0.equals(cl0));
        assertEquals(false, cl0.equals(null));

        var cl1 = new ControlLoop();

        cl1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        cl1.setDescription("Description");
        cl1.setElements(new LinkedHashMap<>());
        cl1.setName("Name");
        cl1.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        cl1.setState(ControlLoopState.UNINITIALISED);
        cl1.setVersion("0.0.1");

        assertThat(cl1.toString()).contains("ControlLoop(");
        assertEquals(false, cl1.hashCode() == 0);
        assertEquals(false, cl1.equals(cl0));
        assertEquals(false, cl1.equals(null));

        assertNotEquals(cl1, cl0);

        var cl2 = new ControlLoop();
        cl2.setElements(new LinkedHashMap<>());

        // @formatter:off
        assertThatThrownBy(() -> cl2.setDefinition(null)).  isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cl2.setOrderedState(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cl2.setState(null)).       isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertEquals(cl2, cl0);

        cl1.setCascadedOrderedState(ControlLoopOrderedState.PASSIVE);
        assertEquals(ControlLoopOrderedState.PASSIVE, cl1.getOrderedState());

        cl1.getElements().put(UUID.randomUUID(), new ControlLoopElement());
        cl1.setCascadedOrderedState(ControlLoopOrderedState.RUNNING);
        assertEquals(ControlLoopOrderedState.RUNNING, cl1.getOrderedState());
        assertEquals(ControlLoopOrderedState.RUNNING, cl1.getElements().values().iterator().next().getOrderedState());

        assertNull(cl0.getElements().get(UUID.randomUUID()));
        assertNull(cl1.getElements().get(UUID.randomUUID()));

        assertEquals(PfKey.NULL_KEY_NAME, cl0.getDefinition().getName());

    }

    @Test
    void testControlLoopElementStatisticsList() {
        var cl = new ControlLoop();
        List<ClElementStatistics> emptylist = cl.getControlLoopElementStatisticsList(cl);
        assertNull(emptylist);

        var cl1 = getControlLoopTest();
        List<ClElementStatistics> list = cl1.getControlLoopElementStatisticsList(cl1);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(ControlLoopState.UNINITIALISED, list.get(0).getControlLoopState());
    }

    private ControlLoop getControlLoopTest() {
        var cl = new ControlLoop();
        cl.setDefinition(new ToscaConceptIdentifier("defName", "1.2.3"));
        cl.setDescription("Description");
        cl.setElements(new LinkedHashMap<>());
        cl.setName("Name");
        cl.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        cl.setState(ControlLoopState.UNINITIALISED);
        cl.setVersion("0.0.1");

        var uuid = UUID.randomUUID();
        var id = new ToscaConceptIdentifier(
                "org.onap.policy.controlloop.PolicyControlLoopParticipant", "1.0.1");
        var clElement = getControlLoopElementTest(uuid, id);

        var uuid2 = UUID.randomUUID();
        var id2 = new ToscaConceptIdentifier(
                "org.onap.policy.controlloop.PolicyControlLoopParticipantIntermediary", "0.0.1");
        var clElement2 = getControlLoopElementTest(uuid2, id2);

        cl.getElements().put(uuid, clElement);
        cl.getElements().put(uuid2, clElement2);
        return cl;
    }

    private ControlLoopElement getControlLoopElementTest(UUID uuid, ToscaConceptIdentifier id) {
        var clElement = new ControlLoopElement();
        clElement.setId(uuid);
        clElement.setParticipantId(id);
        clElement.setDefinition(id);
        clElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);

        var clElementStatistics = new ClElementStatistics();
        clElementStatistics.setParticipantId(id);
        clElementStatistics.setControlLoopState(ControlLoopState.UNINITIALISED);
        clElementStatistics.setTimeStamp(Instant.now());

        clElement.setClElementStatistics(clElementStatistics);

        return clElement;
    }
}
