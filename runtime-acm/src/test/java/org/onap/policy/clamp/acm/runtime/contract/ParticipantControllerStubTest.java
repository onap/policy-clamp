/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023, 2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "stub" })
class ParticipantControllerStubTest extends CommonRestClient {
    private static final String PARTICIPANT_ENDPOINT = "participants";
    private static final String PARTICIPANT_ID = "101c62b3-8918-41b9-a747-d21eb79c6c03";

    @LocalServerPort
    private int randomServerPort;

    @BeforeEach
    void setUpPort() {
        super.initializeRestClient(randomServerPort);
    }

    @Test
    void testGet() {
        var respPost = super.sendGet(PARTICIPANT_ENDPOINT + "/" + PARTICIPANT_ID).retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testQuery() {
        var respPost = super.sendGet(PARTICIPANT_ENDPOINT).retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testOrderReport() {
        var respPost = super.sendPut(PARTICIPANT_ENDPOINT + "/" + PARTICIPANT_ID).body("")
                .retrieve().toBodilessEntity();
        assertThat(HttpStatus.ACCEPTED.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testOrderAllReport() {
        var respPost = super.sendPut(PARTICIPANT_ENDPOINT).body("").retrieve().toBodilessEntity();
        assertThat(HttpStatus.ACCEPTED.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testRestartParticipants() {
        var response = super.sendPut(PARTICIPANT_ENDPOINT + "/sync").body("").retrieve().toBodilessEntity();
        assertEquals(HttpStatus.ACCEPTED.value(), response.getStatusCode().value());
    }

    @Test
    void testRestartParticipantById() {
        var response = super.sendPut(PARTICIPANT_ENDPOINT + "/sync/"
            + UUID.randomUUID()).body("").retrieve().toBodilessEntity();
        assertEquals(HttpStatus.ACCEPTED.value(), response.getStatusCode().value());
    }
}
