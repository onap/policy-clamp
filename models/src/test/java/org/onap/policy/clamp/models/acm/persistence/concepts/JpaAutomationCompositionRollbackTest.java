/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;

class JpaAutomationCompositionRollbackTest {

    private static final String NULL_INSTANCE_ID_ERROR = "instanceId is marked .*ull but is null";
    private static final String NULL_ERROR = " is marked .*ull but is null";
    private static final String INSTANCE_ID = "709c62b3-8918-41b9-a747-d21eb79c6c20";
    private static final String COMPOSITION_ID = "709c62b3-8918-41b9-a747-e21eb79c6c41";

    @Test
    void testJpaAutomationCompositionRollbackConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationCompositionRollback((JpaAutomationCompositionRollback) null);
        }).hasMessageMatching("copyConcept" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionRollback((AutomationCompositionRollback) null);
        }).hasMessageMatching("authorativeConcept" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionRollback(null, null, null);
        }).hasMessageMatching(NULL_INSTANCE_ID_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionRollback(INSTANCE_ID, null, null);
        }).hasMessageMatching("compositionId" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationCompositionRollback(INSTANCE_ID, COMPOSITION_ID, null);
        }).hasMessageMatching("elements" + NULL_ERROR);

        Map<String, Object> map = Map.of("test", "test");
        assertDoesNotThrow(() -> new JpaAutomationCompositionRollback(INSTANCE_ID, COMPOSITION_ID, map));
    }

    @Test
    void testJpaAutomationCompositionRollback() {
        var automationCompositionRollback = createAutomationCompositionRollbackInstance();
        var jpaAutomationCompositionRollback = new JpaAutomationCompositionRollback(automationCompositionRollback);


        assertEquals(automationCompositionRollback, jpaAutomationCompositionRollback.toAuthorative());
        assertDoesNotThrow(() -> new JpaAutomationCompositionRollback(jpaAutomationCompositionRollback));
    }

    @Test
    void testJpaCompositionRollbackCompareTo() {
        var jpaAutomationCompositionRollback =
                new JpaAutomationCompositionRollback(createAutomationCompositionRollbackInstance());

        var otherJpaAutomationComposition =
                new JpaAutomationCompositionRollback(jpaAutomationCompositionRollback);

        assertEquals(0, jpaAutomationCompositionRollback.compareTo(otherJpaAutomationComposition));
        assertEquals(-1, jpaAutomationCompositionRollback.compareTo(null));
        assertEquals(0, jpaAutomationCompositionRollback.compareTo(jpaAutomationCompositionRollback));
    }

    private AutomationCompositionRollback createAutomationCompositionRollbackInstance() {
        var testAcmRollback = new AutomationCompositionRollback();
        testAcmRollback.setInstanceId(UUID.fromString(INSTANCE_ID));
        testAcmRollback.setCompositionId(UUID.fromString(COMPOSITION_ID));
        var acElement = new AutomationCompositionElement();
        testAcmRollback.setElements(Map.of(acElement.getId(), acElement));

        return testAcmRollback;
    }
}
