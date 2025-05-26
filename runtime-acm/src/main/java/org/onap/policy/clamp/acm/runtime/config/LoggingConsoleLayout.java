/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

public class LoggingConsoleLayout extends LayoutBase<ILoggingEvent> {

    private static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS+00:00";

    @Setter
    private String timestampFormat = DEFAULT_FORMAT;

    @Setter
    private String timestampFormatTimezoneId = "";

    @Setter
    private String staticParameters = "";

    private final Map<String, String> staticParameterMap = new HashMap<>();

    private final Coder coder = new StandardCoder();

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_FORMAT);

    @Override
    public void start() {
        super.start();
        extractParameters();
        if (timestampFormat != null && !timestampFormat.isBlank()) {
            formatter = extractDateTimeFormatter();
        }
    }

    private void extractParameters() {
        staticParameterMap.clear();
        if (staticParameters == null || staticParameters.isBlank()) {
            return;
        }
        var split = staticParameters.split("\\|");
        for (var str : split) {
            var s = str.split("=");
            if (s.length == 2) {
                staticParameterMap.put(s[0], s[1]);
            }
        }
    }

    private DateTimeFormatter extractDateTimeFormatter() {
        try {
            var dtf = DateTimeFormatter.ofPattern(timestampFormat);
            if (timestampFormatTimezoneId != null && !timestampFormatTimezoneId.isBlank()) {
                dtf = dtf.withZone(ZoneId.of(timestampFormatTimezoneId));
            }
            return dtf;
        } catch (RuntimeException e) {
            return DateTimeFormatter.ofPattern(DEFAULT_FORMAT);
        }
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", getTimestamp(event.getInstant()));
        map.put("severity", event.getLevel().toString());
        map.put("message", event.getFormattedMessage());
        Map<String, Object> extraDatamap = new HashMap<>();
        extraDatamap.put("logger", event.getLoggerName());
        extraDatamap.put("thread", event.getThreadName());
        var throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            var m = Map.of("stack_trace", ThrowableProxyUtil.asString(throwableProxy));
            extraDatamap.put("exception", m);
        }
        map.put("extra_data", extraDatamap);
        map.putAll(staticParameterMap);
        return getJson(map);
    }

    private String getJson(Map<String, Object> map) {
        try {
            return coder.encode(map) + CoreConstants.LINE_SEPARATOR;
        } catch (CoderException e) {
            return map.get("message").toString();
        }
    }

    private String getTimestamp(Instant instant) {
        try {
            return formatter.format(instant);
        } catch (RuntimeException e) {
            return instant.toString();
        }
    }
}
