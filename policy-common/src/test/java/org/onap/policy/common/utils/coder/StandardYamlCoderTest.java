/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.YamlJsonTranslatorTest.Container;

class StandardYamlCoderTest {
    private static final File YAML_FILE =
                    new File("src/test/resources/org/onap/policy/common/utils/coder/YamlJsonTranslator.yaml");

    private StandardYamlCoder coder;
    private Container cont;

    @BeforeEach
    public void setUp() throws CoderException {
        coder = new StandardYamlCoder();
        cont = coder.decode(YAML_FILE, Container.class);
    }

    @Test
    void testToPrettyJson() throws CoderException {
        String expected = coder.encode(cont);
        assertEquals(expected, coder.encode(cont, false));

        String yaml = coder.encode(cont, true);
        assertEquals(expected, yaml);

        Container cont2 = coder.decode(yaml, Container.class);
        assertEquals(cont, cont2);

        // test exception cases
        IllegalArgumentException expex = new IllegalArgumentException("expected exception");
        coder = spy(new StandardYamlCoder());
        when(coder.toJson(cont)).thenThrow(expex);
        assertThatThrownBy(() -> coder.encode(cont, false)).isInstanceOf(CoderException.class).hasCause(expex);
        assertThatThrownBy(() -> coder.encode(cont, true)).isInstanceOf(CoderException.class).hasCause(expex);
    }

    @Test
    void testToJsonObject() throws CoderException {
        String yaml = coder.encode(cont);

        Container cont2 = coder.decode(yaml, Container.class);
        assertEquals(cont, cont2);
    }

    @Test
    void testToJsonWriterObject() throws CoderException {
        StringWriter wtr = new StringWriter();
        coder.encode(wtr, cont);
        String yaml = wtr.toString();

        Container cont2 = coder.decode(yaml, Container.class);
        assertEquals(cont, cont2);
    }

    @Test
    void testFromJsonStringClassOfT() throws Exception {
        String yaml = new String(Files.readAllBytes(YAML_FILE.toPath()), StandardCharsets.UTF_8);
        Container cont2 = coder.decode(yaml, Container.class);
        assertEquals(cont, cont2);
    }

    @Test
    void testFromJsonReaderClassOfT() {
        YamlJsonTranslatorTest.verify(cont);
    }

    @Test
    void testFromJsonDoubleToInteger() throws Exception {
        Object value = coder.decode("20", Object.class);
        assertEquals(Integer.valueOf(20), value);
    }

    @Test
    void testStandardTypeAdapter() {
        String yaml = "abc: def\n";
        StandardCoderObject sco = coder.fromJson(yaml, StandardCoderObject.class);
        assertNotNull(sco.getData());
        assertEquals("{'abc':'def'}".replace('\'', '"'), sco.getData().toString());
        assertEquals(yaml, coder.toJson(sco));
    }
}
