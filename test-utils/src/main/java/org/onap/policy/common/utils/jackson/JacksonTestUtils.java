/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.utils.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * Utilities used to test encoding and decoding of Policy objects.
 */
@Getter
public class JacksonTestUtils {

    /**
     * Engine used to interpolate strings before they're compared.
     */

    private final ObjectMapper objectMapper;

    /**
     * Constructs the object.
     */
    public JacksonTestUtils() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Encodes an object using jackson and then compares it to the expected value, after
     * sorting the elements. The class name is used to find the json file, whose contents
     * is interpolated (i.e., script elements, of the form ${obj.xxx}, are expanded).
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareJson(Object object, Class<?> expected) throws JsonParseException {
        compareJson(object, new File(expected.getSimpleName() + ".json"));
    }

    /**
     * Encodes an object using jackson and then compares it to the expected value, after
     * sorting the elements. The content of the file is interpolated (i.e., script
     * elements, of the form ${obj.xxx}, are expanded).
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareJson(Object object, File expected) throws JsonParseException {
        var url = object.getClass().getResource(expected.getName());
        if (url == null) {
            throw new JsonParseException(new FileNotFoundException(expected.getName()).getMessage());
        }

        String expectedText;
        try {
            expectedText = readFile(new File(url.getFile()));
        } catch (IOException e) {
            throw new JsonParseException("error reading: " + e.getMessage());
        }

        compareJson(object, expectedText);
    }

    /**
     * Encodes an object using jackson and then compares it to the expected value, after
     * sorting the elements. The expected value is interpolated (i.e., script elements, of
     * the form ${obj.xxx}, are expanded).
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareJson(Object object, String expected) throws JsonParseException {
        try {
            compareJson(object, objectMapper.readTree(expected));
        } catch (IOException e) {
            throw new JsonParseException(e.getMessage());
        }
    }

    /**
     * Encodes an object using jackson and then compares it to the expected value, after
     * sorting the elements.
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareJson(Object object, JsonNode expected) throws JsonParseException {
        try {
            var json = objectMapper.writeValueAsString(object);

            var actualNode = reorder(objectMapper.readTree(json));
            var expectedNode = reorder(expected);

            /*
             * As this method is only used within junit tests, it is OK to use assert calls,
             * thus sonar is disabled.
             */
            assertEquals(expectedNode.toString(), actualNode.toString()); // NOSONAR
        } catch (IOException e) {
            throw new JsonParseException(e.getMessage());
        }
    }



    /**
     * Reads the content of a file.
     * @param file file to read
     * @return the content of the file
     * @throws IOException if an error occurs
     */
    protected String readFile(File file) throws IOException {
        return Files.readString(file.toPath());
    }


    /**
     * Recursively re-orders a JSON object, arranging the keys alphabetically
     * and removing null items.
     */
    public ObjectNode reorder(ObjectNode jsonObj) {
        var newObj = objectMapper.createObjectNode();

        // Collect and sort fields by key
        List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
        jsonObj.fields().forEachRemaining(fields::add);
        fields.sort(Map.Entry.comparingByKey());

        for (var entry : fields) {
            var value = entry.getValue();
            if (value == null || value.isNull()) {
                continue;
            }

            newObj.set(entry.getKey(), reorder(value));
        }

        return newObj;
    }

    /**
     * Recursively re-orders a JSON array, removing null items
     * from all elements.
     */
    public ArrayNode reorder(ArrayNode jsonArray) {
        var newArr = objectMapper.createArrayNode();

        for (var element : jsonArray) {
            if (element == null || element.isNull()) {
                continue;
            }
            newArr.add(reorder(element));
        }

        return newArr;
    }

    /**
     * Recursively re-orders a JSON node, arranging object keys
     * alphabetically and removing null items.
     */
    public JsonNode reorder(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return NullNode.getInstance();
        }

        if (jsonNode.isObject()) {
            return reorder((ObjectNode) jsonNode);
        }

        if (jsonNode.isArray()) {
            return reorder((ArrayNode) jsonNode);
        }

        // Primitive nodes are returned as-is
        return jsonNode;
    }

}
