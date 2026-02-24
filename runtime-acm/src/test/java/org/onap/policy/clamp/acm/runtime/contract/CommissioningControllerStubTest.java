/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023, 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestClient;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "stub" })
class CommissioningControllerStubTest extends CommonRestClient {

    private static final String COMMISSIONING_ENDPOINT = "compositions";
    private static final String COMPOSITION_ID = "1aeed185-a98b-45b6-af22-8d5d20485ea3";
    private static final ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    @LocalServerPort
    private int randomServerPort;

    @BeforeEach
    void setUpPort() {
        super.initializeRestClient(randomServerPort);
    }

    @Test
    void testQuery() {
        var respPost = super.sendGet(COMMISSIONING_ENDPOINT).retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testGet() {
        var respPost = super.sendGet(COMMISSIONING_ENDPOINT + "/" + COMPOSITION_ID).retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testPut() {
        var respPost = super.sendPut(COMMISSIONING_ENDPOINT + "/" + COMPOSITION_ID)
                .body(serviceTemplate).retrieve().toBodilessEntity();
        assertThat(HttpStatus.ACCEPTED.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testPost() {
        var respPost = super.sendPost(COMMISSIONING_ENDPOINT).body(serviceTemplate).retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testDelete() {
        var respPost = super.sendDelete(COMMISSIONING_ENDPOINT + "/" + COMPOSITION_ID).retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(respPost.getStatusCode().value());
    }
}
