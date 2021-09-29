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
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ControlLoopElementTest {
    @Test
    void testControlLoopElement() {
        ControlLoopElement cle0 = new ControlLoopElement();
        ControlLoopElement cle1 = new ControlLoopElement(cle0);

        assertEquals(cle0, cle1);
    }

    @Test
    void testControlLoopState() {
        ControlLoopElement cle0 = new ControlLoopElement();

        assertTrue(
                cle0.getOrderedState()
                .equalsControlLoopState(ControlLoopState.UNINITIALISED));

        assertTrue(
                cle0.getOrderedState().asState()
                .equalsControlLoopOrderedState(ControlLoopOrderedState.UNINITIALISED));
    }

    @Test
    void testControlLoopElementLombok() {
        assertNotNull(new ControlLoopElement());
        ControlLoopElement cle0 = new ControlLoopElement();

        assertThat(cle0.toString()).contains("ControlLoopElement(");
        assertThat(cle0.hashCode()).isNotZero();
        assertEquals(true, cle0.equals(cle0));
        assertEquals(false, cle0.equals(null));

        ControlLoopElement cle1 = new ControlLoopElement();

        cle1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        cle1.setDescription("Description");
        cle1.setId(UUID.randomUUID());
        cle1.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        cle1.setParticipantId(new ToscaConceptIdentifier("id", "1.2.3"));
        cle1.setState(ControlLoopState.UNINITIALISED);

        assertThat(cle1.toString()).contains("ControlLoopElement(");
        assertEquals(false, cle1.hashCode() == 0);
        assertEquals(false, cle1.equals(cle0));
        assertEquals(false, cle1.equals(null));

        assertNotEquals(cle1, cle0);

        ControlLoopElement cle2 = new ControlLoopElement();

        // @formatter:off
        assertThatThrownBy(() -> cle2.setDefinition(null)).   isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cle2.setId(null)).           isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cle2.setOrderedState(null)). isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cle2.setParticipantId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cle2.setState(null)).        isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertNotEquals(cle2, cle0);
    }
}
