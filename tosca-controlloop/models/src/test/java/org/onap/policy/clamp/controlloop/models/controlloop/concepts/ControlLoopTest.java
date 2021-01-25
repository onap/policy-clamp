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

import java.util.ArrayList;
import java.util.UUID;
import org.junit.Test;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class ControlLoopTest {
    @Test
    public void testControlLoop() {
        ControlLoop cl0 = new ControlLoop();
        cl0.setDefinition(new ToscaConceptIdentifier("dfName", "1.2.3"));
        assertEquals("dfName", cl0.getType());
        assertEquals("1.2.3", cl0.getTypeVersion());

        ControlLoop cl1 = new ControlLoop(cl0);
        assertEquals(cl0, cl1);

        assertEquals(0, cl0.compareTo(cl1));
    }

    @Test
    public void testControlLoopLombok() {
        assertNotNull(new ControlLoop());
        ControlLoop cl0 = new ControlLoop();

        assertThat(cl0.toString()).contains("ControlLoop(");
        assertEquals(false, cl0.hashCode() == 0);
        assertEquals(true, cl0.equals(cl0));
        assertEquals(false, cl0.equals(null));

        ControlLoop cl1 = new ControlLoop();

        cl1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        cl1.setDescription("Description");
        cl1.setElements(new ArrayList<>());
        cl1.setName("Name");
        cl1.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        cl1.setState(ControlLoopState.UNINITIALISED);
        cl1.setVersion("0.0.1");

        cl1.setElements(new ArrayList<>());

        assertThat(cl1.toString()).contains("ControlLoop(");
        assertEquals(false, cl1.hashCode() == 0);
        assertEquals(false, cl1.equals(cl0));
        assertEquals(false, cl1.equals(null));

        assertNotEquals(cl1, cl0);

        ControlLoop cl2 = new ControlLoop();

        // @formatter:off
        assertThatThrownBy(() -> cl2.setDefinition(null)).  isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cl2.setOrderedState(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cl2.setState(null)).       isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertEquals(cl2, cl0);

        cl1.setCascadedOrderedState(ControlLoopOrderedState.PASSIVE);
        assertEquals(ControlLoopOrderedState.PASSIVE, cl1.getOrderedState());

        cl1.getElements().add(new ControlLoopElement());
        cl1.setCascadedOrderedState(ControlLoopOrderedState.RUNNING);
        assertEquals(ControlLoopOrderedState.RUNNING, cl1.getOrderedState());
        assertEquals(ControlLoopOrderedState.RUNNING, cl1.getElements().get(0).getOrderedState());

        assertNull(cl0.getElement(UUID.randomUUID()));
        assertNull(cl1.getElement(UUID.randomUUID()));
        assertEquals(cl1.getElements().get(0), cl1.getElement(cl1.getElements().get(0).getId()));

        assertEquals(PfKey.NULL_KEY_NAME, cl0.getDefinition().getName());
    }
}
