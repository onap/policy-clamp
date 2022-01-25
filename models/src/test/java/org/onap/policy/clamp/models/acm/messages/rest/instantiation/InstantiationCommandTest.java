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

package org.onap.policy.clamp.models.acm.messages.rest.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;

class InstantiationCommandTest {
    @Test
    void testInstantiationCommandLombok() {
        assertNotNull(new InstantiationCommand());
        InstantiationCommand ic0 = new InstantiationCommand();

        assertThat(ic0.toString()).contains("InstantiationCommand(");
        assertEquals(false, ic0.hashCode() == 0);
        assertEquals(true, ic0.equals(ic0));
        assertEquals(false, ic0.equals(null));


        InstantiationCommand ic1 = new InstantiationCommand();

        ic1.setAutomationCompositionIdentifierList(new ArrayList<>());
        ic1.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);

        assertThat(ic1.toString()).contains("InstantiationCommand(");
        assertEquals(false, ic1.hashCode() == 0);
        assertEquals(false, ic1.equals(ic0));
        assertEquals(false, ic1.equals(null));

        assertNotEquals(ic1, ic0);

        InstantiationCommand ic2 = new InstantiationCommand();

        assertEquals(ic2, ic0);
    }
}
