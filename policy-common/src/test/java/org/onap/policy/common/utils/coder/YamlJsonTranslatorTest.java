/*-
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

package org.onap.policy.common.utils.coder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.error.YAMLException;

class YamlJsonTranslatorTest {
    private static final File YAML_FILE =
                    new File("src/test/resources/org/onap/policy/common/utils/coder/YamlJsonTranslator.yaml");

    private Container cont;
    private YamlJsonTranslator translator;

    /**
     * Creates {@link #translator} and uses it to load {@link #cont}.
     *
     * @throws IOException if an error occurs
     */
    @BeforeEach
    public void setUp() throws IOException {
        translator = new YamlJsonTranslator();

        try (FileReader rdr = new FileReader(YAML_FILE)) {
            cont = translator.fromYaml(rdr, Container.class);
        }
    }

    @Test
    void testToYamlObject() {
        String yaml = translator.toYaml(cont);

        Container cont2 = translator.fromYaml(yaml, Container.class);
        assertEquals(cont, cont2);
    }

    @Test
    void testToYamlWriterObject() throws IOException {
        IOException ex = new IOException("expected exception");

        // writer that throws an exception when the write() method is invoked
        Writer wtr = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw ex;
            }

            @Override
            public void flush() throws IOException {
                // do nothing
            }

            @Override
            public void close() throws IOException {
                // do nothing
            }
        };

        assertThatThrownBy(() -> translator.toYaml(wtr, cont)).isInstanceOf(YAMLException.class);

        wtr.close();
    }

    @Test
    void testFromYamlStringClassOfT() throws IOException {
        String yaml = new String(Files.readAllBytes(YAML_FILE.toPath()), StandardCharsets.UTF_8);
        Container cont2 = translator.fromYaml(yaml, Container.class);
        assertEquals(cont, cont2);
    }

    @Test
    void testFromYamlReaderClassOfT() {
        verify(cont);
    }

    /**
     * Verifies that the container has contents matching the yaml file.
     *
     * @param container container whose contents are to be verified
     */
    public static void verify(Container container) {
        assertNotNull(container.item);
        assertTrue(container.item.boolVal);
        assertEquals(1000L, container.item.longVal);
        assertEquals(1010.1f, container.item.floatVal, 0.00001);

        assertEquals(4, container.list.size());
        assertNull(container.list.get(1));

        assertEquals(20, container.list.get(0).intVal);
        assertEquals("string 30", container.list.get(0).stringVal);
        assertNull(container.list.get(0).nullVal);

        assertEquals(40.0, container.list.get(2).doubleVal, 0.000001);
        assertNull(container.list.get(2).nullVal);
        assertNotNull(container.list.get(2).another);
        assertEquals(50, container.list.get(2).another.intVal);

        assertTrue(container.list.get(3).boolVal);

        assertNotNull(container.map);
        assertEquals(3, container.map.size());

        assertNotNull(container.map.get("itemA"));
        assertEquals("stringA", container.map.get("itemA").stringVal);

        assertNotNull(container.map.get("itemB"));
        assertEquals("stringB", container.map.get("itemB").stringVal);

        double dbl = 123456789012345678901234567890.0;
        assertEquals(dbl, container.map.get("itemB").doubleVal, 1000.0);

        assertNotNull(container.map.get("itemC"));
        assertTrue(container.map.get("itemC").boolVal);
    }


    @EqualsAndHashCode
    public static class Container {
        protected Item item;
        protected List<Item> list;
        protected Map<String, Item> map;
    }

    @EqualsAndHashCode
    public static class Item {
        protected boolean boolVal;
        protected int intVal;
        protected long longVal;
        protected double doubleVal;
        protected float floatVal;
        protected String stringVal;
        protected Object nullVal;
        protected Item another;
    }
}
