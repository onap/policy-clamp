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

import java.util.UUID;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {

    public static final UUID PARTCICIPANT_ID = UUID.randomUUID();

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getParticipantId() {
        return PARTCICIPANT_ID;
    }

    /**
     * Returns participantId for test Jpa cases.
     *
     * @return participant Id
     */
    public static String getJpaParticipantId() {
        return PARTCICIPANT_ID.toString();
    }

    /**
     * Returns random participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getRndParticipantId() {
        return UUID.randomUUID();
    }
}
