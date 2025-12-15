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

package org.onap.policy.common.utils.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.re2j.Pattern;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;

/**
 * Utilities used to test encoding and decoding of Policy objects.
 */
@Getter
public class JacksonTestUtils {

    /**
     * Matches script items, of the form ${xxx}, within text.
     */
    private static final Pattern SCRIPT_PAT = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Engine used to interpolate strings before they're compared.
     */
    private static JexlEngine engineInstance = null;

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
        var result = applyScripts(expected, object);
        try {
            compareJson(object, objectMapper.readTree(result));
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
     * Interpolates script elements, of the form ${obj.xxx}, within some text. The script
     * is evaluated via javascript, where "obj" references the object used by each script
     * element.
     *
     * @param object object to be used by the script
     * @param text text to be evaluated
     * @return the text, after interpolating the script elements
     */
    public String applyScripts(String text, Object object) {
        var mat = SCRIPT_PAT.matcher(text);
        if (!mat.find()) {
            // contains no script elements - just return it as is
            return text;
        }

        // bind the object to the variable, "obj"
        JexlEngine eng = getEngine();
        JexlContext context = new MapContext();
        context.set("obj", object);

        // work our way through the text, interpolating script elements as we go
        var bldr = new StringBuilder();
        var ilast = 0;
        mat.reset();
        while (mat.find(ilast)) {
            // append segment that appears between last match and this
            int inext = mat.start();
            bldr.append(text, ilast, inext);

            // next match begins after the current match
            ilast = mat.end();

            // interpolate the script
            String script = mat.group(1);
            /*
             * Note: must use "eng" instead of "engineInstance" to ensure that we use
             * the same engine that's associated with the bindings.
             */
            Object result = eng.createExpression(script).evaluate(context);
            bldr.append(result == null ? "null" : result.toString());
        }

        // append final segment
        bldr.append(text.substring(ilast));

        return bldr.toString();
    }

    /**
     * Gets the script engine instance.
     *
     * @return the script engine
     */
    private static JexlEngine getEngine() {
        if (engineInstance == null) {
            // race condition here, but it's ok to overwrite with a new engine
            engineInstance = new JexlBuilder().create();
        }

        return engineInstance;
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
