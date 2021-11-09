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

package org.onap.policy.clamp.controlloop.common.rest;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.common.startstop.CommonCommandLineArguments;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;

class CoderHttpMessageConverterTest {


    @Test
    void coderHttpMesageConverterTest() throws ControlLoopRuntimeException, IOException {
        var y = new CoderHttpMesageConverter<>("yaml");
        var j = new CoderHttpMesageConverter<>("json");

        assertTrue(y.supports(CommonCommandLineArguments.class));
        assertTrue(j.supports(CommonCommandLineArguments.class));
        var testInputStream = new ByteArrayInputStream("testdata".getBytes());
        HttpInputMessage input = Mockito.mock(HttpInputMessage.class);
        Mockito.when(input.getBody()).thenReturn(testInputStream);
        assertThrows(ControlLoopRuntimeException.class, () -> {
            y.readInternal(RequestResponseLoggingFilterTest.class, input);
        });

        var testOutputStream = new ByteArrayOutputStream();
        HttpOutputMessage output = Mockito.mock(HttpOutputMessage.class);
        Mockito.when(output.getBody()).thenReturn(testOutputStream);
        assertThrows(ControlLoopRuntimeException.class, () -> {
            j.writeInternal(String.class, output);
        });
    }
}
