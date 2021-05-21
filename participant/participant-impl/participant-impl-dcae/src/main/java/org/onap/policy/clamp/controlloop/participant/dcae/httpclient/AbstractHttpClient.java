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
import javax.ws.rs.core.Response.Status;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.participant.dcae.model.Loop;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpClient.class);
    private final HttpClientContext localContext;
    private final CloseableHttpClient httpclient;
    private final HttpHost target;
    public static final Coder CODER = new StandardCoder();

    /**
     * Constructor.
     */
    protected AbstractHttpClient(RestServerParameters restServerParameters) {
        try {
            final String scheme = restServerParameters.isHttps() ? "https" : "http";
            target = new HttpHost(restServerParameters.getHost(), restServerParameters.getPort(), scheme);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),
                    new UsernamePasswordCredentials(restServerParameters.getUserName(),
                            restServerParameters.getPassword()));

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);
            localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            HttpClientBuilder builder = HttpClients.custom().setDefaultCredentialsProvider(credsProvider);
            if (restServerParameters.isHttps()) {
                final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(new SSLContextBuilder()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy()).setProtocol("TLSv1.2").build(),
                        new NoopHostnameVerifier());
                builder.setSSLSocketFactory(sslsf);
            }
            httpclient = builder.build();

        } catch (final Exception e) {
            throw new ControlLoopRuntimeException(Status.INTERNAL_SERVER_ERROR,
                    restServerParameters.getName() + " Client failed to start", e);
        }
    }

    CloseableHttpResponse execute(HttpRequest request) throws IOException {
        return httpclient.execute(target, request, localContext);
    }

    protected boolean executePut(String path, int statusCode) {
        try (CloseableHttpResponse response = execute(new HttpPut(path))) {
            return response.getStatusLine().getStatusCode() == statusCode;
        } catch (Exception e) {
            return false;
        }
    }

    protected Loop executePost(String path, int statusCode) {
        try (CloseableHttpResponse response = execute(new HttpPost(path))) {
            if (response.getStatusLine().getStatusCode() != statusCode) {
                return null;
            }
            return entityToMap(response.getEntity());
        } catch (Exception e) {
            return null;
        }
    }

    protected Loop executeGet(String path, int statusCode) {
        try (CloseableHttpResponse response = execute(new HttpGet(path))) {
            if (response.getStatusLine().getStatusCode() != statusCode) {
                return null;
            }
            return entityToMap(response.getEntity());
        } catch (Exception e) {
            return null;
        }
    }

    private Loop entityToMap(HttpEntity httpEntity) {
        if (httpEntity == null) {
            return new Loop();
        }
        try {
            return CODER.convert(EntityUtils.toString(httpEntity), Loop.class);
        } catch (ParseException | IOException e) {
            LOGGER.error("error reading Entity", e);
            return new Loop();
        } catch (CoderException e) {
            LOGGER.error("cannot convert to Loop Object", e);
            return new Loop();
        }
    }

    @Override
    public void close() throws IOException {
        httpclient.close();
    }
}
