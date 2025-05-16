/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutomationCompositionRollbackTest {
    @Test
    void testAutomationCompositionRollback() {
        var acr0 = new AutomationCompositionRollback();
        acr0.setInstanceId(UUID.randomUUID());
        acr0.setCompositionId(UUID.randomUUID());
        Map<String, Object> map = new HashMap<>();
        map.put("test", "test");
        acr0.setElements(map);

        var acr1 = new AutomationCompositionRollback(acr0);
        assertEquals(acr0, acr1);
    }
}
