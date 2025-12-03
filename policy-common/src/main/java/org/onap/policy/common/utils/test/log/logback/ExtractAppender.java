/*
 * ============LICENSE_START====================================================
 * Common Utils-Test
 * =============================================================================
 * Copyright (C) 2018-2019, 2021 AT&T Intellectual Property. All rights reserved.
 * =============================================================================
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
 * ============LICENSE_END======================================================
 */

package org.onap.policy.common.utils.test.log.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This is an appender that is intended for use by JUnit tests that wish to
 * capture logged messages. The appender takes an optional list of regular
 * expressions that are used to identify and extract data of interest.
 * <p/>
 * If no patterns are provided, then every logged message is recorded. However,
 * if patterns are provided, then only messages that match one of the patterns
 * are recorded. In addition, if the pattern contains a capture group that is
 * non-null, only the captured group is recorded. Otherwise, the entire portion
 * of the message that matches the pattern is recorded.
 * <p/>
 * All operations are thread-safe.
 */
public class ExtractAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Extracted text is placed here.
     */
    private final Queue<String> extracted;

    /**
     * Regular expressions/Patterns to be used to extract text. Uses a
     * LinkedHashMap so that order is preserved.
     */
    private final LinkedHashMap<String, Pattern> patterns;

    /**
     * Records every message that is logged.
     */
    public ExtractAppender() {
        this(new LinkedList<>());
    }

    /**
     * Records portions of messages that match one of the regular
     * expressions.
     *
     * @param regex
     *            regular expression (i.e., {@link Pattern}) to match
     */
    public ExtractAppender(final String... regex) {
        this(new LinkedList<>(), regex);
    }

    /**
     * Rather than allocating an internal queue to store matched messages,
     * messages are recorded in the specified target queue using the
     * {@link Queue#offer(Object)} method. Note: whenever the queue is used,
     * it will be synchronized to prevent simultaneous accesses.
     *
     * @param target - queue into which the matched text should be placed
     * @param regex regular expression (i.e., {@link Pattern}) to match
     */
    public ExtractAppender(final Queue<String> target,
                    final String... regex) {
        extracted = target;
        patterns = new LinkedHashMap<>(regex.length);

        for (String re : regex) {
            patterns.put(re, Pattern.compile(re));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see ch.qos.logback.core.AppenderBase#append(Object)
     */
    @Override
    protected void append(final ILoggingEvent event) {

        String msg = event.getFormattedMessage();

        synchronized (patterns) {
            if (patterns.isEmpty()) {
                addExtraction(msg);
                return;
            }

            for (Pattern p : patterns.values()) {
                var matcher = p.matcher(msg);

                if (matcher.find()) {
                    addGroupMatch(matcher);
                    break;
                }
            }
        }
    }

    /**
     * Adds the first match group to {@link #extracted}.
     *
     * @param mat the matcher containing the groups
     *
     */
    private void addGroupMatch(final Matcher mat) {
        int ngroups = mat.groupCount();

        for (var x = 1; x <= ngroups; ++x) {
            String txt = mat.group(x);

            if (txt != null) {
                addExtraction(txt);
                return;
            }
        }

        addExtraction(mat.group());
    }

    /**
     * Adds an item to {@link #extracted}, in a thread-safe manner.
     * It uses the queue's <i>offer()</i> method so that the queue
     * can discard the item if it so chooses, without generating
     * an exception.
     *
     * @param txt
     *            text to be added
     */
    private void addExtraction(final String txt) {
        synchronized (extracted) {
            extracted.offer(txt);
        }
    }

    /**
     * Gets the text that has been extracted.
     *
     * @return a copy of the text that has been extracted
     */
    public List<String> getExtracted() {
        synchronized (extracted) {
            return new ArrayList<>(extracted);
        }
    }

    /**
     * Clears the list of extracted text.
     */
    public void clearExtractions() {
        synchronized (extracted) {
            extracted.clear();
        }
    }

    /**
     * Adds a pattern to be matched by this appender.
     *
     * @param regex
     *            regular expression (i.e., {@link Pattern}) to match
     */
    public void setPattern(final String regex) {
        synchronized (patterns) {
            patterns.put(regex, Pattern.compile(regex));
        }
    }

}
