/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.common.message.bus.healthcheck;

import java.util.HashMap;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.message.bus.healthcheck.kafka.KafkaHealthCheck;
import org.onap.policy.common.message.bus.healthcheck.noop.NoopHealthCheck;
import org.onap.policy.common.parameters.topic.TopicParameters;

public class TopicHealthCheckFactory {

    /**
     * Get Topic HealthCheck.
     *
     * @param param TopicParameters
     * @return TopicHealthCheck
     */
    public TopicHealthCheck getTopicHealthCheck(TopicParameters param) {
        return switch (Topic.CommInfrastructure.valueOf(param.getTopicCommInfrastructure().toUpperCase())) {
            case KAFKA -> {
                var map = new HashMap<String, Object>();
                if (param.getAdditionalProps() != null) {
                    map.putAll(param.getAdditionalProps());
                }
                map.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, param.getServers().getFirst());
                yield new KafkaHealthCheck(map);
            }
            case NOOP ->  new NoopHealthCheck();
            default -> null;
        };
    }
}
