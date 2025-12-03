/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.common.spring.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

class YamlHttpMessageConverterTest {

    private YamlHttpMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new YamlHttpMessageConverter();
    }

    @Test
    void testCanReadAndWriteYamlMediaType() {
        MediaType mediaType = new MediaType("application", "yaml");
        assertTrue(converter.canRead(Object.class, mediaType));
        assertTrue(converter.canWrite(Object.class, mediaType));
    }

    @Test
    void testReadInternal() throws IOException {
        // YAML content representing a simple key-value pair as a map
        String yamlContent = "key: value";

        // Mocking HttpHeaders
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getContentType()).thenReturn(MediaType.APPLICATION_JSON);  // Return JSON media type

        // Mocking HttpInputMessage
        HttpInputMessage inputMessage = mock(HttpInputMessage.class);
        when(inputMessage.getBody()).thenReturn(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
        when(inputMessage.getHeaders()).thenReturn(headers);

        // Now we call the converter's read method and assert the results
        Map<String, String> result = (Map<String, String>) converter.read(Map.class, null, inputMessage);

        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }


    @Test
    void testReadInternalWithException() throws IOException {
        HttpInputMessage inputMessage = mock(HttpInputMessage.class);
        when(inputMessage.getBody()).thenThrow(new IOException("IO Exception during reading"));

        assertThrows(HttpMessageNotReadableException.class, () -> converter.read(Map.class, null, inputMessage));
    }

    @Test
    void testWriteInternal() throws IOException {
        // Mocking HttpHeaders
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getContentType()).thenReturn(MediaType.APPLICATION_JSON);  // Return JSON media type
        when(headers.getAcceptCharset()).thenReturn(null);  // Return null to use default charset

        // Mocking HttpOutputMessage
        HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(outputMessage.getBody()).thenReturn(outputStream);
        when(outputMessage.getHeaders()).thenReturn(headers);

        // A simple map to be serialized into YAML
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        // Calling the converter's write method
        converter.write(map, null, outputMessage);

        // Verifying the output
        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("key: value"));
    }


    @Test
    void testWriteInternalWithException() throws IOException {
        // Mocking HttpHeaders
        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getContentType()).thenReturn(MediaType.APPLICATION_JSON);  // Return YAML media type

        // Mocking HttpOutputMessage to throw an IOException when getBody() is called
        HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
        when(outputMessage.getBody()).thenThrow(new IOException("IO Exception during writing"));
        when(outputMessage.getHeaders()).thenReturn(headers);

        // A simple map to be serialized into YAML
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        // We expect the write method to throw a HttpMessageNotWritableException
        assertThrows(HttpMessageNotWritableException.class, () -> converter.write(map, null, outputMessage));
    }

}
