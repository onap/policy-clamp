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
     * Used by the controlloop runtime to change the state of participants, triggers a
     * PARTICIPANT_STATUS message with the result of the PARTICIPANT_STATE_CHANGE operation.
     */
    PARTICIPANT_STATE_CHANGE,

    /**
     * Used by controlloop runtime to update the controlloops running on participants, triggers a
     * PARTICIPANT_STATUS message with the result of the CONTROL_LOOP_UPDATE operation.
     */
    CONTROL_LOOP_UPDATE,

    /**
     * Used by controlloop runtime to change the state of controlloops in participants, triggers a
     * PARTICIPANT_STATUS message with result of CONTROL_LOOP_STATE_CHANGE operation.
     */
    CONTROL_LOOP_STATE_CHANGE,

    /**
     * Used by the control loop runtime to order a health check on participants, triggers a
     * PARTICIPANT_STATUS message with the result of the PARTICIPANT_HEALTH_CHECK operation.
     */
    PARTICIPANT_HEALTH_CHECK,

    /**
     * Used by participant to register itself with control loop runtime.
     */
    PARTICIPANT_REGISTER,

    /**
     * Used by control loop runtime to respond to participant registration.
     */
    PARTICIPANT_REGISTER_ACK,

    /**
     * Used by participant to deregister itself with control loop runtime.
     */
    PARTICIPANT_DEREGISTER,

    /**
     * Used by control loop runtime to respond to participant deregistration.
     */
    PARTICIPANT_DEREGISTER_ACK,

    /**
     * Used by control loop runtime to send ToscaServiceTemplate to participant.
     */
    PARTICIPANT_UPDATE,

    /**
     * Used by participant to acknowledge the receipt of Participant_Update message
     * from control loop runtime.
     */
    PARTICIPANT_UPDATE_ACK
}
