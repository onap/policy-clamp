/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm.serialization;

import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderObject;

public class ParticipantMessageDeserializer implements Deserializer<Object> {

    private static final Coder coder = new StandardCoder();

    @Override
    public Object deserialize(final String topic, final byte[] data) {
        try {
            // decode from JSON into a standard object
            final String jsonString = new String(data);
            final StandardCoderObject sco = coder.decode(jsonString, StandardCoderObject.class);

            // extract the message type
            final String messageType = sco.getString("messageType");
            final ParticipantMessageType participantMessageType = ParticipantMessageType.valueOf(messageType);
            final Class<?> targetClass = resolveClass(participantMessageType);

            // deserialize into target class
            return coder.fromStandard(sco, targetClass);

        } catch (final Exception e) {
            throw new SerializationException("Failed to deserialize JSON", e);
        }
    }

    private static Class<?> resolveClass(final ParticipantMessageType participantMessageType) {
        return switch (participantMessageType) {
            case PARTICIPANT_STATUS -> ParticipantStatus.class;
            case PARTICIPANT_STATE_CHANGE ->
                    throw new UnsupportedOperationException("PARTICIPANT_STATE_CHANGE not supported");
            case AUTOMATION_COMPOSITION_DEPLOY -> AutomationCompositionDeploy.class;
            case AUTOMATION_COMPOSITION_STATE_CHANGE -> AutomationCompositionStateChange.class;
            case PARTICIPANT_REGISTER -> ParticipantRegister.class;
            case PARTICIPANT_REGISTER_ACK -> ParticipantRegisterAck.class;
            case PARTICIPANT_DEREGISTER -> ParticipantDeregister.class;
            case PARTICIPANT_DEREGISTER_ACK -> ParticipantDeregisterAck.class;
            case PARTICIPANT_PRIME -> ParticipantPrime.class;
            case PARTICIPANT_PRIME_ACK -> ParticipantPrimeAck.class;
            case AUTOMATION_COMPOSITION_DEPLOY_ACK -> AutomationCompositionDeployAck.class;
            case AUTOMATION_COMPOSITION_STATECHANGE_ACK -> AutomationCompositionDeployAck.class;
            case PARTICIPANT_STATUS_REQ -> ParticipantStatusReq.class;
            case PROPERTIES_UPDATE -> PropertiesUpdate.class;
            case PARTICIPANT_RESTART -> throw new UnsupportedOperationException("PARTICIPANT_RESTART not supported");
            case AUTOMATION_COMPOSITION_MIGRATION -> AutomationCompositionMigration.class;
            case PARTICIPANT_SYNC_MSG -> ParticipantSync.class;
            case AUTOMATION_COMPOSITION_PREPARE -> AutomationCompositionPrepare.class;
            case PARTICIPANT_REQ_SYNC_MSG -> ParticipantReqSync.class;
        };
    }

}
