/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoderTest {
    private static final Long LONG = 10L;
    private static final Integer INTEGER = 10;
    private static final String INT_TEXT = INTEGER.toString();
    private static final String TEXT = "some text";
    private static final String ENCODED = "encoded value";
    private static final String DECODED = "decoded value";

    private MyCoder coder;

    @BeforeEach
    void setUp() {
        coder = new MyCoder();
    }

    @Test
    void testConvert() throws CoderException {
        assertNull(coder.convert(null, String.class));

        // same class of object
        assertEquals(TEXT, coder.convert(TEXT, String.class));
        assertEquals(INTEGER, coder.convert(INTEGER, Integer.class));

        // source is a string
        assertEquals(INTEGER, coder.convert(TEXT, Integer.class));

        // target is a string
        assertEquals(INT_TEXT, coder.convert(INTEGER, String.class));

        // source and target are different types, neither is a string
        assertEquals(INTEGER, coder.convert(LONG, Integer.class));
    }

    private static class MyCoder implements Coder {
        @Override
        public String encode(Object object) throws CoderException {
            return (object.getClass() == String.class ? ENCODED : INT_TEXT);
        }

        @Override
        public String encode(Object object, boolean pretty) throws CoderException {
            // unused
            return null;
        }

        @Override
        public void encode(Writer target, Object object) throws CoderException {
            // unused
        }

        @Override
        public void encode(OutputStream target, Object object) throws CoderException {
            // unused
        }

        @Override
        public void encode(File target, Object object) throws CoderException {
            // unused
        }

        @Override
        public <T> T decode(String json, Class<T> clazz) throws CoderException {
            return (clazz == String.class ? clazz.cast(DECODED) : clazz.cast(INTEGER));
        }

        @Override
        public <T> T decode(Reader source, Class<T> clazz) throws CoderException {
            // unused
            return null;
        }

        @Override
        public <T> T decode(InputStream source, Class<T> clazz) throws CoderException {
            // unused
            return null;
        }

        @Override
        public <T> T decode(File source, Class<T> clazz) throws CoderException {
            // unused
            return null;
        }

        @Override
        public StandardCoderObject toStandard(Object object) throws CoderException {
            // unused
            return null;
        }

        @Override
        public <T> T fromStandard(StandardCoderObject sco, Class<T> clazz) throws CoderException {
            // unused
            return null;
        }
    }
}
