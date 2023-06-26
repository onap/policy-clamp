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

package org.onap.policy.clamp.acm.runtime.supervision;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

public class TimeoutHandler<K> {
    @Getter
    @Setter
    private long maxWaitMs;

    private Set<K> mapTimeout = new HashSet<>();
    private Map<K, Long> mapTimer = new HashMap<>();

    public long getDuration(K id) {
        mapTimer.putIfAbsent(id, getEpochMilli());
        return getEpochMilli() - mapTimer.get(id);
    }

    /**
     * Reset timer and timeout by id.
     *
     * @param id the id
     */
    public void clear(K id) {
        mapTimeout.remove(id);
        mapTimer.put(id, getEpochMilli());
    }

    /**
     * Remove timer and timeout by id.
     *
     * @param id the id
     */
    public void remove(K id) {
        mapTimeout.remove(id);
        mapTimer.remove(id);
    }

    public void setTimeout(K id) {
        mapTimeout.add(id);
    }

    public boolean isTimeout(K id) {
        return mapTimeout.contains(id);
    }

    protected long getEpochMilli() {
        return Instant.now().toEpochMilli();
    }
}
