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

import org.junit.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class ParticipantTest {
    @Test
    public void testParticipant() {

        Participant p0 = new Participant();
        p0.setDefinition(new ToscaConceptIdentifier("dfName", "1.2.3"));
        assertEquals("dfName", p0.getType());
        assertEquals("1.2.3", p0.getTypeVersion());

        Participant p1 = new Participant(p0);
        assertEquals(p0, p1);

        assertEquals(0, p0.compareTo(p1));
    }

    @Test
    public void testParticipantLombok() {
        assertNotNull(new Participant());
        Participant p0 = new Participant();

        assertThat(p0.toString()).contains("Participant(");
        assertThat(p0.hashCode()).isNotZero();
        assertEquals(true, p0.equals(p0));
        assertEquals(false, p0.equals(null));


        Participant p1 = new Participant();

        p1.setDefinition(new ToscaConceptIdentifier("defName", "0.0.1"));
        p1.setDescription("Description");
        p1.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        p1.setName("Name");
        p1.setParticipantState(ParticipantState.ACTIVE);
        p1.setVersion("0.0.1");

        assertThat(p1.toString()).contains("Participant(");
        assertEquals(false, p1.hashCode() == 0);
        assertEquals(false, p1.equals(p0));
        assertEquals(false, p1.equals(null));

        assertNotEquals(p1, p0);

        Participant p2 = new Participant();

        // @formatter:off
        assertThatThrownBy(() -> p2.setDefinition(null)).      isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> p2.setHealthStatus(null)).    isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> p2.setParticipantState(null)).isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertEquals(p2, p0);
    }
}
