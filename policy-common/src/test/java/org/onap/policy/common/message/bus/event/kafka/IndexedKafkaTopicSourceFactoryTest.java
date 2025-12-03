/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.topic.BusTopicParams;

class IndexedKafkaTopicSourceFactoryTest {

    private IndexedKafkaTopicSourceFactory factory;

    @Test
    void testBuild() {
        factory = new IndexedKafkaTopicSourceFactory();
        BusTopicParams params = new BusTopicParams();

        // set servers to null
        params.setServers(null);
        assertThatThrownBy(() -> factory.build(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("KAFKA Server(s) must be provided");

        // set servers to empty
        params.setServers(List.of());
        assertThatThrownBy(() -> factory.build(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("KAFKA Server(s) must be provided");

        List<String> servers = List.of("kafka:9092", "kafka:29092");
        params.setServers(servers);

        // set topic to null
        params.setTopic(null);
        assertThatThrownBy(() -> factory.build(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("A topic must be provided");

        // set topic to empty
        params.setTopic("");
        assertThatThrownBy(() -> factory.build(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("A topic must be provided");
    }
}
