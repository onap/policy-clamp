/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.policy.operational.OperationalPolicy;

class ClampGsonDataFormatTest {

    private static String FORMAT = "clamp-gson";
    private static String PATH_JSON = "src/test/resources/tosca/service-details.json";


    @Test
    void testFormat() throws IOException {
        var clampGsonDataFormat = new ClampGsonDataFormat();

        assertEquals(FORMAT, clampGsonDataFormat.getDataFormatName());
        assertTrue(clampGsonDataFormat.isContentTypeHeader());
        assertNull(clampGsonDataFormat.getGson());

        clampGsonDataFormat.close();
    }

    @Test
    void testMarshal() throws IOException {
        var clampGsonDataFormatFull = new ClampGsonDataFormat(new Gson(), OperationalPolicy.class);

        var exchange = mock(Exchange.class);
        when(exchange.hasOut()).thenReturn(true);
        when(exchange.getOut()).thenReturn(mock(Message.class));
        assertDoesNotThrow(() -> clampGsonDataFormatFull
                .marshal(exchange, new OperationalPolicy(), mock(OutputStream.class)));

        when(exchange.hasOut()).thenReturn(false);
        when(exchange.getIn()).thenReturn(mock(Message.class));
        assertDoesNotThrow(() -> clampGsonDataFormatFull
                .marshal(exchange, new OperationalPolicy(), mock(OutputStream.class)));

        clampGsonDataFormatFull.setContentTypeHeader(false);
        assertDoesNotThrow(() -> clampGsonDataFormatFull
                .marshal(exchange, new OperationalPolicy(), mock(OutputStream.class)));

        clampGsonDataFormatFull.close();
    }

    @Test
    void testUnmarshal() throws IOException {
        var clampGsonDataFormatFull = new ClampGsonDataFormat(new Gson(), OperationalPolicy.class);

        var exchange = mock(Exchange.class);
        var jsonExample = new File(PATH_JSON);
        var stubInputStream = new FileInputStream(jsonExample);
        assertDoesNotThrow(() -> clampGsonDataFormatFull.unmarshal(exchange, stubInputStream));

        clampGsonDataFormatFull.setUnmarshalType(String.class);
        var stubInputStream2 = new FileInputStream(jsonExample);
        assertDoesNotThrow(() -> clampGsonDataFormatFull.unmarshal(exchange, stubInputStream2));

        clampGsonDataFormatFull.close();
    }

}
