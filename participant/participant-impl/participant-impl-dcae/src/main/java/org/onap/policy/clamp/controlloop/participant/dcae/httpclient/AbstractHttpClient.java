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

package org.onap.policy.clamp.controlloop.participant.dcae.httpclient;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.participant.dcae.model.Loop;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpClient implements Closeable {

    private static final String MSG_REQUEST_FAILED = "request to {} failed";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpClient.class);
    private final HttpClient httpclient;
    public static final Coder CODER = new StandardCoder();

    /**
     * Constructor.
     *
     * @param restClientParameters the REST client parameters
     * @throws ControlLoopRuntimeException on errors
     */
    protected AbstractHttpClient(BusTopicParams restClientParameters) {
        try {
            httpclient = HttpClientFactoryInstance.getClientFactory().build(restClientParameters);
        } catch (final Exception e) {
            throw new ControlLoopRuntimeException(Status.INTERNAL_SERVER_ERROR,
                    restClientParameters.getClientName() + " Client failed to start", e);
        }
    }

    protected boolean executePut(String path, String jsonEntity, int statusCode) {
        try {
            var response = httpclient.put(path, Entity.json(jsonEntity), Collections.emptyMap());
            return response.getStatus() == statusCode;
        } catch (Exception e) {
            LOGGER.error(MSG_REQUEST_FAILED, httpclient.getName(), e);
            return false;
        }
    }

    protected boolean executePut(String path, int statusCode) {
        try {
            var response = httpclient.put(path, Entity.json(""), Collections.emptyMap());
            return response.getStatus() == statusCode;
        } catch (Exception e) {
            LOGGER.error(MSG_REQUEST_FAILED, httpclient.getName(), e);
            return false;
        }
    }

    protected Loop executePost(String path, int statusCode) {
        try {
            var response = httpclient.post(path, Entity.json(""), Collections.emptyMap());
            if (response.getStatus() != statusCode) {
                return null;
            }
            return response.readEntity(Loop.class);
        } catch (Exception e) {
            LOGGER.error(MSG_REQUEST_FAILED, httpclient.getName(), e);
            return null;
        }
    }

    protected Loop executeGet(String path, int statusCode) {
        try {
            var response = httpclient.get(path);

            if (response.getStatus() != statusCode) {
                return null;
            }
            return response.readEntity(Loop.class);
        } catch (Exception e) {
            LOGGER.error(MSG_REQUEST_FAILED, httpclient.getName(), e);
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        httpclient.shutdown();
    }
}
