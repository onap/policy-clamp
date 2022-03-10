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

package org.onap.policy.clamp.models.acm.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionElementTest {
    @Test
    void testAutomationCompositionElement() {
        assertNotNull(new AutomationCompositionElement());
        var ace0 = new AutomationCompositionElement();
        var ace1 = new AutomationCompositionElement(ace0);
        assertEquals(ace0, ace1);

        ace1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        ace1.setDescription("Description");
        ace1.setId(UUID.randomUUID());
        ace1.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        ace1.setParticipantId(new ToscaConceptIdentifier("id", "1.2.3"));
        ace1.setState(AutomationCompositionState.UNINITIALISED);

        var ace2 = new AutomationCompositionElement(ace1);
        assertEquals(ace1, ace2);
    }

    @Test
    void testAutomationCompositionState() {
        var ace0 = new AutomationCompositionElement();

        assertTrue(
                ace0.getOrderedState()
                .equalsAutomationCompositionState(AutomationCompositionState.UNINITIALISED));

        assertTrue(
                ace0.getOrderedState().asState()
                .equalsAutomationCompositionOrderedState(AutomationCompositionOrderedState.UNINITIALISED));
    }

    @Test
    void testAutomationCompositionElementLombok() {
        var ace0 = new AutomationCompositionElement();

        assertThat(ace0.toString()).contains("AutomationCompositionElement(");
        assertThat(ace0.hashCode()).isNotZero();
        assertEquals(ace0, ace0);
        assertNotEquals(null, ace0);

        var ace1 = new AutomationCompositionElement();

        ace1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        ace1.setDescription("Description");
        ace1.setId(UUID.randomUUID());
        ace1.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        ace1.setParticipantId(new ToscaConceptIdentifier("id", "1.2.3"));
        ace1.setState(AutomationCompositionState.UNINITIALISED);

        assertThat(ace1.toString()).contains("AutomationCompositionElement(");
        assertNotEquals(0, ace1.hashCode());
        assertNotEquals(ace1, ace0);
        assertNotEquals(null, ace1);

        assertNotEquals(ace1, ace0);

        var ace2 = new AutomationCompositionElement();

        // @formatter:off
        assertThatThrownBy(() -> ace2.setDefinition(null)).   isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ace2.setId(null)).           isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ace2.setOrderedState(null)). isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ace2.setParticipantId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ace2.setState(null)).        isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertNotEquals(ace2, ace0);
    }
}
