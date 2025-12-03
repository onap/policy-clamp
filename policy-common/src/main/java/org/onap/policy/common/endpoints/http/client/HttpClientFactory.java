/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2018 Samsung Electronics Co., Ltd.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.endpoints.http.client;

import java.util.List;
import java.util.Properties;
import org.onap.policy.common.parameters.topic.BusTopicParams;

/**
 * Http Client Factory.
 */
public interface HttpClientFactory {

    /**
     * Build and http client with the following parameters.
     */
    HttpClient build(BusTopicParams busTopicParams) throws HttpClientConfigException;

    /**
     * Build http client from properties.
     */
    List<HttpClient> build(Properties properties) throws HttpClientConfigException;

    /**
     * Get http client.
     *
     * @param name the name
     * @return the http client
     */
    HttpClient get(String name);

    /**
     * List of http clients.
     *
     * @return http clients
     */
    List<HttpClient> inventory();

    /**
     * Destroy by name.
     *
     * @param name name
     */
    void destroy(String name);

    /**
     * destroy the factory.
     */
    void destroy();
}
