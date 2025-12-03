/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.gson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.ToString;
import org.junit.jupiter.api.Test;

class ZonedDateTimeTypeAdapterTest {
    private static Gson gson =
                    new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter()).create();

    @Test
    void test() {
        InterestingFields data = new InterestingFields();
        data.date = ZonedDateTime.of(2020, 2, 3, 4, 5, 6, 789000000, ZoneId.of("US/Eastern"));

        String json = gson.toJson(data);

        // instant should be encoded as a number, without quotes
        assertThat(json).doesNotContain("year").contains("\"2020-02-03T04:05:06.789-05:00[US/Eastern]\"");

        InterestingFields data2 = gson.fromJson(json, InterestingFields.class);
        assertEquals(data.toString(), data2.toString());

        // try when the date-time string is invalid
        String json2 = json.replace("2020", "invalid-date");
        assertThatThrownBy(() -> gson.fromJson(json2, InterestingFields.class)).isInstanceOf(JsonParseException.class)
                        .hasMessageContaining("invalid date");
    }


    @ToString
    private static class InterestingFields {
        private ZonedDateTime date;
    }
}
