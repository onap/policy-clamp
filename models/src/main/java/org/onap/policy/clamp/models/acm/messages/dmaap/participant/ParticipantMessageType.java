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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

/**
 * Class to hold the possible values for the type of participant messages.
 */
public enum ParticipantMessageType {

    /**
     * Used by participants to report status to the automation composition runtime.
     */
    PARTICIPANT_STATUS,

    /**
     * Used by the acm runtime to change the state of participants, triggers a
     * PARTICIPANT_STATUS message with the result of the PARTICIPANT_STATE_CHANGE operation.
     */
    PARTICIPANT_STATE_CHANGE,

    /**
     * Used by acm runtime to update the automation compositions running on participants, triggers a
     * PARTICIPANT_STATUS message with the result of the AUTOMATION_COMPOSITION_UPDATE operation.
     */
    AUTOMATION_COMPOSITION_UPDATE,

    /**
     * Used by acm runtime to change the state of automation compositions in participants, triggers a
     * PARTICIPANT_STATUS message with result of AUTOMATION_COMPOSITION_STATE_CHANGE operation.
     */
    AUTOMATION_COMPOSITION_STATE_CHANGE,

    /**
     * Used by the automation composition runtime to order a health check on participants, triggers a
     * PARTICIPANT_STATUS message with the result of the PARTICIPANT_HEALTH_CHECK operation.
     */
    PARTICIPANT_HEALTH_CHECK,

    /**
     * Used by participant to register itself with automation composition runtime.
     */
    PARTICIPANT_REGISTER,

    /**
     * Used by automation composition runtime to respond to participant registration.
     */
    PARTICIPANT_REGISTER_ACK,

    /**
     * Used by participant to deregister itself with automation composition runtime.
     */
    PARTICIPANT_DEREGISTER,

    /**
     * Used by automation composition runtime to respond to participant deregistration.
     */
    PARTICIPANT_DEREGISTER_ACK,

    /**
     * Used by automation composition runtime to send ToscaServiceTemplate to participant.
     */
    PARTICIPANT_UPDATE,

    /**
     * Used by participant to acknowledge the receipt of PARTICIPANT_UPDATE message
     * from automation composition runtime.
     */
    PARTICIPANT_UPDATE_ACK,

    /**
     * Used by participant to acknowledge the receipt of AUTOMATION_COMPOSITION_UPDATE message
     * from automation composition runtime.
     */
    AUTOMATION_COMPOSITION_UPDATE_ACK,

    /**
     * Used by participant to acknowledge the receipt of AUTOMATION_COMPOSITION_STATE_CHANGE message
     * from automation composition runtime.
     */
    AUTOMATION_COMPOSITION_STATECHANGE_ACK,

    /**
     * Used by automation composition runtime to request for PARTICIPANT_STATUS message immediately.
     */
    PARTICIPANT_STATUS_REQ
}
