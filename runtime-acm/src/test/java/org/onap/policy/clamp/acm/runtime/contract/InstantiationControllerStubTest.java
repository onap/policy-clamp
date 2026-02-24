/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023, 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "stub" })
class InstantiationControllerStubTest extends CommonRestController {

    private static final String COMMISSIONING_ENDPOINT = "compositions";
    private static final String INSTANTIATION_ENDPOINT = "instances";
    private static final String COMPOSITION_ID = "1aeed185-a98b-45b6-af22-8d5d20485ea3";
    private static final String INSTANCE_ID = "709c62b3-8918-41b9-a747-d21eb79c6c23";
    private static final String ROLLBACK = "rollback";

    @LocalServerPort
    private int randomServerPort;

    @BeforeEach
    void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    @Test
    void testQuery() {
        RestClient restClient = super.sendRequest(COMMISSIONING_ENDPOINT
                + "/" + COMPOSITION_ID
                + "/" + INSTANTIATION_ENDPOINT);
        var response = restClient.get().retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(response.getStatusCode().value());
    }

    @Test
    void testGet() {
        RestClient restClient = super.sendRequest(COMMISSIONING_ENDPOINT
                + "/" + COMPOSITION_ID
                + "/" + INSTANTIATION_ENDPOINT
                + "/" + INSTANCE_ID);
        var response = restClient.get().retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(response.getStatusCode().value());
    }

    @Test
    void testPut() {
        RestClient restClient = super.sendRequest(COMMISSIONING_ENDPOINT
                + "/" + COMPOSITION_ID
                + "/" + INSTANTIATION_ENDPOINT
                + "/" + INSTANCE_ID);
        var put = restClient.put().body(new AcInstanceStateUpdate()).retrieve().toBodilessEntity();
        assertThat(HttpStatus.ACCEPTED.value()).isEqualTo(put.getStatusCode().value());
    }

    @Test
    void testPost() {
        var ac = new AutomationComposition();
        ac.setCompositionId(UUID.randomUUID());
        RestClient restClient = super.sendRequest(COMMISSIONING_ENDPOINT
                + "/" + COMPOSITION_ID
                + "/" + INSTANTIATION_ENDPOINT);
        var respPost = restClient.post().body(ac).retrieve().toBodilessEntity();
        assertThat(respPost.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void testDelete() {
        RestClient restClient = super.sendRequest(COMMISSIONING_ENDPOINT
                + "/" + COMPOSITION_ID
                + "/" + INSTANTIATION_ENDPOINT
                + "/" + INSTANCE_ID);
        var delete = restClient.delete().retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(delete.getStatusCode().value());
    }

    @Test
    void testRollback() {
        RestClient restClient = super.sendRequest(COMMISSIONING_ENDPOINT
            + "/" + COMPOSITION_ID
            + "/" + INSTANTIATION_ENDPOINT
            + "/" + INSTANCE_ID
            + "/" + ROLLBACK);
        var respPost = restClient.post().body(new AcInstanceStateUpdate()).retrieve().toBodilessEntity();
        assertThat(HttpStatus.ACCEPTED.value()).isEqualTo(respPost.getStatusCode().value());
    }

    @Test
    void testGetInstances() {
        RestClient restClient = super.sendRequest(INSTANTIATION_ENDPOINT);
        var response = restClient.get().retrieve().toBodilessEntity();
        assertThat(HttpStatus.OK.value()).isEqualTo(response.getStatusCode().value());
    }
}
