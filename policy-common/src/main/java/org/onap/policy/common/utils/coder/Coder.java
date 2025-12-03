/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * JSON encoder and decoder.
 */
public interface Coder {

    /**
     * Converts an object/POJO to an object of the given type.
     *
     * @param <T> desired type
     * @param source source object
     * @param clazz class of the desired object type
     * @return the converted object
     * @throws CoderException if an error occurs
     */
    default <S, T> T convert(S source, Class<T> clazz) throws CoderException {
        if (source == null) {
            return null;

        } else if (clazz == source.getClass()) {
            // same class - just cast it
            return clazz.cast(source);

        } else if (clazz == String.class) {
            // target is a string - just encode the source
            return (clazz.cast(encode(source)));

        } else if (source.getClass() == String.class) {
            // source is a string - just decode it
            return decode(source.toString(), clazz);

        } else {
            // do it the long way: encode to a string and then decode the string
            return decode(encode(source), clazz);
        }
    }

    /**
     * Encodes an object into json.
     *
     * @param object object to be encoded
     * @return a json string representing the object
     * @throws CoderException if an error occurs
     */
    String encode(Object object) throws CoderException;

    /**
     * Encodes an object into json, optionally making it "pretty".
     *
     * @param object object to be encoded
     * @param pretty {@code true} if it should be encoded as "pretty" json, {@code false}
     *        otherwise
     * @return a json string representing the object
     * @throws CoderException if an error occurs
     */
    String encode(Object object, boolean pretty) throws CoderException;

    /**
     * Encodes an object into json, writing to the given target.
     *
     * @param target target to which to write the encoded json
     * @param object object to be encoded
     * @throws CoderException if an error occurs
     */
    void encode(Writer target, Object object) throws CoderException;

    /**
     * Encodes an object into json, writing to the given target.
     *
     * @param target target to which to write the encoded json
     * @param object object to be encoded
     * @throws CoderException if an error occurs
     */
    void encode(OutputStream target, Object object) throws CoderException;

    /**
     * Encodes an object into json, writing to the given target.
     *
     * @param target target to which to write the encoded json
     * @param object object to be encoded
     * @throws CoderException if an error occurs
     */
    void encode(File target, Object object) throws CoderException;

    /**
     * Decodes json into an object.
     *
     * @param json json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     * @throws CoderException if an error occurs
     */
    <T> T decode(String json, Class<T> clazz) throws CoderException;

    /**
     * Decodes json into an object, reading it from the given source.
     *
     * @param source source from which to read the json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     * @throws CoderException if an error occurs
     */
    <T> T decode(Reader source, Class<T> clazz) throws CoderException;

    /**
     * Decodes json into an object, reading it from the given source.
     *
     * @param source source from which to read the json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     * @throws CoderException if an error occurs
     */
    <T> T decode(InputStream source, Class<T> clazz) throws CoderException;

    /**
     * Decodes json into an object, reading it from the given source.
     *
     * @param source source from which to read the json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     * @throws CoderException if an error occurs
     */
    <T> T decode(File source, Class<T> clazz) throws CoderException;

    /**
     * Converts an object/POJO to a standard object.
     *
     * @param object object to be converted
     * @return a new standard object representing the original object
     * @throws CoderException if an error occurs
     */
    StandardCoderObject toStandard(Object object) throws CoderException;

    /**
     * Converts a standard object to an object/POJO.
     *
     * @param sco the standard object to be converted
     * @return a new object represented by the standard object
     * @throws CoderException if an error occurs
     */
    <T> T fromStandard(StandardCoderObject sco, Class<T> clazz) throws CoderException;
}
