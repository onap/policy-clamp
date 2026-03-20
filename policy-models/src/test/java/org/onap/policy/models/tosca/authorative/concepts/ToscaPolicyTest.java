/*
 * ============LICENSE_START=======================================================
 * ONAP Policy Models
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2024,2026 OpenInfra Foundation Europe. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.models.tosca.authorative.concepts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfUtils;

/**
 * Tests methods not tested by {@link PojosTest}.
 */
class ToscaPolicyTest {

    @Test
    void testGetIdentifier_testGetTypeIdentifier() {
        assertThatThrownBy(() -> {
            new ToscaPolicy(null);
        }).hasMessageMatching("copyObject is marked non-null but is null");

        ToscaPolicy policy = new ToscaPolicy();

        policy.setName("my_name");
        policy.setVersion("1.2.3");
        policy.setType("my_type");
        policy.setTypeVersion("3.2.1");

        var key = PfUtils.getKey(policy);
        assertEquals("ToscaEntityKey(name=my_name, version=1.2.3)", key.toString());
        assertEquals(new ToscaConceptIdentifier("my_name", "1.2.3"), key.asIdentifier());

        ToscaPolicy clonedPolicy0 = new ToscaPolicy(policy);
        assertEquals(0, new ToscaEntityComparator<ToscaPolicy>().compare(policy, clonedPolicy0));

        policy.setProperties(new LinkedHashMap<String, Object>());
        policy.getProperties().put("PropertyKey", "PropertyValue");
        ToscaPolicy clonedPolicy1 = new ToscaPolicy(policy);
        assertEquals(0, new ToscaEntityComparator<ToscaPolicy>().compare(policy, clonedPolicy1));
    }
}
