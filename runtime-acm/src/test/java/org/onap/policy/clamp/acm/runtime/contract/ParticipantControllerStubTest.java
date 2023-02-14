/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.contract;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "stub" })
public class ParticipantControllerStubTest extends CommonRestController {
    private static final String PARTICIPANT_ENDPOINT = "participants";
    private static final String PARTICIPANT_ID = "101c62b3-8918-41b9-a747-d21eb79c6c03";

    @LocalServerPort
    private int randomServerPort;

    @BeforeEach
    public void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    @Test
    void testGet() {
        var invocationBuilder = super.sendRequest(PARTICIPANT_ENDPOINT + "/" + PARTICIPANT_ID);
        var respPost = invocationBuilder.get();
        assertThat(Response.Status.OK.getStatusCode()).isEqualTo(respPost.getStatus());
    }

    @Test
    void testQuery() {
        var invocationBuilder = super.sendRequest(PARTICIPANT_ENDPOINT);
        var respPost = invocationBuilder.get();
        assertThat(Response.Status.OK.getStatusCode()).isEqualTo(respPost.getStatus());
    }

    @Test
    void testOrderReport() {
        var invocationBuilder = super.sendRequest(PARTICIPANT_ENDPOINT + "/" + PARTICIPANT_ID);

        var respPost = invocationBuilder.header("Content-Length", 0).put(Entity.entity(""
            + "", MediaType.APPLICATION_JSON));
        assertThat(Response.Status.ACCEPTED.getStatusCode()).isEqualTo(respPost.getStatus());
    }

    @Test
    void testOrderAllReport() {
        var invocationBuilder = super.sendRequest(PARTICIPANT_ENDPOINT);

        var respPost = invocationBuilder.header("Content-Length", 0).put(Entity.entity(""
            + "", MediaType.APPLICATION_JSON));
        assertThat(Response.Status.ACCEPTED.getStatusCode()).isEqualTo(respPost.getStatus());
    }
}
