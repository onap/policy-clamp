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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TimeoutHandlerTest {

    private static final int ID = 1;

    @Test
    void testFault() {
        var timeoutHandler = new TimeoutHandler<Integer>();
        timeoutHandler.setTimeout(ID);
        assertThat(timeoutHandler.isTimeout(ID)).isTrue();
        timeoutHandler.clear(ID);
        assertThat(timeoutHandler.isTimeout(ID)).isFalse();
    }

    @Test
    void testDuration() {
        var timeoutHandler = new TimeoutHandler<Integer>() {
            long epochMilli = 0;

            @Override
            protected long getEpochMilli() {
                return epochMilli;
            }
        };
        timeoutHandler.epochMilli = 100;
        var result = timeoutHandler.getDuration(ID);
        assertThat(result).isZero();

        timeoutHandler.epochMilli += 100;
        result = timeoutHandler.getDuration(ID);
        assertThat(result).isEqualTo(100);

        timeoutHandler.epochMilli += 100;
        result = timeoutHandler.getDuration(ID);
        assertThat(result).isEqualTo(200);

        timeoutHandler.epochMilli += 100;
        timeoutHandler.clear(ID);
        result = timeoutHandler.getDuration(ID);
        assertThat(result).isZero();
    }
}
