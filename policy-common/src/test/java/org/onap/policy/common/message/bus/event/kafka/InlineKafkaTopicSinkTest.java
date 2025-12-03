/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.message.bus.event.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.base.TopicTestBase;

class InlineKafkaTopicSinkTest extends TopicTestBase {
    private InlineKafkaTopicSink sink;

    /**
     * Creates the object to be tested.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        sink = new InlineKafkaTopicSink(makeKafkaBuilder().build());
    }

    @AfterEach
    public void tearDown() {
        sink.shutdown();
    }

    @Test
    void testToString() {
        assertTrue(sink.toString().startsWith("InlineKafkaTopicSink ["));
    }

    @Test
    void testInit() {
        // nothing null
        sink = new InlineKafkaTopicSink(makeKafkaBuilder().build());
        sink.init();
        assertThatCode(() -> sink.shutdown()).doesNotThrowAnyException();
    }

    @Test
    void testGetTopicCommInfrastructure() {
        assertEquals(CommInfrastructure.KAFKA, sink.getTopicCommInfrastructure());
    }

}
