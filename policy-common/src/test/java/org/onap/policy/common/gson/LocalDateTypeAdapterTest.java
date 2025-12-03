/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import java.time.LocalDate;
import lombok.ToString;
import org.junit.jupiter.api.Test;

class LocalDateTypeAdapterTest {
    private static Gson gson =
            new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter()).create();
    private static final String TEST_DATE = "2020-01-01";

    @Test
    void test() {
        InterestingFields data = new InterestingFields();
        data.date = LocalDate.parse(TEST_DATE);

        String json = gson.toJson(data);

        // instant should be encoded as a number, without quotes
        assertThat(json).doesNotContain("year").contains(TEST_DATE);

        InterestingFields data2 = gson.fromJson(json, InterestingFields.class);
        assertEquals(data.toString(), data2.toString());

        // try when the date-time string is invalid
        String json2 = json.replace("2020", "invalid-date");
        assertThatThrownBy(() -> gson.fromJson(json2, InterestingFields.class)).isInstanceOf(JsonParseException.class)
                        .hasMessageContaining("invalid date");

        // null output
        data.date = null;
        json = gson.toJson(data);
        data2 = gson.fromJson(json, InterestingFields.class);
        assertEquals(data.toString(), data2.toString());

        // null input
        data2 = gson.fromJson("{\"date\":null}", InterestingFields.class);
        assertEquals(data.toString(), data2.toString());
    }

    @ToString
    private static class InterestingFields {
        private LocalDate date;
    }

}
