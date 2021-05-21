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

package org.onap.policy.clamp.controlloop.runtime.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * Class to perform unit test of {@link RestController}}.
 *
 */
public class RestControllerTest {

    @Test
    public void testProduces() {
        Produces annotation = RestController.class.getAnnotation(Produces.class);
        assertNotNull(annotation);
        assertThat(annotation.value()).contains(MediaType.APPLICATION_JSON)
                        .contains(RestController.APPLICATION_YAML);
    }

    @Test
    public void testAddVersionControlHeaders() {
        RestController ctlr = new RestController();
        Response resp = ctlr.addVersionControlHeaders(Response.status(Response.Status.OK)).build();
        assertEquals("0", resp.getHeaderString(RestController.VERSION_MINOR_NAME));
        assertEquals("0", resp.getHeaderString(RestController.VERSION_PATCH_NAME));
        assertEquals("1.0.0", resp.getHeaderString(RestController.VERSION_LATEST_NAME));
    }

    @Test
    public void testAddLoggingHeaders_Null() {
        RestController ctlr = new RestController();
        Response resp = ctlr.addLoggingHeaders(Response.status(Response.Status.OK), null).build();
        assertNotNull(resp.getHeaderString(RestController.REQUEST_ID_NAME));
    }

    @Test
    public void testAddLoggingHeaders_NonNull() {
        UUID uuid = UUID.randomUUID();
        RestController ctlr = new RestController();
        Response resp = ctlr.addLoggingHeaders(Response.status(Response.Status.OK), uuid).build();
        assertEquals(uuid.toString(), resp.getHeaderString(RestController.REQUEST_ID_NAME));
    }

}
