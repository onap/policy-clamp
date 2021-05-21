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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

/**
 * Class to hold the possible values for the type of participant messages.
 */
public enum ParticipantMessageType {

    /**
     * Used by participants to report status to the control loop runtime.
     */
    PARTICIPANT_STATUS,

    /**
     * Used by the control loop runtime to change the state of participants, triggers a PARTICIPANT_STATUS message with
     * the result of the PARTICIPANT_STATE_CHANGE operation.
     */
    PARTICIPANT_STATE_CHANGE,

    /**
     * Used by the control loop runtime to update the control loops running on participants, triggers a
     * PARTICIPANT_STATUS message with the result of the PARTICIPANT_CONTROL_LOOP_UPDATE operation.
     */
    PARTICIPANT_CONTROL_LOOP_UPDATE,

    /**
     * Used by the control loop runtime to change the state of control loops in participants, triggers a
     * PARTICIPANT_STATUS message with the result of the PARTICIPANT_CONTROL_LOOP_STATE_CHANGE operation.
     */
    PARTICIPANT_CONTROL_LOOP_STATE_CHANGE,

    /**
     * Used by the control loop runtime to order a health check on participants, triggers a PARTICIPANT_STATUS message
     * with the result of the PARTICIPANT_HEALTH_CHECK operation.
     */
    PARTICIPANT_HEALTH_CHECK
}
