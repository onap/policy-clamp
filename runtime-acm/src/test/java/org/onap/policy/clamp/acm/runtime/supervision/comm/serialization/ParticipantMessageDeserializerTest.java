/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.SerializationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.onap.policy.common.utils.coder.StandardCoder;

class ParticipantMessageDeserializerTest {

    private final ParticipantMessageDeserializer deserializer = new ParticipantMessageDeserializer();
    private final StandardCoder coder = new StandardCoder();

    @ParameterizedTest
    @MethodSource("provideValidMessages")
    void testDeserializeValidMessages(Supplier<Object> messageSupplier, Class<?> expectedClass) throws Exception {
        Object message = messageSupplier.get();
        byte[] data = coder.encode(message).getBytes();
        Object result = deserializer.deserialize("topic", data);
        assertInstanceOf(expectedClass, result);
    }

    static Stream<Arguments> provideValidMessages() {
        // TODO no need for suppliers if we ignore AUTOMATION_COMPOSITION_STATECHANGE_ACK
        return Stream.of(
            Arguments.of((Supplier<Object>) ParticipantStatus::new, ParticipantStatus.class),
            Arguments.of((Supplier<Object>) AutomationCompositionDeploy::new, AutomationCompositionDeploy.class),
            Arguments.of((Supplier<Object>) AutomationCompositionStateChange::new,
                    AutomationCompositionStateChange.class),
            Arguments.of((Supplier<Object>) ParticipantRegister::new, ParticipantRegister.class),
            Arguments.of((Supplier<Object>) ParticipantRegisterAck::new, ParticipantRegisterAck.class),
            Arguments.of((Supplier<Object>) ParticipantDeregister::new, ParticipantDeregister.class),
            Arguments.of((Supplier<Object>) ParticipantDeregisterAck::new, ParticipantDeregisterAck.class),
            Arguments.of((Supplier<Object>) ParticipantPrime::new, ParticipantPrime.class),
            Arguments.of((Supplier<Object>) ParticipantPrimeAck::new, ParticipantPrimeAck.class),
            Arguments.of((Supplier<Object>) AutomationCompositionDeployAck::new, AutomationCompositionDeployAck.class),
            Arguments.of((Supplier<Object>) () -> {
                var msg = new AutomationCompositionDeployAck();
                msg.setMessageType(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
                return msg;
            }, AutomationCompositionDeployAck.class),
            Arguments.of((Supplier<Object>) ParticipantStatusReq::new, ParticipantStatusReq.class),
            Arguments.of((Supplier<Object>) PropertiesUpdate::new, PropertiesUpdate.class),
            Arguments.of((Supplier<Object>) AutomationCompositionMigration::new, AutomationCompositionMigration.class),
            Arguments.of((Supplier<Object>) ParticipantSync::new, ParticipantSync.class),
            Arguments.of((Supplier<Object>) AutomationCompositionPrepare::new, AutomationCompositionPrepare.class),
            Arguments.of((Supplier<Object>) ParticipantReqSync::new, ParticipantReqSync.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidInputs")
    void testDeserializeInvalidInputs(String json) {
        byte[] data = json.getBytes();
        assertThrows(SerializationException.class, () -> deserializer.deserialize("topic", data));
    }

    static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(
                Arguments.of("invalid json"),
                Arguments.of("{\"messageType\":\"INVALID_TYPE\"}"),
                Arguments.of("{\"someField\":\"value\"}"),
                Arguments.of("{\"messageType\":\"PARTICIPANT_STATE_CHANGE\"}"),
                Arguments.of("{\"messageType\":\"PARTICIPANT_RESTART\"}")
        );
    }
}
