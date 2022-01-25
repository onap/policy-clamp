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

package org.onap.policy.clamp.models.acm.concepts;

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
        assertEquals(true, ac0.equals(ac0));
        assertEquals(false, ac0.equals(null));

        var ac1 = new AutomationComposition();

        ac1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        ac1.setDescription("Description");
        ac1.setElements(new LinkedHashMap<>());
        ac1.setName("Name");
        ac1.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        ac1.setState(AutomationCompositionState.UNINITIALISED);
        ac1.setVersion("0.0.1");

        assertThat(ac1.toString()).contains("AutomationComposition(");
        assertEquals(false, ac1.hashCode() == 0);
        assertEquals(false, ac1.equals(ac0));
        assertEquals(false, ac1.equals(null));

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

    @Test
    void testAutomationCompositionElementStatisticsList() {
        var ac = new AutomationComposition();
        List<AcElementStatistics> emptylist = ac.getAutomationCompositionElementStatisticsList(ac);
        assertEquals(List.of(), emptylist);

        var ac1 = getAutomationCompositionTest();
        List<AcElementStatistics> list = ac1.getAutomationCompositionElementStatisticsList(ac1);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(AutomationCompositionState.UNINITIALISED, list.get(0).getState());
    }

    private AutomationComposition getAutomationCompositionTest() {
        var ac = new AutomationComposition();
        ac.setDefinition(new ToscaConceptIdentifier("defName", "1.2.3"));
        ac.setDescription("Description");
        ac.setElements(new LinkedHashMap<>());
        ac.setName("Name");
        ac.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        ac.setState(AutomationCompositionState.UNINITIALISED);
        ac.setVersion("0.0.1");

        var uuid = UUID.randomUUID();
        var id = new ToscaConceptIdentifier("org.onap.policy.acm.PolicyAutomationCompositionParticipant", "1.0.1");
        var acElement = getAutomationCompositionElementTest(uuid, id);

        var uuid2 = UUID.randomUUID();
        var id2 = new ToscaConceptIdentifier("org.onap.policy.acm.PolicyAutomationCompositionParticipantIntermediary",
            "0.0.1");
        var acElement2 = getAutomationCompositionElementTest(uuid2, id2);

        ac.getElements().put(uuid, acElement);
        ac.getElements().put(uuid2, acElement2);
        return ac;
    }

    private AutomationCompositionElement getAutomationCompositionElementTest(UUID uuid, ToscaConceptIdentifier id) {
        var acElement = new AutomationCompositionElement();
        acElement.setId(uuid);
        acElement.setParticipantId(id);
        acElement.setDefinition(id);
        acElement.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);

        var acElementStatistics = new AcElementStatistics();
        acElementStatistics.setParticipantId(id);
        acElementStatistics.setState(AutomationCompositionState.UNINITIALISED);
        acElementStatistics.setTimeStamp(Instant.now());

        acElement.setAcElementStatistics(acElementStatistics);

        return acElement;
    }
}
