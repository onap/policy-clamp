/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HandleCounterTest {

    private static final int ID = 1;

    @Test
    void testCount() {
        var handleCounter = new HandleCounter<Integer>();
        handleCounter.setMaxRetryCount(2);
        assertThat(handleCounter.count(ID)).isTrue();
        assertThat(handleCounter.getCounter(ID)).isEqualTo(1);
        assertThat(handleCounter.count(ID)).isTrue();
        assertThat(handleCounter.getCounter(ID)).isEqualTo(2);
        assertThat(handleCounter.count(ID)).isFalse();
        assertThat(handleCounter.getCounter(ID)).isEqualTo(2);

        handleCounter.clear(ID);
        assertThat(handleCounter.count(ID)).isTrue();
        assertThat(handleCounter.getCounter(ID)).isEqualTo(1);
    }

    @Test
    void testFault() {
        var handleCounter = new HandleCounter<Integer>();
        handleCounter.setFault(ID);
        assertThat(handleCounter.isFault(ID)).isTrue();
        handleCounter.clear(ID);
        assertThat(handleCounter.isFault(ID)).isFalse();
    }

    @Test
    void testDuration() {
        var handleCounter = new HandleCounter<Integer>() {
            long epochMilli = 0;

            @Override
            protected long getEpochMilli() {
                return epochMilli;
            }
        };
        handleCounter.epochMilli = 100;
        var result = handleCounter.getDuration(ID);
        assertThat(result).isZero();

        handleCounter.epochMilli += 100;
        result = handleCounter.getDuration(ID);
        assertThat(result).isEqualTo(100);

        handleCounter.epochMilli += 100;
        result = handleCounter.getDuration(ID);
        assertThat(result).isEqualTo(200);

        handleCounter.epochMilli += 100;
        handleCounter.clear(ID);
        result = handleCounter.getDuration(ID);
        assertThat(result).isZero();
    }
}
