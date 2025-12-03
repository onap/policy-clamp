/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.policy.common.message.bus.event.base;

import lombok.Getter;
import org.onap.policy.common.parameters.topic.BusTopicParams;

/**
 * Bus Topic Base.
 */
@Getter
public abstract class BusTopicBase extends TopicBase implements ApiKeyEnabled {

    /**
     * API Key.
     */
    protected String apiKey;

    /**
     * API Secret.
     */
    protected String apiSecret;

    /**
     * Use https.
     */
    protected boolean useHttps;

    /**
     * Allow tracing.
     */
    protected boolean allowTracing;

    /**
     * allow self-signed certificates.
     */
    protected boolean allowSelfSignedCerts;

    /**
     * Instantiates a new Bus Topic Base.
     *
     * <p>servers list of servers
     * topic: the topic name
     * apiKey: API Key
     * apiSecret: API Secret
     * useHttps: does connection use HTTPS?
     * allowTracing: Is tracing allowed?
     * allowSelfSignedCerts: are self-signed certificates allow
     *
     * @param busTopicParams holds all our parameters
     * @throws IllegalArgumentException if invalid parameters are present
     */
    protected BusTopicBase(BusTopicParams busTopicParams) {
        super(busTopicParams.getServers(), busTopicParams.getTopic(), busTopicParams.getEffectiveTopic());
        this.apiKey = busTopicParams.getApiKey();
        this.apiSecret = busTopicParams.getApiSecret();
        this.useHttps = busTopicParams.isUseHttps();
        this.allowTracing = busTopicParams.isAllowTracing();
        this.allowSelfSignedCerts = busTopicParams.isAllowSelfSignedCerts();
    }

    protected boolean anyNullOrEmpty(String... args) {
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    protected boolean allNullOrEmpty(String... args) {
        for (String arg : args) {
            if (!(arg == null || arg.isEmpty())) {
                return false;
            }
        }

        return true;
    }


    @Override
    public String toString() {
        return "BusTopicBase [apiKey=" + apiKey + ", apiSecret=" + apiSecret + ", useHttps=" + useHttps
            + ", allowSelfSignedCerts=" + allowSelfSignedCerts + ", toString()=" + super.toString() + "]";
    }

}
