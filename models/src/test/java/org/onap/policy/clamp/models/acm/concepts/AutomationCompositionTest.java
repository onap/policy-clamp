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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionTest {
    @Test
    void testAutomationComposition() {
        var ac0 = new AutomationComposition();
        ac0.setDefinition(new ToscaConceptIdentifier("dfName", "1.2.3"));
        assertEquals("dfName", ac0.getType());
        assertEquals("1.2.3", ac0.getTypeVersion());

        var ac1 = new AutomationComposition(ac0);
        assertEquals(ac0, ac1);

        assertEquals(0, ac0.compareTo(ac1));
    }

    @Test
    void testAutomationCompositionLombok() {
        assertNotNull(new AutomationComposition());
        var ac0 = new AutomationComposition();
        ac0.setElements(new LinkedHashMap<>());

        assertThat(ac0.toString()).contains("AutomationComposition(");
        assertThat(ac0.hashCode()).isNotZero();
        assertEquals(ac0, ac0);
        assertNotEquals(null, ac0);

        var ac1 = new AutomationComposition();

        ac1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        ac1.setDescription("Description");
        ac1.setElements(new LinkedHashMap<>());
        ac1.setName("Name");
        ac1.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        ac1.setState(AutomationCompositionState.UNINITIALISED);
        ac1.setVersion("0.0.1");

        assertThat(ac1.toString()).contains("AutomationComposition(");
        assertNotEquals(0, ac1.hashCode());
        assertNotEquals(ac1, ac0);
        assertNotEquals(null, ac1);

        assertNotEquals(ac1, ac0);

        var ac2 = new AutomationComposition();
        ac2.setElements(new LinkedHashMap<>());

        // @formatter:off
        assertThatThrownBy(() -> ac2.setDefinition(null)).  isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ac2.setOrderedState(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ac2.setState(null)).       isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertEquals(ac2, ac0);

        ac1.setCascadedOrderedState(AutomationCompositionOrderedState.PASSIVE);
        assertEquals(AutomationCompositionOrderedState.PASSIVE, ac1.getOrderedState());

        ac1.getElements().put(UUID.randomUUID(), new AutomationCompositionElement());
        ac1.setCascadedOrderedState(AutomationCompositionOrderedState.RUNNING);
        assertEquals(AutomationCompositionOrderedState.RUNNING, ac1.getOrderedState());
        assertEquals(AutomationCompositionOrderedState.RUNNING,
            ac1.getElements().values().iterator().next().getOrderedState());

        assertNull(ac0.getElements().get(UUID.randomUUID()));
        assertNull(ac1.getElements().get(UUID.randomUUID()));

        assertEquals(PfKey.NULL_KEY_NAME, ac0.getDefinition().getName());

    }

    private AutomationCompositionElement getAutomationCompositionElementTest(UUID uuid, ToscaConceptIdentifier id) {
        var acElement = new AutomationCompositionElement();
        acElement.setId(uuid);
        acElement.setParticipantId(id);
        acElement.setDefinition(id);
        acElement.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);

        return acElement;
    }
}
