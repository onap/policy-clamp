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
 * Class to hold the possible values for mode of participant.
 */
public enum ParticipantState {
    /**
     * Participant state is unknown.
     */
    UNKNOWN,

    /**
     * Participant is passive, automation composition element execution is always rejected.
     */
    PASSIVE,

    /**
     * Automation composition element execution execution proceeds, but changes to domain state or context are not
     * carried out. The participant returns an indication that it is running in SAFE mode together with the action it
     * would have performed if it was operating in ACTIVE mode.
     */
    SAFE,

    /**
     * Automation composition element execution execution proceeds and changes to domain and state are carried out in a
     * test environment. The participant returns an indication that it is running in TEST mode together with the action
     * it has performed on the test environment.
     */
    TEST,

    /**
     * Automation composition element execution is executed in the live environment by the participant.
     */
    ACTIVE,

    /**
     * Automation composition element execution has been terminated.
     */
    TERMINATED
}
