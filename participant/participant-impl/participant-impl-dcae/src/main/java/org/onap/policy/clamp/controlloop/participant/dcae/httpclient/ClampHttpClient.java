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
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ClampEndPoints;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameters;
import org.onap.policy.clamp.controlloop.participant.dcae.model.ExternalComponent;
import org.onap.policy.clamp.controlloop.participant.dcae.model.Loop;
import org.springframework.stereotype.Component;

@Component
public class ClampHttpClient extends AbstractHttpClient {

    private final ClampEndPoints endPoints;
    public static final String STATUS_NOT_FOUND = "STATUS_NOT_FOUND";
    public static final String POLICY_NOT_FOUND = "POLICY_NOT_FOUND";

    /**
     * Constructor.
     *
     * @param parameters the DCAE parameters
     */
    public ClampHttpClient(ParticipantDcaeParameters parameters) {
        super(parameters.getClampClientParameters());
        this.endPoints = parameters.getClampClientEndPoints();
    }

    /**
     * Create.
     *
     * @param loopName the loopName
     * @param templateName the templateName
     * @return the Loop object or null if error occurred
     */
    public Loop create(String loopName, String templateName) {
        return executePost(String.format(endPoints.getCreate(), loopName, templateName), HttpStatus.SC_OK);
    }

    /**
     * Deploy.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean deploy(String loopName) {
        return executePut(endPoints.getDeploy() + loopName, HttpStatus.SC_ACCEPTED);
    }

    /**
     * Get Status.
     *
     * @param loopName the loopName
     * @return the Loop object or null if error occurred
     */
    public Loop getstatus(String loopName) {
        return executeGet(endPoints.getStatus() + loopName, HttpStatus.SC_OK);
    }

    /**
     * Undeploy.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean undeploy(String loopName) {
        return executePut(endPoints.getUndeploy() + loopName, HttpStatus.SC_ACCEPTED);
    }

    /**
     * Stop.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean stop(String loopName) {
        return executePut(endPoints.getStop() + loopName, HttpStatus.SC_OK);
    }

    /**
     * Delete.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean delete(String loopName) {
        return executePut(endPoints.getDelete() + loopName, HttpStatus.SC_OK);
    }

    /**
     * return status from Loop object.
     *
     * @param loop Loop
     * @return status
     */
    public static String getStatusCode(Loop loop) {
        if (loop == null || loop.getComponents() == null || loop.getComponents().isEmpty()) {
            return STATUS_NOT_FOUND;
        }
        ExternalComponent externalComponent = loop.getComponents().get("DCAE");
        if (externalComponent == null || externalComponent.getComponentState() == null) {
            return STATUS_NOT_FOUND;
        }

        return externalComponent.getComponentState().getStateName();
    }
}
