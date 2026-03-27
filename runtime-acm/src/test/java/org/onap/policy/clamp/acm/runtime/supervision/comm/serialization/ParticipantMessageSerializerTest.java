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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;

class ParticipantMessageSerializerTest {

    private final ParticipantMessageSerializer serializer = new ParticipantMessageSerializer();

    @Test
    void testSerializeSuccess() {
        var testObject = new ParticipantRegisterAck();

        byte[] result = serializer.serialize("test-topic", testObject);
        assertNotNull(result);

        String resultString = new String(result);
        assertThat(resultString).startsWith("{").endsWith("}");
    }
}
