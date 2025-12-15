/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.common.utils.coder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
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
 */
public class YamlJsonTranslator {

    private final ObjectMapper mapper;

    /**
     * Constructs the object.
     */
    public YamlJsonTranslator() {
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules(); // for Instant
        // Configure to handle empty beans
        this.mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // Configure to ignore unknown properties (similar to Gson behavior)
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Configure to handle null values more gracefully
        this.mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        // Configure to handle circular references
        this.mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
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
        try {
            var json = mapper.valueToTree(object);
            var yamlNode = makeYaml(json);

            var options = new DumperOptions();
            var serializer = new Serializer(new Emitter(target, options), new Resolver(), options, null);
            serializer.open();
            serializer.serialize(yamlNode);
            serializer.close();
        } catch (IOException e) {
            throw new YAMLException(e);
        }
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
        var json = makeJson(node);
        try {
            return mapper.treeToValue(json, clazz);
        } catch (Exception ex) {
            throw new YAMLException(ex);
        }
    }

    /**
     * Converts an arbitrary json element into a corresponding Yaml node.
     *
     * @param node json element to be converted
     * @return a yaml node corresponding to the element
     */
    protected Node makeYaml(JsonNode node) {
        if (node.isArray()) {
            return makeYamlSequence((ArrayNode) node);
        }
        if (node.isObject()) {
            return makeYamlMap((ObjectNode) node);
        }
        if (node.isValueNode()) {
            return makeYamlPrim((ValueNode) node);
        }

        return new ScalarNode(Tag.NULL, "", null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    /**
     * Converts an arbitrary json array into a corresponding Yaml sequence.
     *
     * @param array json element to be converted
     * @return a yaml node corresponding to the element
     */
    protected Node makeYamlSequence(ArrayNode array) {
        var nodes = new ArrayList<Node>();
        array.forEach(item -> nodes.add(makeYaml(item)));
        return new SequenceNode(Tag.SEQ, nodes, DumperOptions.FlowStyle.AUTO);
    }

    /**
     * Converts an arbitrary json object into a corresponding Yaml map.
     *
     * @param obj json element to be converted
     * @return a yaml node corresponding to the element
     */
    protected Node makeYamlMap(ObjectNode obj) {
        var tuples = new ArrayList<NodeTuple>();
        Iterator<Map.Entry<String, JsonNode>> it = obj.fields();

        while (it.hasNext()) {
            var entry = it.next();
            var key = new ScalarNode(Tag.STR, entry.getKey(), null, null, DumperOptions.ScalarStyle.PLAIN);
            var value = makeYaml(entry.getValue());
            tuples.add(new NodeTuple(key, value));
        }

        return new MappingNode(Tag.MAP, tuples, DumperOptions.FlowStyle.AUTO);
    }

    /**
     * Converts an arbitrary json primitive into a corresponding Yaml scalar.
     *
     * @param node json element to be converted
     * @return a yaml node corresponding to the element
     */
    protected Node makeYamlPrim(ValueNode node) {
        Tag tag;
        if (node.isNumber()) {
            tag = (node.isFloatingPointNumber()) ? Tag.FLOAT : Tag.INT;
        } else if (node.isBoolean()) {
            tag = Tag.BOOL;
        } else {
            tag = Tag.STR;
        }
        return new ScalarNode(tag, node.asText(), null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    /**
     * Converts an arbitrary Yaml node into a corresponding json element.
     *
     * @param node node to be converted
     * @return a json element corresponding to the node
     */
    protected JsonNode makeJson(Node node) {
        if (node == null) {
            return NullNode.getInstance();
        }
        return switch (node) {
            case MappingNode mapping -> makeJsonObject(mapping);
            case SequenceNode seq -> makeJsonArray(seq);
            case ScalarNode scalar -> makeJsonPrim(scalar);
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };
    }

    /**
     * Converts a Yaml sequence into a corresponding json array.
     *
     * @param seq node to be converted
     * @return a json element corresponding to the node
     */
    protected JsonNode makeJsonArray(SequenceNode seq) {
        var array = mapper.createArrayNode();
        for (var n : seq.getValue()) {
            array.add(makeJson(n));
        }
        return array;
    }

    /**
     * Converts a Yaml map into a corresponding gson object.
     *
     * @param map node to be converted
     * @return a json element corresponding to the node
     */
    protected JsonNode makeJsonObject(MappingNode map) {
        var obj = mapper.createObjectNode();
        for (var tuple : map.getValue()) {
            var key = ((ScalarNode) tuple.getKeyNode()).getValue();
            obj.set(key, makeJson(tuple.getValueNode()));
        }
        return obj;
    }

    /**
     * Converts a Yaml scalar into a corresponding json primitive.
     *
     * @param node node to be converted
     * @return a json element corresponding to the node
     */
    protected JsonNode makeJsonPrim(ScalarNode node) {
        var tag = node.getTag();
        var val = node.getValue();
        
        // Handle null values explicitly
        if (tag == Tag.NULL || val == null || "null".equals(val) || val.isEmpty()) {
            return NullNode.getInstance();
        }
        if (tag == Tag.INT) {
            // Try to parse as int first, then long if it's too big
            try {
                return mapper.getNodeFactory().numberNode(Integer.parseInt(val));
            } catch (NumberFormatException e) {
                return mapper.getNodeFactory().numberNode(Long.parseLong(val));
            }
        }
        if (tag == Tag.FLOAT) {
            // Try to parse as float first, then double if needed
            try {
                float floatVal = Float.parseFloat(val);
                if (!Float.isInfinite(floatVal)) {
                    return mapper.getNodeFactory().numberNode(floatVal);
                }
            } catch (NumberFormatException ignored) {
                // Fall through to double
            }
            return mapper.getNodeFactory().numberNode(Double.parseDouble(val));
        }
        if (tag == Tag.BOOL) {
            return mapper.getNodeFactory().booleanNode(Boolean.parseBoolean(val));
        }        
        return mapper.getNodeFactory().textNode(val);
    }

}