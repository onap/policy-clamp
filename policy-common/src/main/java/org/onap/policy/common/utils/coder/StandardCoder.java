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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * JSON encoder and decoder using the "standard" mechanism, which is currently jackson.
 */
public class StandardCoder implements Coder {

    private static final ObjectMapper MAPPER = createMapper();
    private static final ObjectMapper MAPPER_PRETTY = createMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Configure to handle empty beans (like test classes with no getters/setters)
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // Configure to ignore unknown properties (similar to Gson behavior)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Configure to handle null values more gracefully
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        // Configure to handle circular references - disable self-reference detection entirely
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        // Don't write self references as null, just ignore them
        mapper.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, false);
        
        // Register modules for Java 8 time support (JSR310)
        mapper.findAndRegisterModules();

        return mapper;
    }

    protected final ObjectMapper objectMapper;
    protected final ObjectMapper mapperPretty;

    public StandardCoder() {
        this(MAPPER, MAPPER_PRETTY);
    }

    protected StandardCoder(ObjectMapper mapper, ObjectMapper mapperPretty) {
        this.objectMapper = mapper;
        this.mapperPretty = mapperPretty;
    }

    @Override
    public <S, T> T convert(S source, Class<T> clazz) throws CoderException {
        if (source == null) {
            return null;
        }
        if (clazz.isInstance(source)) {
            return clazz.cast(source);
        }
        if (clazz == String.class) {
            return clazz.cast(encode(source));
        }
        if (source instanceof String) {
            return decode((String) source, clazz);
        }
        try {
            var node = objectMapper.valueToTree(source);
            return fromJson(node, clazz);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public String encode(Object object) throws CoderException {
        return encode(object, false);
    }

    @Override
    public String encode(Object object, boolean pretty) throws CoderException {
        try {
            return pretty ? toPrettyJson(object) : toJson(object);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public void encode(Writer target, Object object) throws CoderException {
        try {
            toJson(target, object);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public void encode(OutputStream target, Object object) throws CoderException {
        var writer = makeWriter(target);
        try {
            toJson(writer, object);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public void encode(File target, Object object) throws CoderException {
        try (var writer = makeWriter(target)) {
            toJson(writer, object);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public <T> T decode(String json, Class<T> clazz) throws CoderException {
        try {
            return fromJson(json, clazz);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public <T> T decode(Reader source, Class<T> clazz) throws CoderException {
        try {
            return fromJson(source, clazz);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public <T> T decode(InputStream source, Class<T> clazz) throws CoderException {
        try (var reader = makeReader(source)) {
            return fromJson(reader, clazz);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public <T> T decode(File source, Class<T> clazz) throws CoderException {
        try (var reader = makeReader(source)) {
            return fromJson(reader, clazz);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public StandardCoderObject toStandard(Object object) throws CoderException {
        if (object instanceof Class) {
            throw new CoderException("Cannot serialize Class objects");
        }
        try {
            return new StandardCoderObject(objectMapper.valueToTree(object));
        } catch (IllegalArgumentException e) {
            throw new CoderException(e);
        }
    }

    @Override
    public <T> T fromStandard(StandardCoderObject sco, Class<T> clazz) throws CoderException {
        if (sco == null || clazz == null) {
            throw new CoderException("null argument");
        }
        try {
            return objectMapper.treeToValue(sco.getData(), clazz);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    /**
     * Makes a writer for the given file.
     *
     * @param file file of interest
     * @return a writer for the file
     * @throws FileNotFoundException if the file cannot be created
     */
    protected Writer makeWriter(File file) throws FileNotFoundException {
        return new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
    }

    /**
     * Makes a writer for the given stream.
     *
     * @param target stream of interest
     * @return a writer for the stream
     */
    protected Writer makeWriter(OutputStream target) {
        return new OutputStreamWriter(target, StandardCharsets.UTF_8);
    }

    /**
     * Makes a reader for the given file.
     *
     * @param file file of interest
     * @return a reader for the file
     * @throws FileNotFoundException if the file does not exist
     */
    protected Reader makeReader(File file) throws FileNotFoundException {
        return new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
    }

    /**
     * Makes a reader for the given stream.
     *
     * @param source stream of interest
     * @return a reader for the stream
     */
    protected Reader makeReader(InputStream source) {
        return new InputStreamReader(source, StandardCharsets.UTF_8);
    }

    /**
     * Encodes an object into json, without catching exceptions.
     *
     * @param object object to be encoded
     * @return a json string representing the object
     */
    protected String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Encodes an object into json, without catching exceptions.
     *
     * @param target target to which to write the encoded json
     * @param object object to be encoded
     */
    protected void toJson(Writer target, Object object) throws IOException {
        objectMapper.writeValue(target, object);
    }

    /**
     * Decodes a json element into an object, without catching exceptions.
     *
     * @param node json element to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json element
     */
    protected <T> T fromJson(JsonNode node, Class<T> clazz) throws CoderException {
        try {
            return objectMapper.treeToValue(node, clazz);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    /**
     * Decodes a json string into an object, without catching exceptions.
     *
     * @param json json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     */
    protected <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    /**
     * Decodes a json string into an object, without catching exceptions.
     *
     * @param source source from which to read the json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     */
    protected <T> T fromJson(Reader source, Class<T> clazz) throws IOException {
        return objectMapper.readValue(source, clazz);
    }

    protected String toPrettyJson(Object object) throws JsonProcessingException {
        return mapperPretty.writeValueAsString(object);
    }

}
