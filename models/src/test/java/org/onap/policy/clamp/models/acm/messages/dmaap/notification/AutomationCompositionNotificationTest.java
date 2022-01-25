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

        assertEquals(true, acn0.isEmpty());

        acn0.setAdded(addedList);
        assertEquals(false, acn0.isEmpty());
        acn0.setAdded(null);
        assertEquals(true, acn0.isEmpty());

        acn0.setDeleted(deletedList);
        assertEquals(false, acn0.isEmpty());
        acn0.setDeleted(null);
        assertEquals(true, acn0.isEmpty());

        acn0.setAdded(addedList);
        acn0.setDeleted(deletedList);
        assertEquals(false, acn0.isEmpty());
        acn0.setAdded(null);
        acn0.setDeleted(null);
        assertEquals(true, acn0.isEmpty());
    }

    @Test
    void testAutomationCompositionNotificationLombok() {
        assertNotNull(new AutomationCompositionNotification());
        assertNotNull(new AutomationCompositionNotification(new ArrayList<>(), new ArrayList<>()));

        AutomationCompositionNotification acn0 = new AutomationCompositionNotification();

        assertThat(acn0.toString()).contains("AutomationCompositionNotification(");
        assertEquals(false, acn0.hashCode() == 0);
        assertEquals(true, acn0.equals(acn0));
        assertEquals(false, acn0.equals(null));


        AutomationCompositionNotification acn1 = new AutomationCompositionNotification();

        assertThat(acn1.toString()).contains("AutomationCompositionNotification(");
        assertEquals(false, acn1.hashCode() == 0);
        assertEquals(true, acn1.equals(acn0));
        assertEquals(false, acn1.equals(null));
    }
}
