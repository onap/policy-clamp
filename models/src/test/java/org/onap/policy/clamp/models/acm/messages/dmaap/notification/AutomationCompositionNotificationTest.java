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

package org.onap.policy.clamp.models.acm.messages.dmaap.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutomationCompositionNotificationTest {

    @Test
    void testAutomationCompositionNotification() {
        AutomationCompositionNotification acn0 = new AutomationCompositionNotification();

        List<AutomationCompositionStatus> addedList = new ArrayList<>();
        addedList.add(new AutomationCompositionStatus());

        List<AutomationCompositionStatus> deletedList = new ArrayList<>();
        deletedList.add(new AutomationCompositionStatus());

        assertTrue(acn0.isEmpty());

        acn0.setAdded(addedList);
        assertFalse(acn0.isEmpty());
        acn0.setAdded(null);
        assertTrue(acn0.isEmpty());

        acn0.setDeleted(deletedList);
        assertFalse(acn0.isEmpty());
        acn0.setDeleted(null);
        assertTrue(acn0.isEmpty());

        acn0.setAdded(addedList);
        acn0.setDeleted(deletedList);
        assertFalse(acn0.isEmpty());
        acn0.setAdded(null);
        acn0.setDeleted(null);
        assertTrue(acn0.isEmpty());
    }

    @Test
    void testAutomationCompositionNotificationLombok() {
        assertNotNull(new AutomationCompositionNotification());
        assertNotNull(new AutomationCompositionNotification(new ArrayList<>(), new ArrayList<>()));

        AutomationCompositionNotification acn0 = new AutomationCompositionNotification();

        assertThat(acn0.toString()).contains("AutomationCompositionNotification(");
        assertNotEquals(0, acn0.hashCode());
        assertEquals(acn0, acn0);
        assertNotEquals(null, acn0);


        AutomationCompositionNotification acn1 = new AutomationCompositionNotification();

        assertThat(acn1.toString()).contains("AutomationCompositionNotification(");
        assertNotEquals(0, acn1.hashCode());
        assertEquals(acn1, acn0);
        assertNotEquals(null, acn1);
    }
}
