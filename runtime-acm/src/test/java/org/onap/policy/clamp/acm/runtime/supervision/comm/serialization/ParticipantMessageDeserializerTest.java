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
    void testDeserializeValidMessages(Object message, Class<?> expectedClass) throws Exception {
        byte[] data = coder.encode(message).getBytes();
        Object result = deserializer.deserialize("topic", data);
        assertInstanceOf(expectedClass, result);
    }

    static Stream<Arguments> provideValidMessages() {
        return Stream.of(
                Arguments.of(new ParticipantStatus(), ParticipantStatus.class),
                Arguments.of(new AutomationCompositionDeploy(), AutomationCompositionDeploy.class),
                Arguments.of(new AutomationCompositionDeployAck(), AutomationCompositionDeployAck.class),
                Arguments.of(new AutomationCompositionStateChange(), AutomationCompositionStateChange.class),
                Arguments.of(new ParticipantRegister(), ParticipantRegister.class),
                Arguments.of(new ParticipantRegisterAck(), ParticipantRegisterAck.class),
                Arguments.of(new ParticipantDeregister(), ParticipantDeregister.class),
                Arguments.of(new ParticipantDeregisterAck(), ParticipantDeregisterAck.class),
                Arguments.of(new ParticipantPrime(), ParticipantPrime.class),
                Arguments.of(new ParticipantPrimeAck(), ParticipantPrimeAck.class),
                Arguments.of(new ParticipantStatusReq(), ParticipantStatusReq.class),
                Arguments.of(new PropertiesUpdate(), PropertiesUpdate.class),
                Arguments.of(new AutomationCompositionMigration(), AutomationCompositionMigration.class),
                Arguments.of(new ParticipantSync(), ParticipantSync.class),
                Arguments.of(new AutomationCompositionPrepare(), AutomationCompositionPrepare.class),
                Arguments.of(new ParticipantReqSync(), ParticipantReqSync.class)
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
