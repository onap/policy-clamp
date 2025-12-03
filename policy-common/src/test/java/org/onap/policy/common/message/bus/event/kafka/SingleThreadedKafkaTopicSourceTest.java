/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.base.TopicTestBase;

class SingleThreadedKafkaTopicSourceTest extends TopicTestBase {
    private SingleThreadedKafkaTopicSource source;

    /**
     * Creates the object to be tested.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        source = new SingleThreadedKafkaTopicSource(makeKafkaBuilder().build());
    }

    @AfterEach
    public void tearDown() {
        source.shutdown();
    }

    @Test
    void testToString() {
        assertTrue(source.toString().startsWith("SingleThreadedKafkaTopicSource ["));
        source.shutdown();
    }

    @Test
    void testGetTopicCommInfrastructure() {
        assertEquals(CommInfrastructure.KAFKA, source.getTopicCommInfrastructure());
    }

}
