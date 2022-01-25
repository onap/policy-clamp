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

package org.onap.policy.clamp.acm.runtime.supervision;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

public class HandleCounter<K> {
    @Getter
    @Setter
    private long maxWaitMs;

    @Getter
    @Setter
    private int maxRetryCount;

    private Map<K, Integer> mapCounter = new HashMap<>();
    private Set<K> mapFault = new HashSet<>();
    private Map<K, Long> mapTimer = new HashMap<>();

    public long getDuration(K id) {
        mapTimer.putIfAbsent(id, getEpochMilli());
        return getEpochMilli() - mapTimer.get(id);
    }

    /**
     * Reset timer and clear counter and fault by id.
     *
     * @param id the id
     */
    public void clear(K id) {
        mapFault.remove(id);
        mapCounter.put(id, 0);
        mapTimer.put(id, getEpochMilli());
    }

    /**
     * Remove counter, timer and fault by id.
     *
     * @param id the id
     */
    public void remove(K id) {
        mapFault.remove(id);
        mapCounter.remove(id);
        mapTimer.remove(id);
    }

    public void setFault(K id) {
        mapCounter.put(id, 0);
        mapFault.add(id);
    }

    /**
     * Increment RetryCount by id e return true if minor or equal of maxRetryCount.
     *
     * @param id the identifier
     * @return false if count is major of maxRetryCount
     */
    public boolean count(K id) {
        int counter = mapCounter.getOrDefault(id, 0) + 1;
        if (counter <= maxRetryCount) {
            mapCounter.put(id, counter);
            return true;
        }
        return false;
    }

    public boolean isFault(K id) {
        return mapFault.contains(id);
    }

    public int getCounter(K id) {
        return mapCounter.getOrDefault(id, 0);
    }

    protected long getEpochMilli() {
        return Instant.now().toEpochMilli();
    }

    public Set<K> keySet() {
        return mapCounter.keySet();
    }
}
