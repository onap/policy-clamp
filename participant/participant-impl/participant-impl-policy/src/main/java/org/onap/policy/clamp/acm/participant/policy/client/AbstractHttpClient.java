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

package org.onap.policy.clamp.acm.participant.policy.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpClient.class);
    private final HttpClient httpclient;

    /**
     * Constructor.
     *
     * @param policyParticipantParameters the parameters for the policy participant
     * @throws AutomationCompositionRuntimeException on client start errors
     */
    protected AbstractHttpClient(BusTopicParams policyParticipantParameters) {
        try {
            httpclient = HttpClientFactoryInstance.getClientFactory().build(policyParticipantParameters);
        } catch (final Exception e) {
            throw new AutomationCompositionRuntimeException(Status.INTERNAL_SERVER_ERROR, " Client failed to start", e);
        }
    }

    protected Response executePost(String path, final Entity<?> entity) {
        var response = httpclient.post(path, entity, Map.of(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
            HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON));
        if (response.getStatus() / 100 != 2) {
            LOGGER.error("Invocation of path {} failed for entity {}. Response status: {}, Response status info: {}",
                path, entity, response.getStatus(), response.getStatusInfo());
        }
        return response;
    }

    protected Response executeDelete(String path) {
        return httpclient.delete(path, Collections.emptyMap());
    }

    @Override
    public void close() throws IOException {
        httpclient.shutdown();
    }
}
