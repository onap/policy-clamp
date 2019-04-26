/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * 
 */

package org.onap.clamp.clds.sdc.controller;

import org.onap.sdc.api.consumer.IDistributionStatusMessage;
import org.onap.sdc.utils.DistributionStatusEnum;

public class DistributionStatusMessage implements IDistributionStatusMessage {

    private String artifactURL;
    private String consumerID;
    private String distributionID;
    private DistributionStatusEnum distributionStatus;
    private long timestamp;

    /**
     * Distribution status message constructor.
     *
     * @param artifactUrl
     *        Url of specific SDC artifact(resource)
     * @param consumerId
     *        Unique ID of SDC component instance
     * @param distributionId
     *        Distribution ID published in the distribution notification.
     * @param distributionStatusEnum
     *        Status to send in the message
     * @param timestamp
     *        Timestamp of the message
     */
    public DistributionStatusMessage(final String artifactUrl, final String consumerId, final String distributionId,
            final DistributionStatusEnum distributionStatusEnum, final long timestamp) {
        this.artifactURL = artifactUrl;
        this.consumerID = consumerId;
        this.distributionID = distributionId;
        this.distributionStatus = distributionStatusEnum;
        this.timestamp = timestamp;
    }

    @Override
    public String getArtifactURL() {
        return artifactURL;
    }

    @Override
    public String getConsumerID() {
        return consumerID;
    }

    @Override
    public String getDistributionID() {
        return distributionID;
    }

    @Override
    public DistributionStatusEnum getStatus() {
        return distributionStatus;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
