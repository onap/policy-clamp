/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GsonTestUtilsBuilderTest {

    private GsonTestUtils utils;

    @BeforeEach
    public void setUp() {
        utils = new MyBuilder().build();
    }

    @Test
    void testBuilderAddMock() {
        PreMock pre = mock(PreMock.class);
        when(pre.getId()).thenReturn(2000);

        assertEquals("{\"name\":2000}", utils.gsonEncode(pre));
    }

    /**
     * Builder that provides an adapter for mock(PreMock.class).
     */
    private static class MyBuilder extends GsonTestUtilsBuilder {
        public MyBuilder() {
            TypeAdapterFactory sgson = new TypeAdapterFactory() {
                @Override
                public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                    Class<? super T> clazz = type.getRawType();

                    if (PreMock.class.isAssignableFrom(clazz)) {
                        return new GsonSerializer<T>() {
                            @Override
                            public void write(JsonWriter out, T value) throws IOException {
                                PreMock obj = (PreMock) value;
                                out.beginObject().name("name").value(obj.getId()).endObject();
                            }
                        };
                    }

                    return null;
                }
            };

            addMock(PreMock.class, sgson);
        }
    }

    /**
     * Class that will be mocked.
     */
    public static class PreMock {
        protected int id = 1000;

        public int getId() {
            return id;
        }
    }
}
