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

package org.onap.policy.clamp.acm.element.handler.serialization;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.apache.commons.lang3.SerializationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessage;
import org.onap.policy.common.utils.coder.MapperFactory;

class ParticipantMessageDeserializerTest {

    private final ParticipantMessageDeserializer deserializer = new ParticipantMessageDeserializer();
    private static final ObjectMapper MAPPER = MapperFactory.createJsonMapper();

    @ParameterizedTest
    @MethodSource("provideValidMessages")
    void testDeserializeValidMessages(Object message, Class<?> expectedClass) throws Exception {
        var data = MAPPER.writeValueAsString(message).getBytes();
        var result = deserializer.deserialize("topic", data);
        assertInstanceOf(expectedClass, result);
    }

    static Stream<Arguments> provideValidMessages() {
        return Stream.of(
                Arguments.of(new ElementMessage(), ElementMessage.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidInputs")
    void testDeserializeInvalidInputs(String json) {
        var data = json.getBytes();
        assertThrows(SerializationException.class, () -> deserializer.deserialize("topic", data));
    }

    static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(
                Arguments.of("invalid json"),
                Arguments.of("{\"messageType\":\"INVALID_TYPE\"}"),
                Arguments.of("{\"messageType\":\"PARTICIPANT_STATE_CHANGE\"}"),
                Arguments.of("{\"messageType\":\"PARTICIPANT_RESTART\"}")
        );
    }
}
