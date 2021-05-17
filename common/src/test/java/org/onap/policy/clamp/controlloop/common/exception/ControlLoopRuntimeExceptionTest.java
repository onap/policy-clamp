/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.Test;
import org.onap.policy.models.errors.concepts.ErrorResponse;

public class ControlLoopRuntimeExceptionTest {

    private static final String STRING_TEXT = "String";
    private static final String MESSAGE = "Message";

    @Test
    public void testControlLoopRuntimeException() {
        assertNotNull(new ControlLoopRuntimeException(Response.Status.OK, MESSAGE));
        assertNotNull(new ControlLoopRuntimeException(Response.Status.OK, MESSAGE, STRING_TEXT));
        assertNotNull(new ControlLoopRuntimeException(Response.Status.OK, MESSAGE, new IOException()));
        assertNotNull(new ControlLoopRuntimeException(Response.Status.OK, MESSAGE, new IOException(), STRING_TEXT));

        String rkey = "A String";
        ControlLoopRuntimeException re = new ControlLoopRuntimeException(Response.Status.OK, "Runtime Message",
                new IOException("IO runtime exception message"), rkey);
        ErrorResponse errorResponse = re.getErrorResponse();
        assertEquals("Runtime Message\nIO runtime exception message",
                String.join("\n", errorResponse.getErrorDetails()));
        assertEquals(rkey, re.getObject());

        ControlLoopException ae =
                new ControlLoopException(Response.Status.OK, MESSAGE, new IOException("IO exception message"), rkey);
        ControlLoopRuntimeException clre = new ControlLoopRuntimeException(ae);
        assertEquals(ae.getErrorResponse().getResponseCode(), clre.getErrorResponse().getResponseCode());
        assertEquals(ae.getMessage(), clre.getMessage());

        assertThat(ae.toString()).contains("A String");
        assertThat(re.toString()).contains("A String");
    }
}
