/*-
 * ============LICENSE_START=======================================================
 * policy-management
 * ================================================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.re2j.Pattern;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities used to test encoding and decoding of Policy objects.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class GsonTestUtils {

    private static final Logger logger = LoggerFactory.getLogger(GsonTestUtils.class);

    /**
     * Matches script items, of the form ${xxx}, within text.
     */
    private static final Pattern SCRIPT_PAT = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Engine used to interpolate strings before they're compared.
     */
    private static JexlEngine engineInstance = null;

    /**
     * Used to encode and decode an object via gson.
     */
    private Gson gson;

    /**
     * Constructs the object.
     */
    public GsonTestUtils() {
        GsonTestUtils other = new GsonTestUtilsBuilder().build();

        gson = other.gson;
    }

    /**
     * Serializes and then deserializes an object using gson.
     *
     * @param object the object to be serialized
     * @param clazz the class of object to deserialize
     * @return the deserialized object
     */
    public <T> T gsonRoundTrip(T object, Class<T> clazz) {
        String sgson = gsonEncode(object);
        return gson.fromJson(sgson, clazz);
    }

    /**
     * Encodes an object using gson and then compares it to the expected value, after
     * sorting the elements. The class name is used to find the json file, whose contents
     * is interpolated (i.e., script elements, of the form ${obj.xxx}, are expanded).
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareGson(Object object, Class<?> expected) {
        compareGson(object, new File(expected.getSimpleName() + ".json"));
    }

    /**
     * Encodes an object using gson and then compares it to the expected value, after
     * sorting the elements. The content of the file is interpolated (i.e., script
     * elements, of the form ${obj.xxx}, are expanded).
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareGson(Object object, File expected) {
        // file is not required to have a full path - find it via getResource()
        var url = object.getClass().getResource(expected.getName());
        if (url == null) {
            throw new JsonParseException(new FileNotFoundException(expected.getName()));
        }

        String expectedText;
        try {
            expectedText = readFile(new File(url.getFile()));

        } catch (IOException e) {
            throw new JsonParseException("error reading: " + expected, e);
        }

        compareGson(object, expectedText);
    }

    /**
     * Encodes an object using gson and then compares it to the expected value, after
     * sorting the elements. The expected value is interpolated (i.e., script elements, of
     * the form ${obj.xxx}, are expanded).
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareGson(Object object, String expected) {
        String result = applyScripts(expected, object);
        compareGson(object, gson.fromJson(result, JsonElement.class));
    }

    /**
     * Encodes an object using gson and then compares it to the expected value, after
     * sorting the elements.
     *
     * @param object the object to be encoded
     * @param expected the expected value
     */
    public void compareGson(Object object, JsonElement expected) {
        String sgson = gsonEncode(object);

        JsonElement gsonjo = reorder(gson.fromJson(sgson, JsonElement.class));
        JsonElement expjo = reorder(expected);

        /*
         * As this method is only used within junit tests, it is OK to use assert calls,
         * thus sonar is disabled.
         */
        assertEquals(expjo.toString(), gsonjo.toString());      // NOSONAR
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
     * Encodes an object using gson.
     *
     * @param object the object to be encoded
     * @return the encoded object
     */
    public String gsonEncode(Object object) {
        String sgson = gson.toJson(object);
        logger.debug("gson={}", sgson);
        return sgson;
    }

    /**
     * Recursively re-orders a json object, arranging the keys alphabetically and removing
     * null items.
     *
     * @param jsonObj object from which nulls are to be removed
     * @return a new object, without the null items
     */
    public JsonObject reorder(JsonObject jsonObj) {
        var newjo = new JsonObject();

        // sort the keys before copying to the new object
        List<Entry<String, JsonElement>> sortedSet = new ArrayList<>(jsonObj.entrySet());
        sortedSet.sort(Entry.comparingByKey());

        for (Entry<String, JsonElement> ent : sortedSet) {
            JsonElement val = ent.getValue();
            if (val.isJsonNull()) {
                continue;
            }

            newjo.add(ent.getKey(), reorder(val));
        }

        return newjo;
    }

    /**
     * Recursively re-orders a json array, arranging the keys alphabetically and removing
     * null items.
     *
     * @param jsonArray array from which nulls are to be removed
     * @return a new array, with null items removed from all elements
     */
    public JsonArray reorder(JsonArray jsonArray) {
        var newarr = new JsonArray();
        for (JsonElement ent : jsonArray) {
            newarr.add(reorder(ent));
        }

        return newarr;
    }

    /**
     * Recursively re-orders a json element, arranging the keys alphabetically and
     * removing null items.
     *
     * @param jsonEl element from which nulls are to be removed
     * @return a new element, with null items removed
     */
    public JsonElement reorder(JsonElement jsonEl) {
        if (jsonEl == null) {
            return null;

        } else if (jsonEl.isJsonObject()) {
            return reorder(jsonEl.getAsJsonObject());

        } else if (jsonEl.isJsonArray()) {
            return reorder(jsonEl.getAsJsonArray());

        } else {
            return jsonEl;
        }
    }
}
