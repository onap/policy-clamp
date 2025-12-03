/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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
import static org.mockito.Mockito.spy;
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
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.common.utils.test.log.logback.ExtractAppender;
import org.slf4j.LoggerFactory;

class ScoListenerTest {

    /**
     * Used to attach an appender to the class' logger.
     */
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ScoListener.class);
    private static final ExtractAppender appender = new ExtractAppender();

    /**
     * Original logging level for the logger.
     */
    private static Level saveLevel;

    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    private static final String NAME = "pdp_1";

    private static final Coder coder = new StandardCoder();

    private ScoListener<MyMessage> primary;

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
     * Create various mocks and primary handler.
     */
    @BeforeEach
    public void setUp() {
        appender.clearExtractions();

        primary = new ScoListener<MyMessage>(MyMessage.class) {
            @Override
            public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco,
                            MyMessage message) {
                // do nothing
            }
        };
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void testOnTopicEvent() {
        primary = spy(primary);

        MyMessage status = new MyMessage(NAME);
        StandardCoderObject sco = makeSco(status);
        primary.onTopicEvent(INFRA, TOPIC, sco);
        verify(primary).onTopicEvent(INFRA, TOPIC, sco, status);

        assertFalse(appender.getExtracted().toString().contains("unable to decode"));

        // undecodable message
        logger.addAppender(appender);
        primary.onTopicEvent(INFRA, TOPIC, makeSco());
        verify(primary, times(1)).onTopicEvent(INFRA, TOPIC, sco, status);
        assertTrue(appender.getExtracted().toString().contains("unable to decode"));
    }

    /**
     * Makes a standard object from a JSON string.
     *
     * @return a standard object representing the message
     */
    private StandardCoderObject makeSco() {
        try {
            return coder.decode("[]", StandardCoderObject.class);

        } catch (CoderException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Makes a standard object from a status message.
     *
     * @param source message to be converted
     * @return a standard object representing the message
     */
    private StandardCoderObject makeSco(MyMessage source) {
        try {
            return coder.toStandard(source);

        } catch (CoderException e) {
            throw new RuntimeException(e);
        }
    }

    protected static class MyMessage {
        private String name;

        public MyMessage() {
            super();
        }

        public MyMessage(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MyMessage other = (MyMessage) obj;
            if (name == null) {
                return other.name == null;
            } else {
                return name.equals(other.name);
            }
        }
    }
}
