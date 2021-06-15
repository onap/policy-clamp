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

import org.apache.http.HttpStatus;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ConsulEndPoints;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameters;
import org.springframework.stereotype.Component;

@Component
public class ConsulDcaeHttpClient extends AbstractHttpClient {

    private final ConsulEndPoints endPoints;

    /**
     * Constructor.
     */
    public ConsulDcaeHttpClient(ParticipantDcaeParameters parameters) {
        super(parameters.getConsulClientParameters());
        this.endPoints = parameters.getConsulClientEndPoints();
    }

    /**
     * Call deploy consult.
     *
     * @param jsonEntity the Entity
     * @return true
     */
    public boolean deploy(String name, String jsonEntity) {
        return executePut(endPoints.getDeploy() + name, jsonEntity, HttpStatus.SC_OK);
    }
}
