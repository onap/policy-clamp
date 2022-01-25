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

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcElementStatisticsTest {
    @Test
    void testAcElementStatisticsLombok() {
        assertNotNull(new AcElementStatistics());
        AcElementStatistics aces0 = new AcElementStatistics();

        assertThat(aces0.toString()).contains("AcElementStatistics(");
        assertThat(aces0.hashCode()).isNotZero();
        assertEquals(true, aces0.equals(aces0));
        assertEquals(false, aces0.equals(null));


        AcElementStatistics aces1 = new AcElementStatistics();
        aces1.setParticipantId(new ToscaConceptIdentifier("defName", "0.0.1"));
        aces1.setTimeStamp(Instant.now());

        assertThat(aces1.toString()).contains("AcElementStatistics(");
        assertEquals(false, aces1.hashCode() == 0);
        assertEquals(false, aces1.equals(aces0));
        assertEquals(false, aces1.equals(null));

        assertNotEquals(aces1, aces0);

        AcElementStatistics aces2 = new AcElementStatistics();
        aces2.setId(UUID.randomUUID());

        // @formatter:off
        assertThatThrownBy(() -> aces2.setParticipantId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> aces2.setTimeStamp(null)).isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertNotEquals(aces2, aces0);
    }
}
