/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 */

package org.onap.clamp.configuration;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatName;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.IOHelper;
import org.onap.clamp.clds.util.JsonUtils;

public class ClampGsonDataFormat extends ServiceSupport implements DataFormat, DataFormatName {
    private Gson gson;
    private Class<?> unmarshalType;
    private Type unmarshalGenericType;
    private boolean contentTypeHeader = true;

    public ClampGsonDataFormat() {
        this(Object.class);
    }

    /**
     * Use the default Gson {@link Gson} and with a custom unmarshal type.
     *
     * @param unmarshalType the custom unmarshal type
     */
    public ClampGsonDataFormat(Class<?> unmarshalType) {
        this(null, unmarshalType);
    }

    /**
     * Use a custom Gson mapper and and unmarshal type.
     *
     * @param gson          the custom mapper
     * @param unmarshalType the custom unmarshal type
     */
    public ClampGsonDataFormat(Gson gson, Class<?> unmarshalType) {
        this.gson = gson;
        this.unmarshalType = unmarshalType;
    }

    /**
     * Use the default Gson {@link Gson} and with a custom unmarshal generic type.
     *
     * @param unmarshalGenericType the custom unmarshal generic type
     */
    public ClampGsonDataFormat(Type unmarshalGenericType) {
        this(null, unmarshalGenericType);
    }

    /**
     * Use a custom Gson mapper and and unmarshal token type.
     *
     * @param gson                 the custom mapper
     * @param unmarshalGenericType the custom unmarshal generic type
     */
    public ClampGsonDataFormat(Gson gson, Type unmarshalGenericType) {
        this.gson = gson;
        this.unmarshalGenericType = unmarshalGenericType;
    }

    @Override
    public String getDataFormatName() {
        return "clamp-gson";
    }

    @Override
    public void marshal(final Exchange exchange, final Object graph, final OutputStream stream) throws Exception {
        try (final OutputStreamWriter osw = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                final BufferedWriter writer = IOHelper.buffered(osw)) {
            gson.toJson(graph, writer);
        }

        if (contentTypeHeader) {
            if (exchange.hasOut()) {
                exchange.getOut().setHeader(Exchange.CONTENT_TYPE, "application/json");
            } else {
                exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
            }
        }
    }

    @Override
    public Object unmarshal(final Exchange exchange, final InputStream stream) throws Exception {
        try (final InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
                final BufferedReader reader = IOHelper.buffered(isr)) {
            if (unmarshalGenericType == null) {
                return gson.fromJson(reader, unmarshalType);
            } else {
                return gson.fromJson(reader, unmarshalGenericType);
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (gson == null) {
            gson = JsonUtils.GSON_JPA_MODEL;
        }
    }

    @Override
    protected void doStop() throws Exception {
        // noop
    }

    // Properties
    // -------------------------------------------------------------------------

    public Class<?> getUnmarshalType() {
        return this.unmarshalType;
    }

    public void setUnmarshalType(Class<?> unmarshalType) {
        this.unmarshalType = unmarshalType;
    }

    public Type getUnmarshalGenericType() {
        return this.unmarshalGenericType;
    }

    public void setUnmarshalGenericType(Type unmarshalGenericType) {
        this.unmarshalGenericType = unmarshalGenericType;
    }

    public boolean isContentTypeHeader() {
        return contentTypeHeader;
    }

    /**
     * If enabled then Gson will set the Content-Type header to
     * <tt>application/json</tt> when marshalling.
     */
    public void setContentTypeHeader(boolean contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }

    public Gson getGson() {
        return this.gson;
    }
}
