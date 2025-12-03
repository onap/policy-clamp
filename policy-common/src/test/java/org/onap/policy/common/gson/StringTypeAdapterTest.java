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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.junit.jupiter.api.Test;

class StringTypeAdapterTest {
    private static Gson gson = new GsonBuilder().registerTypeAdapter(MyData.class, new MyAdapter()).create();
    private static final int TEST_NUM1 = 10;
    private static final int TEST_NUM3 = 30;

    @Test
    void test() {
        InterestingFields data = new InterestingFields();
        data.data1 = new MyData(TEST_NUM1);
        data.data2 = null;
        data.data3 = new MyData(TEST_NUM3);

        String json = gson.toJson(data);

        // instant should be encoded as a number, without quotes
        assertThat(json).contains("10", "30");

        InterestingFields data2 = gson.fromJson(json, InterestingFields.class);
        assertEquals(data.toString(), data2.toString());

        // try when the string is invalid
        String json2 = json.replace("30", "invalid-value");
        assertThatThrownBy(() -> gson.fromJson(json2, InterestingFields.class)).isInstanceOf(JsonParseException.class)
                        .hasMessageContaining("invalid data");

        // null output
        data = new InterestingFields();
        json = gson.toJson(data);
        data2 = gson.fromJson(json, InterestingFields.class);
        assertEquals(data.toString(), data2.toString());

        // null input
        data2 = gson.fromJson("{\"data1\":null, \"data1\":null, \"data1\":null}", InterestingFields.class);
        assertEquals(data.toString(), data2.toString());

        // empty input
        data2 = gson.fromJson("{}", InterestingFields.class);
        assertEquals(data.toString(), data2.toString());
    }

    @Getter
    @ToString
    @AllArgsConstructor
    private static class MyData {
        private int num;
    }

    @ToString
    private static class InterestingFields {
        private MyData data1;
        private MyData data2;
        private MyData data3;
    }

    private static class MyAdapter extends StringTypeAdapter<MyData> {
        public MyAdapter() {
            super("data", string -> new MyData(Integer.parseInt(string)), data -> String.valueOf(data.num));
        }
    }
}
