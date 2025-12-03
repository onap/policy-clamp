/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

public class OffsetTimeTypeAdapter extends StringTypeAdapter<OffsetTime> {

    public OffsetTimeTypeAdapter() {
        this(DateTimeFormatter.ISO_OFFSET_TIME);
    }

    public OffsetTimeTypeAdapter(DateTimeFormatter formatter) {
        super("time", string -> OffsetTime.parse(string, formatter), value -> value.format(formatter));
    }
}
