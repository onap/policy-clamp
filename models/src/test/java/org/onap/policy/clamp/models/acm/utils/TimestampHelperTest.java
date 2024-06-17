/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TimestampHelperTest {

    @Test
    void testNow() {
        assertThat(TimestampHelper.nowTimestamp()).isNotNull();
        assertThat(TimestampHelper.now()).isNotNull();
        assertThat(TimestampHelper.nowEpochMilli()).isNotZero();
    }

    @Test
    void testToEpochMilli() {
        var timeStr = TimestampHelper.now();
        var milli = TimestampHelper.toTimestamp(timeStr).toInstant().toEpochMilli();
        var result = TimestampHelper.toEpochMilli(timeStr);
        assertThat(milli).isEqualTo(result);
    }
}
