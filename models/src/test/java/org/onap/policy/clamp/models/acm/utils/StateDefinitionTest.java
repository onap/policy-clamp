/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StateDefinitionTest {

    @Test
    void testNonNull() {
        var stateDefinition = new StateDefinition<String>(2, null);
        assertThatThrownBy(() -> stateDefinition.put(null, null))
            .hasMessageMatching("keys is marked .*ull but is null");
        assertThatThrownBy(() -> stateDefinition.put(new String[] {"", ""}, null))
            .hasMessageMatching("value is marked .*ull but is null");
        assertThatThrownBy(() -> stateDefinition.get(null))
            .hasMessageMatching("keys is marked .*ull but is null");
    }

    @Test
    void testWrongKeys() {
        var stateDefinition = new StateDefinition<String>(2, "NONE", "@");
        assertThatThrownBy(() -> stateDefinition.get(new String[] {"key"}))
            .hasMessageMatching("wrong number of keys");
        assertThatThrownBy(() -> stateDefinition.put(new String[] {"key", "@id"}, "value"))
            .hasMessageMatching("wrong key @id");
        assertThatThrownBy(() -> stateDefinition.put(new String[] {"key", null}, "value"))
            .hasMessageMatching("wrong key null");
    }
}
