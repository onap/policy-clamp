/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.common.acm.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.errors.concepts.ErrorResponse;

class ExceptionsTest {

    private static final String STRING_TEXT = "String";
    private static final String MESSAGE = "Message";

    @Test
    void testExceptions() {
        assertNotNull(new AutomationCompositionException(Response.Status.OK, MESSAGE));
        assertNotNull(new AutomationCompositionException(Response.Status.OK, MESSAGE, STRING_TEXT));
        assertNotNull(new AutomationCompositionException(Response.Status.OK, MESSAGE, new IOException()));
        assertNotNull(new AutomationCompositionException(Response.Status.OK, MESSAGE, new IOException(), STRING_TEXT));

        String key = "A String";
        AutomationCompositionException ae = new AutomationCompositionException(Response.Status.OK, MESSAGE,
            new IOException("IO exception message"), key);
        ErrorResponse errorResponse = ae.getErrorResponse();
        assertEquals("Message\nIO exception message", String.join("\n", errorResponse.getErrorDetails()));
        assertEquals(key, ae.getObject());

        assertNotNull(new AutomationCompositionRuntimeException(Response.Status.OK, MESSAGE));
        assertNotNull(new AutomationCompositionRuntimeException(Response.Status.OK, MESSAGE, STRING_TEXT));
        assertNotNull(new AutomationCompositionRuntimeException(Response.Status.OK, MESSAGE, new IOException()));
        assertNotNull(
            new AutomationCompositionRuntimeException(Response.Status.OK, MESSAGE, new IOException(), STRING_TEXT));

        String rkey = "A String";
        AutomationCompositionRuntimeException re = new AutomationCompositionRuntimeException(Response.Status.OK,
            "Runtime Message", new IOException("IO runtime exception message"), rkey);
        errorResponse = re.getErrorResponse();
        assertEquals("Runtime Message\nIO runtime exception message",
            String.join("\n", errorResponse.getErrorDetails()));
        assertEquals(key, re.getObject());

        AutomationCompositionRuntimeException acre = new AutomationCompositionRuntimeException(ae);
        assertEquals(ae.getErrorResponse().getResponseCode(), acre.getErrorResponse().getResponseCode());
        assertEquals(ae.getMessage(), acre.getMessage());

        try {
            try {
                throw new AutomationCompositionException(Status.BAD_GATEWAY, "An Exception");
            } catch (AutomationCompositionException ace) {
                throw new AutomationCompositionRuntimeException(ace);
            }
        } catch (AutomationCompositionRuntimeException acred) {
            assertEquals(Status.BAD_GATEWAY, acred.getErrorResponse().getResponseCode());
            assertEquals("An Exception", acred.getMessage());
            assertEquals(AutomationCompositionException.class.getName(), acred.getCause().getClass().getName());
        }

        assertThat(ae.toString()).contains("A String");
        assertThat(re.toString()).contains("A String");
    }
}
