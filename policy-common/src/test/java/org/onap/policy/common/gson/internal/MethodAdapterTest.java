/*--
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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

package org.onap.policy.common.gson.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

class MethodAdapterTest {
    private static final Gson gson = new Gson();

    private String saved;

    @Test
    void testMethodAdapter_testInvoke() throws Exception {
        MethodAdapter adapter =
                new MethodAdapter(gson, MethodAdapterTest.class.getDeclaredMethod("getValue"), String.class);
        assertEquals("hello", adapter.invoke(this));

        MethodAdapter adapter2 = new MethodAdapter(gson,
                MethodAdapterTest.class.getDeclaredMethod("setValue", String.class), String.class);
        adapter2.invoke(this, "world");
        assertEquals("world", saved);

        assertThatThrownBy(() -> adapter2.invoke(this, 100)).isInstanceOf(JsonParseException.class)
                .hasMessage(MethodAdapter.INVOKE_ERR + MethodAdapterTest.class.getName() + ".setValue");
    }

    public String getValue() {
        return "hello";
    }

    void setValue(String val) {
        saved = val;
    }
}
