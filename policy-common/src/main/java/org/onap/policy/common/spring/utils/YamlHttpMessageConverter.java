/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Bell Canada. All rights reserved.
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

package org.onap.policy.common.spring.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;

/**
 * Custom converter to marshal/unmarshall data structured with YAML media type.
 */
public class YamlHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final YamlJsonTranslator TRANSLATOR = new YamlJsonTranslator();

    public YamlHttpMessageConverter() {
        super(new MediaType("application", "yaml"));
        setDefaultCharset(DEFAULT_CHARSET);
    }

    @Override
    public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
        throws IOException {
        return readResolved(GenericTypeResolver.resolveType(type, contextClass), inputMessage);
    }

    @Override
    protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        return readResolved(clazz, inputMessage);
    }

    private Object readInternal(Type resolvedType, Reader reader) {
        Class<?> clazz = (Class<?>) resolvedType;
        return TRANSLATOR.fromYaml(reader, clazz);
    }

    @Override
    protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
        throws IOException {
        try (var writer = getWriter(outputMessage)) {
            writeInternal(object, writer);
            writer.flush();
        } catch (Exception ex) {
            throw new HttpMessageNotWritableException("Could not write YAML: " + ex.getMessage(), ex);
        }
    }

    private void writeInternal(Object object, Writer writer) {
        TRANSLATOR.toYaml(writer, object);
    }

    private Object readResolved(Type resolvedType, HttpInputMessage inputMessage) throws IOException {
        try (var reader = getReader(inputMessage)) {
            return readInternal(resolvedType, reader);
        } catch (Exception ex) {
            throw new HttpMessageNotReadableException("Could not read YAML: " + ex.getMessage(), ex, inputMessage);
        }
    }

    private static Reader getReader(HttpInputMessage inputMessage) throws IOException {
        return new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()));
    }

    private static Writer getWriter(HttpOutputMessage outputMessage) throws IOException {
        return new OutputStreamWriter(outputMessage.getBody(), getCharset(outputMessage.getHeaders()));
    }

    private static Charset getCharset(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        Charset charset = (contentType == null ? null : contentType.getCharset());
        return (charset != null ? charset : DEFAULT_CHARSET);
    }
}
