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

package org.onap.policy.common.message.bus.event.base;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.onap.policy.common.utils.gson.GsonTestUtils;

class BusTopicBaseTest extends TopicTestBase {

    private BusTopicBaseImpl base;

    /**
     * Initializes the object to be tested.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        base = new BusTopicBaseImpl(builder.build());
    }

    @Test
    void testToString() {
        assertNotNull(base.toString());
    }

    @Test
    void testSerialize() {
        assertThatCode(() -> new GsonTestUtils().compareGson(base, BusTopicBaseTest.class)).doesNotThrowAnyException();
    }

    @Test
    void testGetApiKey() {
        assertEquals(MY_API_KEY, base.getApiKey());
    }

    @Test
    void testGetApiSecret() {
        assertEquals(MY_API_SECRET, base.getApiSecret());
    }

    @Test
    void testIsUseHttps() {
        assertTrue(base.isUseHttps());
        assertFalse(new BusTopicBaseImpl(builder.useHttps(false).build()).isUseHttps());
    }

    @Test
    void testIsAllowSelfSignedCerts() {
        assertTrue(base.isAllowSelfSignedCerts());
        assertFalse(new BusTopicBaseImpl(builder.allowSelfSignedCerts(false).build()).isAllowSelfSignedCerts());
    }

    @Test
    void testTopic() {
        assertEquals(MY_TOPIC, base.getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, base.getEffectiveTopic());
        assertNotEquals(base.getTopic(), base.getEffectiveTopic());
    }

    @Test
    void testAnyNullOrEmpty() {
        assertFalse(base.anyNullOrEmpty());
        assertFalse(base.anyNullOrEmpty("any-none-null", "any-none-null-B"));

        assertTrue(base.anyNullOrEmpty(null, "any-first-null"));
        assertTrue(base.anyNullOrEmpty("any-middle-null", null, "any-middle-null-B"));
        assertTrue(base.anyNullOrEmpty("any-last-null", null));
        assertTrue(base.anyNullOrEmpty("any-empty", ""));
    }

    @Test
    void testAllNullOrEmpty() {
        assertTrue(base.allNullOrEmpty());
        assertTrue(base.allNullOrEmpty(""));
        assertTrue(base.allNullOrEmpty(null, ""));

        assertFalse(base.allNullOrEmpty("all-ok-only-one"));
        assertFalse(base.allNullOrEmpty("all-ok-one", "all-ok-two"));
        assertFalse(base.allNullOrEmpty("all-ok-null", null));
        assertFalse(base.allNullOrEmpty("", "all-ok-empty"));
        assertFalse(base.allNullOrEmpty("", "all-one-ok", null));
    }

    private static class BusTopicBaseImpl extends BusTopicBase {

        public BusTopicBaseImpl(BusTopicParams busTopicParams) {
            super(busTopicParams);
        }

        @Override
        public CommInfrastructure getTopicCommInfrastructure() {
            return CommInfrastructure.NOOP;
        }

        @Override
        public boolean start() {
            return true;
        }

        @Override
        public boolean stop() {
            return true;
        }

        @Override
        public void shutdown() {
            // do nothing
        }

    }
}
