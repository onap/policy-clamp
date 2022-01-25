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
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ParticipantStatisticsTest {
    @Test
    void testParticipantStatisticsLombok() {
        assertNotNull(new ParticipantStatistics());
        ParticipantStatistics ps0 = new ParticipantStatistics();

        assertThat(ps0.toString()).contains("ParticipantStatistics(");
        assertThat(ps0.hashCode()).isNotZero();
        assertEquals(true, ps0.equals(ps0));
        assertEquals(false, ps0.equals(null));


        ParticipantStatistics ps1 = new ParticipantStatistics();
        ps1.setParticipantId(new ToscaConceptIdentifier("defName", "0.0.1"));
        ps1.setTimeStamp(Instant.now());

        assertThat(ps1.toString()).contains("ParticipantStatistics(");
        assertEquals(false, ps1.hashCode() == 0);
        assertEquals(false, ps1.equals(ps0));
        assertEquals(false, ps1.equals(null));

        assertNotEquals(ps1, ps0);

        ParticipantStatistics ps2 = new ParticipantStatistics();

        // @formatter:off
        assertThatThrownBy(() -> ps2.setParticipantId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ps2.setTimeStamp(null)).    isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertEquals(ps2, ps0);
    }
}
