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

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response.Status;
import lombok.NonNull;
import org.onap.policy.models.base.PfModelRuntimeException;

public class StateDefinition<V> {

    private final Map<String, V> map = new HashMap<>();

    private final int size;
    private final V defaultValue;
    private final String separator;

    /**
     * Constructor.
     *
     * @param size the number of keys
     * @param defaultValue the default Value
     * @param separator the separator used to concatenate keys
     */
    public StateDefinition(int size, V defaultValue, String separator) {
        this.size = size;
        this.defaultValue = defaultValue;
        this.separator = separator;
    }

    /**
     * Constructor.
     *
     * @param size the number of keys
     * @param defaultValue the default Value
     */
    public StateDefinition(int size, V defaultValue) {
        this(size, defaultValue, "@");
    }

    public void put(@NonNull String[] keys, @NonNull V value) {
        map.put(createKey(keys), value);
    }

    public V get(@NonNull String[] keys) {
        return map.getOrDefault(createKey(keys), defaultValue);
    }

    private String createKey(String[] keys) {
        if (keys.length != size) {
            throw new PfModelRuntimeException(Status.INTERNAL_SERVER_ERROR, "wrong number of keys");
        }
        var sb = new StringBuilder();
        for (var key : keys) {
            if (key == null || key.contains(separator)) {
                throw new PfModelRuntimeException(Status.INTERNAL_SERVER_ERROR, "wrong key " + key);
            }
            sb.append(key + separator);
        }
        return sb.toString();
    }
}
