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

import java.sql.Timestamp;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimestampHelper {

    public static String now() {
        return Timestamp.from(Instant.now()).toString();
    }

    public static Timestamp nowTimestamp() {
        return Timestamp.from(Instant.now());
    }

    public static Timestamp toTimestamp(String time) {
        return Timestamp.valueOf(time);
    }

    public static long nowEpochMilli() {
        return Instant.now().toEpochMilli();
    }

    public static long toEpochMilli(String time) {
        return Timestamp.valueOf(time).toInstant().toEpochMilli();
    }
}
