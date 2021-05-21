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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;
import org.junit.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class ControlLoopStatusTest {

    @Test
    public void testControlLoopStatusLombok() {
        assertNotNull(new ControlLoopStatus());
        assertNotNull(new ControlLoopStatus(UUID.randomUUID(), new ToscaConceptIdentifier()));

        ControlLoopStatus cln0 = new ControlLoopStatus();

        assertThat(cln0.toString()).contains("ControlLoopStatus(");
        assertEquals(false, cln0.hashCode() == 0);
        assertEquals(true, cln0.equals(cln0));
        assertEquals(false, cln0.equals(null));

        ControlLoopStatus cln1 = new ControlLoopStatus();
        assertEquals(true, cln1.equals(cln0));
    }
}
