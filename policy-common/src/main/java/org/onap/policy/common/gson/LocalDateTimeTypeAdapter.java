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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GSON Type Adapter for "LocalDateTime" fields, that uses the standard
 * ISO_LOCAL_DATE_TIME formatter, by default.
 */
public class LocalDateTimeTypeAdapter extends StringTypeAdapter<LocalDateTime> {

    public LocalDateTimeTypeAdapter() {
        this(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public LocalDateTimeTypeAdapter(DateTimeFormatter formatter) {
        super("date", string -> LocalDateTime.parse(string, formatter), value -> value.format(formatter));
    }
}
