/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.base.validation.BeanValidator;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

/**
 * Test the{@link JpaAutomationCompositionTest} class.
 */
class JpaAutomationCompositionTest {
    private static final String NULL_ERROR = " is marked non-null but is null";
    private static final String INSTANCE_ID = "709c62b3-8918-41b9-a747-d21eb79c6c20";
    private static final String COMPOSITION_ID = "709c62b3-8918-41b9-a747-e21eb79c6c41";

    @Test
    void testJpaAutomationCompositionConstructor() {
        assertThatThrownBy(() -> {
            new JpaAutomationComposition((JpaAutomationComposition) null);
        }).hasMessageMatching("copyConcept" + NULL_ERROR);

        assertThatThrownBy(() -> {
            new JpaAutomationComposition((AutomationComposition) null);
        }).hasMessageMatching("authorativeConcept" + NULL_ERROR);
    }

    @Test
    void testJpaAutomationComposition() {
        var automationComposition = createAutomationCompositionInstance();
        var jpaAutomationComposition = new JpaAutomationComposition(automationComposition);

        assertEquals(automationComposition, jpaAutomationComposition.toAuthorative());

        var target = UUID.randomUUID();
        jpaAutomationComposition.setCompositionTargetId(target.toString());
        automationComposition.setCompositionTargetId(target);
        assertEquals(automationComposition, jpaAutomationComposition.toAuthorative());

        assertThatThrownBy(() -> {
            jpaAutomationComposition.fromAuthorative(null);
        }).hasMessageMatching("automationComposition" + NULL_ERROR);

        assertThatThrownBy(() -> new JpaAutomationComposition((JpaAutomationComposition) null))
                .isInstanceOf(NullPointerException.class);

        var jpaAutomationCompositionFa = new JpaAutomationComposition();
        jpaAutomationCompositionFa.setInstanceId(null);
        jpaAutomationCompositionFa.fromAuthorative(automationComposition);
        assertEquals(jpaAutomationComposition, jpaAutomationCompositionFa);

        assertEquals("automation-composition", jpaAutomationComposition.getName());
        assertEquals("automation-composition",
                new JpaAutomationComposition(createAutomationCompositionInstance()).getName());

        var jpaAutomationComposition2 = new JpaAutomationComposition(jpaAutomationComposition);
        assertEquals(jpaAutomationComposition, jpaAutomationComposition2);
    }

    @Test
    void testJpaAutomationCompositionValidation() {
        var testJpaAutomationComposition = new JpaAutomationComposition(createAutomationCompositionInstance());
        assertTrue(BeanValidator.isValid(testJpaAutomationComposition));
    }

    @Test
    void testJpaAutomationCompositionLombok() {
        var ac0 = new JpaAutomationComposition();
        ac0.setCompositionId(COMPOSITION_ID);

        assertThat(ac0.hashCode()).isNotZero();
        assertNotEquals(null, ac0);

        var ac1 = new JpaAutomationComposition();

        ac1.setCompositionId(UUID.randomUUID().toString());
        ac1.setDescription("Description");
        ac1.setElements(new ArrayList<>());
        ac1.setInstanceId(INSTANCE_ID);

        assertNotEquals(0, ac1.hashCode());
        assertNotEquals(ac1, ac0);
        assertNotEquals(null, ac1);

        assertNotEquals(ac1, ac0);

        var ac2 = new JpaAutomationComposition();
        ac2.setCompositionId(COMPOSITION_ID);
        ac2.setInstanceId(ac0.getInstanceId());
        assertEquals(ac2, ac0);
    }

    private AutomationComposition createAutomationCompositionInstance() {
        var testAutomationComposition = new AutomationComposition();
        testAutomationComposition.setName("automation-composition");
        testAutomationComposition.setInstanceId(UUID.fromString(INSTANCE_ID));
        testAutomationComposition.setVersion("0.0.1");
        testAutomationComposition.setLastMsg(TimestampHelper.now());
        testAutomationComposition.setCompositionId(UUID.fromString(COMPOSITION_ID));
        testAutomationComposition.setElements(new LinkedHashMap<>());

        return testAutomationComposition;
    }
}
