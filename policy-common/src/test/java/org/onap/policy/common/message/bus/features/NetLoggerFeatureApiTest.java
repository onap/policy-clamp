/*-
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

package org.onap.policy.common.message.bus.features;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.utils.NetLoggerUtil.EventType;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class NetLoggerFeatureApiTest {

    @Mock
    private Logger mockLogger;

    @Mock
    private EventType mockEventType;

    @Mock
    private CommInfrastructure mockCommInfrastructure;

    private NetLoggerFeatureApi featureApi;

    @BeforeEach
    public void setUp() {
        featureApi = new NetLoggerFeatureApi() {
            @Override
            public boolean beforeLog(Logger eventLogger, EventType type, CommInfrastructure protocol, String topic,
                                     String message) {
                return NetLoggerFeatureApi.super.beforeLog(eventLogger, type, protocol, topic, message);
            }

            @Override
            public boolean afterLog(Logger eventLogger, EventType type, CommInfrastructure protocol, String topic,
                                    String message) {
                return NetLoggerFeatureApi.super.afterLog(eventLogger, type, protocol, topic, message);
            }

            @Override
            public int getSequenceNumber() {
                return 0;
            }

            @Override
            public String getName() {
                return NetLoggerFeatureApi.super.getName();
            }
        };
    }

    @Test
    void testBeforeLogDefaultBehavior() {
        boolean result = featureApi.beforeLog(mockLogger, mockEventType, mockCommInfrastructure,
            "testTopic", "testMessage");
        assertFalse(result, "Expected beforeLog to return false by default");
    }

    @Test
    void testAfterLogDefaultBehavior() {
        boolean result = featureApi.afterLog(mockLogger, mockEventType, mockCommInfrastructure,
            "testTopic", "testMessage");
        assertFalse(result, "Expected afterLog to return false by default");
    }
}
