/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import lombok.AllArgsConstructor;
import org.onap.policy.common.gson.InstantTypeAdapter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

/**
 * YAML-JSON translator. The methods may throw either of the runtime exceptions,
 * YAMLException or JsonSyntaxException.
 * <p/>
 * Note: if the invoker wishes Double to be converted to Integer/Long when type
 * Object.class is requested, then a Gson object must be used that will perform the
 * translation. In addition, the {@link #convertFromDouble(Class, Object)} method should
 * be overridden with an appropriate conversion method.
 */
@AllArgsConstructor
public class YamlJsonTranslator {

    /**
     * Object to be used to translate between YAML and JsonElement.
     */
    private final Gson gson;

    /**
     * Constructs the object.
     */
    public YamlJsonTranslator() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
        gson = builder.create();
    }

    /**
     * Translates a POJO into a YAML String.
     *
     * @param object POJO to be translated
     * @return YAML representing the original object
     */
    public String toYaml(Object object) {
        var output = new StringWriter();
        toYaml(output, object);
        return output.toString();
    }

    /**
     * Serializes a POJO to a writer, as YAML.
     *
     * @param target target writer
     * @param object POJO to be translated
     */
    public void toYaml(Writer target, Object object) {
        var dumper = new DumperOptions();
        var serializer = new Serializer(new Emitter(target, dumper), new Resolver(), dumper, null);

        try {
            serializer.open();
            serializer.serialize(makeYaml(toJsonTree(object)));
            serializer.close();

        } catch (IOException e) {
            throw new YAMLException(e);
        }
    }

    /**
     * Translates a POJO into a JsonElement.
     *
     * @param object POJO to be translated
     * @return a JsonElement representing the original object
     */
    protected JsonElement toJsonTree(Object object) {
        return gson.toJsonTree(object);
    }

    /**
     * Translates a YAML string to a POJO.
     *
     * @param yaml YAML string to be translated
     * @param clazz class of POJO to be created
     * @return a POJO representing the original YAML
     */
    public <T> T fromYaml(String yaml, Class<T> clazz) {
        return fromYaml(new StringReader(yaml), clazz);
    }

    /**
     * Translates a YAML string, read from a reader, into a POJO.
     *
     * @param source source of the YAML string to be translated
     * @param clazz class of POJO to be created
     * @return a POJO representing the YAML read from the reader
     */
    public <T> T fromYaml(Reader source, Class<T> clazz) {
        var node = new Yaml().compose(source);
        return fromJson(makeJson(node), clazz);
    }

    /**
     * Translates a JsonElement to a POJO of the given class.
     *
     * @param jel element to be translated
     * @param clazz class of POJO to be created
     * @return a POJO representing the original element
     */
    protected <T> T fromJson(JsonElement jel, Class<T> clazz) {
        return convertFromDouble(clazz, gson.fromJson(jel, clazz));
    }

    /**
     * Converts a value from Double to Integer/Long, walking the value's contents if it's
     * a List/Map. Only applies if the specified class refers to the Object class.
     * Otherwise, it leaves the value unchanged.
     * <p/>
     * The default method simply returns the original value.
     *
     * @param clazz class of object to be decoded
     * @param value value to be converted
     * @return the converted value
     */
    protected <T> T convertFromDouble(Class<T> clazz, T value) {
        return value;
    }

    /**
     * Converts an arbitrary gson element into a corresponding Yaml node.
     *
     * @param jel gson element to be converted
     * @return a yaml node corresponding to the element
     */
    protected Node makeYaml(JsonElement jel) {
        if (jel.isJsonArray()) {
            return makeYamlSequence((JsonArray) jel);

        } else if (jel.isJsonObject()) {
            return makeYamlMap((JsonObject) jel);

        } else if (jel.isJsonPrimitive()) {
            return makeYamlPrim((JsonPrimitive) jel);

        } else {
            return new ScalarNode(Tag.NULL, "", null, null, DumperOptions.ScalarStyle.PLAIN);
        }
    }

    /**
     * Converts an arbitrary gson array into a corresponding Yaml sequence.
     *
     * @param jel gson element to be converted
     * @return a yaml node corresponding to the element
     */
    protected SequenceNode makeYamlSequence(JsonArray jel) {
        List<Node> nodes = new ArrayList<>(jel.size());
        jel.forEach(item -> nodes.add(makeYaml(item)));

        return new SequenceNode(Tag.SEQ, true, nodes, null, null, DumperOptions.FlowStyle.AUTO);
    }

    /**
     * Converts an arbitrary gson object into a corresponding Yaml map.
     *
     * @param jel gson element to be converted
     * @return a yaml node corresponding to the element
     */
    protected MappingNode makeYamlMap(JsonObject jel) {
        List<NodeTuple> nodes = new ArrayList<>(jel.size());

        for (Entry<String, JsonElement> entry : jel.entrySet()) {
            Node key = new ScalarNode(Tag.STR, entry.getKey(), null, null, DumperOptions.ScalarStyle.PLAIN);
            Node value = makeYaml(entry.getValue());

            nodes.add(new NodeTuple(key, value));
        }

        return new MappingNode(Tag.MAP, true, nodes, null, null, DumperOptions.FlowStyle.AUTO);
    }

    /**
     * Converts an arbitrary gson primitive into a corresponding Yaml scalar.
     *
     * @param jel gson element to be converted
     * @return a yaml node corresponding to the element
     */
    protected ScalarNode makeYamlPrim(JsonPrimitive jel) {
        Tag tag;
        if (jel.isNumber()) {
            Class<? extends Number> clazz = jel.getAsNumber().getClass();

            if (clazz == Double.class || clazz == Float.class) {
                tag = Tag.FLOAT;

            } else {
                tag = Tag.INT;
            }

        } else if (jel.isBoolean()) {
            tag = Tag.BOOL;

        } else {
            // treat anything else as a string
            tag = Tag.STR;
        }

        return new ScalarNode(tag, jel.getAsString(), null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    /**
     * Converts an arbitrary Yaml node into a corresponding gson element.
     *
     * @param node node to be converted
     * @return a gson element corresponding to the node
     */
    protected JsonElement makeJson(Node node) {
        if (node instanceof MappingNode mappingNode) {
            return makeJsonObject(mappingNode);

        } else if (node instanceof SequenceNode sequenceNode) {
            return makeJsonArray(sequenceNode);

        } else {
            return makeJsonPrim((ScalarNode) node);
        }

        // yaml doesn't appear to use anchor nodes when decoding so ignore them for now
    }

    /**
     * Converts a Yaml sequence into a corresponding gson array.
     *
     * @param node node to be converted
     * @return a gson element corresponding to the node
     */
    protected JsonArray makeJsonArray(SequenceNode node) {
        List<Node> nodes = node.getValue();

        var array = new JsonArray(nodes.size());
        nodes.forEach(subnode -> array.add(makeJson(subnode)));

        return array;
    }

    /**
     * Converts a Yaml map into a corresponding gson object.
     *
     * @param node node to be converted
     * @return a gson element corresponding to the node
     */
    protected JsonObject makeJsonObject(MappingNode node) {
        var obj = new JsonObject();

        for (NodeTuple tuple : node.getValue()) {
            var key = tuple.getKeyNode();
            String skey = ((ScalarNode) key).getValue();

            obj.add(skey, makeJson(tuple.getValueNode()));
        }

        return obj;
    }

    /**
     * Converts a Yaml scalar into a corresponding gson primitive.
     *
     * @param node node to be converted
     * @return a gson element corresponding to the node
     */
    protected JsonElement makeJsonPrim(ScalarNode node) {
        try {
            var tag = node.getTag();

            if (tag == Tag.INT) {
                return new JsonPrimitive(Long.valueOf(node.getValue()));

            } else if (tag == Tag.FLOAT) {
                return new JsonPrimitive(Double.valueOf(node.getValue()));

            } else if (tag == Tag.BOOL) {
                return new JsonPrimitive(Boolean.valueOf(node.getValue()));

            } else if (tag == Tag.NULL) {
                return JsonNull.INSTANCE;

            } else {
                // treat anything else as a string
                return new JsonPrimitive(node.getValue());
            }

        } catch (NumberFormatException ex) {
            // just treat it as a string
            return new JsonPrimitive(node.getValue());
        }
    }
}
