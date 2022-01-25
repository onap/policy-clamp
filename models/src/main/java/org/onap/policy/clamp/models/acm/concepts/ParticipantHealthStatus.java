/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.concepts;

/**
 * Class to hold the possible values for health status of Participant.
 */
public enum ParticipantHealthStatus {

    /**
     * Participant is healthy and working fine.
     */
    HEALTHY,

    /**
     * Participant is not healthy.
     */
    NOT_HEALTHY,

    /**
     * Participant is currently under test state and performing tests.
     */
    TEST_IN_PROGRESS,

    /**
     * The health status of the Participant is unknown.
     */
    UNKNOWN,

    /**
     * The health status of the Participant is off line.
     */
    OFF_LINE
}
