/*
 * ============LICENSE_START=======================================================
 * Common Utils-Test
 * ================================================================================
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

package org.onap.policy.common.utils.test.log.logback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ExtractAppenderTest {
    private static final String ABC_DIGIT = "abc[0-9]";
    private static final String ABC_DIGIT1 = "abc[1-9]";
    private static final String DEF_DIGIT = "def[0-9]";
    private static final String HELLO = "hello";
    private static final String HELLO_ABC = "hello abc";
    private static final String HELLO_ABC1_WORLD = "hello abc1 world";
    private static final String HELLO_ABC3 = "hello abc3";
    private static final String WORLD = "world";
    private static final String WORLD_ABC = "world abc";
    private static final String WORLD_GHI2_WORLD = "world ghi2 world";

    /**
     * Milliseconds to wait for a thread to terminate.
     */
    private static final long THREAD_WAIT_MS = 5000L;

    private static Logger logger;

    private List<Thread> threads;

    @BeforeAll
    public static void setUpBeforeClass() {
        logger = (Logger) LoggerFactory.getLogger(ExtractAppenderTest.class);
        logger.setLevel(Level.INFO);
    }

    @BeforeEach
    public void setUp() {
        threads = new LinkedList<>();
    }

    /**
     * Tear down all appenders and threads.
     */
    @AfterEach
    public void tearDown() throws Exception {
        logger.detachAndStopAllAppenders();

        for (Thread p : threads) {
            p.interrupt();
            p.join(THREAD_WAIT_MS);
        }
    }

    @Test
    void testExtractAppender() {
        AtomicInteger count = new AtomicInteger(0);

        ExtractAppender appender = new ExtractAppender() {
            @Override
            protected void append(ILoggingEvent event) {
                count.incrementAndGet();
                super.append(event);
            }
        };

        addAppender(appender);

        logger.info(HELLO);
        logger.info(WORLD);

        // "append" should always be called
        assertEquals(2, count.get());

        // appender with no patterns - everything should match
        assertEquals(strList(HELLO, WORLD), appender.getExtracted());

        // add a pattern and verify match
        appender.setPattern(ABC_DIGIT);
        logger.info("hello abc1");

        // this should not match
        logger.info("hello def2");

        assertEquals(4, count.get());
        assertEquals(strList(HELLO, WORLD, "abc1"), appender.getExtracted());
    }

    @Test
    void testExtractAppenderStringArray() {
        AtomicInteger count = new AtomicInteger(0);

        ExtractAppender appender = new ExtractAppender(ABC_DIGIT, DEF_DIGIT) {
            @Override
            protected void append(ILoggingEvent event) {
                count.incrementAndGet();
                super.append(event);
            }
        };

        addAppender(appender);

        logger.info(HELLO_ABC1_WORLD);
        logger.info(WORLD_GHI2_WORLD); // no match
        logger.info("world def3 world");
        logger.info("hello abc4");
        logger.info("abc5 world");
        logger.info("hello def6");
        logger.info("ghi7 world"); // no match
        logger.info("def8 world");

        // "append" should always be called
        assertEquals(8, count.get());

        assertEquals(strList("abc1", "def3", "abc4", "abc5", "def6", "def8"), appender.getExtracted());

        appender.setPattern("ghi[0-9]");
        logger.info("hello abc9");
        logger.info("hello ghi9");

        // this should not match
        logger.info("hello xyz");

        assertEquals(11, count.get());
        assertEquals(strList("abc1", "def3", "abc4", "abc5", "def6", "def8", "abc9", "ghi9"), appender.getExtracted());
    }

    @Test
    void testExtractAppenderQueueStringArray() {
        // no. of matches allowed in the list
        int nallowed = 3;

        AtomicInteger count = new AtomicInteger(0);

        LinkedList<String> queue = new LinkedList<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean offer(String element) {
                if (count.incrementAndGet() <= nallowed) {
                    return super.offer(element);

                } else {
                    return false;
                }
            }
        };

        ExtractAppender appender = new ExtractAppender(queue, ABC_DIGIT);
        addAppender(appender);

        // these shouldn't match
        for (int x = 0; x < 10; ++x) {
            logger.info("xyz");
        }

        int nmatches = 10;

        LinkedList<String> expected = new LinkedList<>();

        for (int x = 0; x < nmatches; ++x) {
            String msg = "abc" + x;
            logger.info("{} world", msg);

            if (x < nallowed) {
                expected.add(msg);
            }
        }

        // "offer" should always be called for a match
        assertEquals(nmatches, count.get());

        assertEquals(expected, appender.getExtracted());
    }

    @Test
    void testAppendILoggingEvent_NoPatterns() {
        ExtractAppender appender = makeAppender();

        logger.info(HELLO);
        logger.info(WORLD);

        assertEquals(strList(HELLO, WORLD), appender.getExtracted());
    }

    @Test
    void testAppendILoggingEvent_Formatted() {
        ExtractAppender appender = makeAppender();

        logger.info("hello {} world{}", "there", "!");

        assertEquals(strList("hello there world!"), appender.getExtracted());
    }

    @Test
    void testAppendILoggingEvent_MatchFirstPattern() {
        ExtractAppender appender = makeAppender(ABC_DIGIT, DEF_DIGIT);

        logger.info("hello abc1");
        logger.info("world xyz2");

        assertEquals(strList("abc1"), appender.getExtracted());
    }

    @Test
    void testAppendILoggingEvent_MatchLastPattern() {
        ExtractAppender appender = makeAppender(ABC_DIGIT, DEF_DIGIT);

        logger.info("hello def1");
        logger.info("world xyz2");

        assertEquals(strList("def1"), appender.getExtracted());
    }

    @Test
    void testAppendILoggingEvent_Group1() {
        ExtractAppender appender = makeAppender("hello (abc)|(xyz)", DEF_DIGIT);

        logger.info("hello abc, world!");
        logger.info(WORLD_ABC);

        assertEquals(strList("abc"), appender.getExtracted());
    }

    @Test
    void testAppendILoggingEvent_Group3() {
        ExtractAppender appender = makeAppender("hello (abc)|(pdq)|(xyz)", DEF_DIGIT);

        logger.info("say hello xyz, world!");
        logger.info(WORLD_ABC);

        assertEquals(strList("xyz"), appender.getExtracted());
    }

    @Test
    void testAppendILoggingEvent_NoGroup() {
        ExtractAppender appender = makeAppender(HELLO_ABC);

        logger.info("say hello abc, world!");
        logger.info(WORLD_ABC);

        assertEquals(strList(HELLO_ABC), appender.getExtracted());
    }

    @Test
    void testGetExtracted() {
        ExtractAppender appender = makeAppender(ABC_DIGIT1);

        logger.info(HELLO_ABC1_WORLD);
        logger.info(WORLD_GHI2_WORLD); // no match
        logger.info(HELLO_ABC3);

        List<String> oldlst = appender.getExtracted();
        assertEquals(strList("abc1", "abc3"), oldlst);
        assertEquals(oldlst, appender.getExtracted());

        logger.info("abc9");
        assertEquals(strList("abc1", "abc3", "abc9"), appender.getExtracted());
    }

    @Test
    void testClearExtractions() {
        final ExtractAppender appender = makeAppender(ABC_DIGIT1);

        logger.info(HELLO_ABC1_WORLD);
        logger.info(WORLD_GHI2_WORLD);
        logger.info(HELLO_ABC3);

        assertEquals(strList("abc1", "abc3"), appender.getExtracted());

        appender.clearExtractions();

        // list should be empty now
        assertEquals(strList(), appender.getExtracted());

        logger.info("hello abc4 world");
        logger.info("world ghi5 world");
        logger.info("hello abc6");

        // list should only contain the new items
        assertEquals(strList("abc4", "abc6"), appender.getExtracted());
    }

    @Test
    void testSetPattern() {
        final ExtractAppender appender = makeAppender(ABC_DIGIT1);

        logger.info(HELLO_ABC1_WORLD);
        logger.info(WORLD_GHI2_WORLD); // no match
        logger.info(HELLO_ABC3);

        assertEquals(strList("abc1", "abc3"), appender.getExtracted());

        appender.setPattern("ghi[0-9]");

        logger.info("world ghi4 world"); // this should match now
        logger.info("hello abc5"); // this should still match
        logger.info("hello xyz5"); // no match

        assertEquals(strList("abc1", "abc3", "ghi4", "abc5"), appender.getExtracted());
    }

    /**
     * Launches threads doing everything in parallel to ensure nothing crashes.
     */
    @Test
    void test_MultiThreaded() throws Exception {
        // when to stop
        long tend = System.currentTimeMillis() + 250;

        // maximum number of items allowed in the extraction list
        int maxItems = 10;

        // this will be set if one of the threads generates an error
        AtomicBoolean err = new AtomicBoolean(false);

        // extracted messages go here - this is a finite-length queue since
        // we don't know how many messages may actually be logged
        LinkedList<String> queue = new LinkedList<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean offer(String element) {
                if (size() < maxItems) {
                    return super.offer(element);
                } else {
                    return false;
                }
            }
        };

        ExtractAppender app = new ExtractAppender(queue, ABC_DIGIT1);
        addAppender(app);

        // create some threads to add another pattern
        addThread(tend, err, xtxt -> app.setPattern(DEF_DIGIT));

        // create some threads to log "abc" messages
        addThread(tend, err, xtxt -> logger.info("{}{}world!", HELLO_ABC, xtxt));

        // create some threads to log "def" messages
        addThread(tend, err, xtxt -> logger.info("hello def{}world!", xtxt));

        // create some threads to get extractions
        addThread(tend, err, xtxt -> app.getExtracted());

        /*
         * Finally ready to start.
         */

        // start all of the threads
        for (Thread t : threads) {
            t.setDaemon(true);
            t.start();
        }

        // wait for each thread to stop
        for (Thread t : threads) {
            t.join(THREAD_WAIT_MS);
            assertFalse(t.isAlive());
        }

        // ensure none of the threads threw an exception
        assertFalse(err.get());
    }

    /**
     * Adds multiple threads to perform some function repeatedly until the given time is reached.
     *
     * @param tend time, in milliseconds, when the test should terminate
     * @param haderr this will be set to {@code true} if the function throws an exception other than
     *        an InterruptedException
     * @param func function to be repeatedly invoked
     */
    private void addThread(long tend, AtomicBoolean haderr, VoidFunction func) {
        // number of threads of each type to create
        int neach = 3;

        for (int x = 0; x < neach; ++x) {
            String xtxt = String.valueOf(x);

            threads.add(new Thread() {
                @Override
                public void run() {
                    try {
                        while (System.currentTimeMillis() < tend) {
                            func.apply(xtxt);
                        }

                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();

                    } catch (Exception ex) {
                        haderr.set(true);
                    }
                }
            });

        }
    }

    /**
     * Makes an appender that recognizes the given set of strings.
     *
     * @param strings regular expressions to be matched
     * @return a new appender
     */
    private ExtractAppender makeAppender(String... strings) {
        ExtractAppender appender = new ExtractAppender(strings);

        addAppender(appender);

        return appender;
    }

    /**
     * Adds an appender to the logger.
     *
     * @param app appender to be added
     */
    private void addAppender(ExtractAppender app) {
        app.setContext(logger.getLoggerContext());
        app.start();

        logger.addAppender(app);
    }

    /**
     * Converts an array of strings into a list of strings.
     *
     * @param strings array of strings
     * @return a list of the strings
     */
    private List<String> strList(String... strings) {
        return Arrays.asList(strings);
    }

    @FunctionalInterface
    public interface VoidFunction {
        public void apply(String text) throws InterruptedException;
    }
}
