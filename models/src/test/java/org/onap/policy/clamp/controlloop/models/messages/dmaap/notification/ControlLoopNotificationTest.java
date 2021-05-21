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

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ControlLoopNotificationTest {

    @Test
    public void testControlLoopNotification() {
        ControlLoopNotification cln0 = new ControlLoopNotification();

        List<ControlLoopStatus> addedList = new ArrayList<>();
        addedList.add(new ControlLoopStatus());

        List<ControlLoopStatus> deletedList = new ArrayList<>();
        deletedList.add(new ControlLoopStatus());

        assertEquals(true, cln0.isEmpty());

        cln0.setAdded(addedList);
        assertEquals(false, cln0.isEmpty());
        cln0.setAdded(null);
        assertEquals(true, cln0.isEmpty());

        cln0.setDeleted(deletedList);
        assertEquals(false, cln0.isEmpty());
        cln0.setDeleted(null);
        assertEquals(true, cln0.isEmpty());

        cln0.setAdded(addedList);
        cln0.setDeleted(deletedList);
        assertEquals(false, cln0.isEmpty());
        cln0.setAdded(null);
        cln0.setDeleted(null);
        assertEquals(true, cln0.isEmpty());
    }

    @Test
    public void testControlLoopNotificationLombok() {
        assertNotNull(new ControlLoopNotification());
        assertNotNull(new ControlLoopNotification(new ArrayList<>(), new ArrayList<>()));

        ControlLoopNotification cln0 = new ControlLoopNotification();

        assertThat(cln0.toString()).contains("ControlLoopNotification(");
        assertEquals(false, cln0.hashCode() == 0);
        assertEquals(true, cln0.equals(cln0));
        assertEquals(false, cln0.equals(null));


        ControlLoopNotification cln1 = new ControlLoopNotification();

        assertThat(cln1.toString()).contains("ControlLoopNotification(");
        assertEquals(false, cln1.hashCode() == 0);
        assertEquals(true, cln1.equals(cln0));
        assertEquals(false, cln1.equals(null));
    }
}
