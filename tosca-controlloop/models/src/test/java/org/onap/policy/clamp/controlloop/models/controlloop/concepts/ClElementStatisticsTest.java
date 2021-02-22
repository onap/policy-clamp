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

import java.time.Instant;
import org.junit.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class ClElementStatisticsTest {
    @Test
    public void testClElementStatisticsLombok() {
        assertNotNull(new ClElementStatistics());
        ClElementStatistics cles0 = new ClElementStatistics();

        assertThat(cles0.toString()).contains("ClElementStatistics(");
        assertThat(cles0.hashCode()).isNotZero();
        assertEquals(true, cles0.equals(cles0));
        assertEquals(false, cles0.equals(null));


        ClElementStatistics cles1 = new ClElementStatistics();
        cles1.setControlLoopElementId(new ToscaConceptIdentifier("defName", "0.0.1"));
        cles1.setTimeStamp(Instant.now());

        assertThat(cles1.toString()).contains("ClElementStatistics(");
        assertEquals(false, cles1.hashCode() == 0);
        assertEquals(false, cles1.equals(cles0));
        assertEquals(false, cles1.equals(null));

        assertNotEquals(cles1, cles0);

        ClElementStatistics cles2 = new ClElementStatistics();

        // @formatter:off
        assertThatThrownBy(() -> cles2.setControlLoopElementId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cles2.setTimeStamp(null)).           isInstanceOf(NullPointerException.class);
        // @formatter:on

        assertEquals(cles2, cles0);
    }
}
