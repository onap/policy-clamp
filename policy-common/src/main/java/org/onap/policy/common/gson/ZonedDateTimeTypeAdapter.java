/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.gson;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GSON Type Adapter for "ZonedDateTime" fields, that uses the standard
 * ISO_ZONED_DATE_TIME formatter.
 */
public class ZonedDateTimeTypeAdapter extends StringTypeAdapter<ZonedDateTime> {

    /**
     * Constructs an adapter that uses the ISO_ZONED_DATE_TIME formatter.
     */
    public ZonedDateTimeTypeAdapter() {
        this(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    /**
     * Constructs an adapter that uses the specified formatter for reading and writing.
     *
     * @param formatter date-time formatter
     */
    public ZonedDateTimeTypeAdapter(DateTimeFormatter formatter) {
        super("date", string -> ZonedDateTime.parse(string, formatter), value -> value.format(formatter));
    }
}
