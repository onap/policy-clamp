/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.common.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider that serializes and de-serializes JSON via gson.
 */
@Provider
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class GsonMessageBodyHandler implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    public static final Logger logger = LoggerFactory.getLogger(GsonMessageBodyHandler.class);

    /**
     * Object to be used to serialize and de-serialize.
     */
    @Getter(AccessLevel.PROTECTED)
    private final Gson gson;

    /**
     * Constructs the object, using a Gson object that translates Doubles inside of Maps
     * into Integer/Long, where possible.
     */
    public GsonMessageBodyHandler() {
        this(configBuilder(new GsonBuilder()).create());
    }

    /**
     * Constructs the object.
     *
     * @param gson the Gson object to be used to serialize and de-serialize
     */
    public GsonMessageBodyHandler(Gson gson) {
        this.gson = gson;

        logger.info("Using GSON for REST calls");
    }

    /**
     * Configures a builder with the adapters normally used by this handler (e.g., mapper
     * that converts Double to Integer).
     *
     * @param builder builder to be configured
     * @return the configured builder
     */
    public static GsonBuilder configBuilder(GsonBuilder builder) {
        return builder.disableHtmlEscaping().registerTypeAdapterFactory(new MapDoubleAdapterFactory())
                        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter())
                        .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter())
                        .registerTypeAdapter(OffsetTime.class, new OffsetTimeTypeAdapter())
                        .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                        .registerTypeAdapter(ZoneOffset.class, new ZoneOffsetTypeAdapter());
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canHandle(mediaType);
    }

    @Override
    public long getSize(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                    MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {

        try (var writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8)) {
            Type jsonType = (type.equals(genericType) ? type : genericType);
            gson.toJson(object, jsonType, writer);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canHandle(mediaType);
    }

    /**
     * Determines if this provider can handle the given media type.
     *
     * @param mediaType the media type of interest
     * @return {@code true} if this provider handles the given media type, {@code false}
     *         otherwise
     */
    private boolean canHandle(MediaType mediaType) {
        if (mediaType == null) {
            return true;
        }

        String subtype = mediaType.getSubtype();

        if ("json".equalsIgnoreCase(subtype) || "javascript".equals(subtype)) {
            return true;
        }

        return subtype.endsWith("+json") || "x-json".equals(subtype) || "x-javascript".equals(subtype);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                    MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {

        try (var streamReader = new InputStreamReader(entityStream, StandardCharsets.UTF_8)) {
            Type jsonType = (type.equals(genericType) ? type : genericType);
            return gson.fromJson(streamReader, jsonType);
        }
    }
}
