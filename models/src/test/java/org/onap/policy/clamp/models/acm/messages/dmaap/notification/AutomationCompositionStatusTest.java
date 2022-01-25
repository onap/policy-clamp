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

package org.onap.policy.clamp.models.acm.messages.dmaap.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionStatusTest {

    @Test
    void testAutomationCompositionStatusLombok() {
        assertNotNull(new AutomationCompositionStatus());
        assertNotNull(new AutomationCompositionStatus(UUID.randomUUID(), new ToscaConceptIdentifier()));

        AutomationCompositionStatus acn0 = new AutomationCompositionStatus();

        assertThat(acn0.toString()).contains("AutomationCompositionStatus(");
        assertEquals(false, acn0.hashCode() == 0);
        assertEquals(true, acn0.equals(acn0));
        assertEquals(false, acn0.equals(null));

        AutomationCompositionStatus acn1 = new AutomationCompositionStatus();
        assertEquals(true, acn1.equals(acn0));
    }
}
