/*--
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.endpoints.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.common.utils.test.log.logback.ExtractAppender;
import org.slf4j.LoggerFactory;

class MessageTypeDispatcherTest {

    /**
     * Used to attach an appender to the class' logger.
     */
    private static final Logger logger = (Logger) LoggerFactory.getLogger(MessageTypeDispatcher.class);
    private static final ExtractAppender appender = new ExtractAppender();

    /**
     * Original logging level for the logger.
     */
    private static Level saveLevel;

    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TYPE_FIELD = "msg-type";
    private static final String TOPIC = "my-topic";
    private static final String TYPE1 = "msg-type-1";
    private static final String TYPE2 = "msg-type-2";

    private MessageTypeDispatcher primary;

    private ScoListener<String> secondary1;
    private ScoListener<String> secondary2;

    /**
     * Initializes statics.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        saveLevel = logger.getLevel();
        logger.setLevel(Level.INFO);

        appender.setContext(logger.getLoggerContext());
        appender.start();
    }

    @AfterAll
    public static void tearDownAfterClass() {
        logger.setLevel(saveLevel);
        appender.stop();
    }

    /**
     * Initializes mocks and a listener.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        appender.clearExtractions();

        secondary1 = mock(ScoListener.class);
        secondary2 = mock(ScoListener.class);

        primary = new MessageTypeDispatcher(TYPE_FIELD);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void testRegister_testUnregister() {
        primary.register(TYPE1, secondary1);
        primary.register(TYPE2, secondary2);

        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE1));
        verify(secondary1).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));
        verify(secondary2, never()).onTopicEvent(any(), any(), any());

        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE1));
        verify(secondary1, times(2)).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));
        verify(secondary2, never()).onTopicEvent(any(), any(), any());

        primary.unregister(TYPE1);
        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE1));
        verify(secondary1, times(2)).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));
        verify(secondary2, never()).onTopicEvent(any(), any(), any());

        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE2));
        verify(secondary1, times(2)).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));
        verify(secondary2).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));

        // unregister again
        primary.unregister(TYPE1);

        // unregister second type
        primary.unregister(TYPE2);
        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE1));
        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE2));
        verify(secondary1, times(2)).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));
        verify(secondary2, times(1)).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));
    }

    @Test
    void testOnTopicEvent() {
        primary.register(TYPE1, secondary1);

        logger.addAppender(appender);

        // success
        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE1));
        verify(secondary1).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));

        // repeat
        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE1));
        verify(secondary1, times(2)).onTopicEvent(eq(INFRA), eq(TOPIC), any(StandardCoderObject.class));

        assertFalse(appender.getExtracted().toString().contains("unable to extract"));
        assertFalse(appender.getExtracted().toString().contains("discarding event of type"));

        // no message type
        appender.clearExtractions();
        primary.onTopicEvent(INFRA, TOPIC, "{}");
        assertTrue(appender.getExtracted().toString().contains("unable to extract"));
        verify(secondary1, times(2)).onTopicEvent(any(), any(), any());

        // unknown type
        appender.clearExtractions();
        primary.onTopicEvent(INFRA, TOPIC, makeMessage(TYPE2));
        assertTrue(appender.getExtracted().toString().contains("discarding event of type"));
        verify(secondary1, times(2)).onTopicEvent(any(), any(), any());
    }

    /**
     * Makes a JSON message of the given type.
     *
     * @param msgType the message type
     * @return a JSON message of the given type
     */
    private String makeMessage(String msgType) {
        String json = "{'" + TYPE_FIELD + "':'" + msgType + "', 'abc':'def'}";
        return json.replace('\'', '"');
    }
}
