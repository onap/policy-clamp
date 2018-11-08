/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model;

import java.util.List;

import javax.ws.rs.BadRequestException;

/**
 * Represent a DCAE Event.
 */
public class DcaeEvent {
    // this is an event we (clds) sends to dcae
    public static final String EVENT_CREATED = "created";
    public static final String EVENT_DISTRIBUTION = "distribute";
    public static final String EVENT_DEPLOYMENT = "deployment";
    public static final String EVENT_UNDEPLOYMENT = "undeployment";
    public static final String ARTIFACT_NAME_SUFFIX = ".yml";

    private String event;
    private String serviceUUID;
    private String resourceUUID;
    private String artifactName; // controlName.yml
    private List<CldsModelInstance> instances;

    /**
     * Transform a DCAE Event Action to a CldsEvent.actionCd
     *
     * @return
     */
    public String getCldsActionCd() {
        if (event == null || event.length() == 0) {
            throw new BadRequestException("action null or empty");
        } else if (event.equalsIgnoreCase(EVENT_CREATED)) {
            return CldsEvent.ACTION_CREATE;
        } else if (event.equalsIgnoreCase(EVENT_DISTRIBUTION)) {
            return CldsEvent.ACTION_DISTRIBUTE;
        } else if (event.equalsIgnoreCase(EVENT_DEPLOYMENT) && (instances == null || instances.isEmpty())) {
            return CldsEvent.ACTION_DEPLOY;
        } else if (event.equalsIgnoreCase(EVENT_DEPLOYMENT)) {
            return CldsEvent.ACTION_DEPLOY;
            // EVENT_UNDEPLOYMENT is defunct - DCAE Proxy will not undeploy
            // individual instances. It will send an empty list of
            // deployed instances to indicate all have been removed. Or it will
            // send an updated list to indicate those that
            // are still deployed with any not on the list considered
            // undeployed.
        } else if (event.equals(EVENT_UNDEPLOYMENT)) {
            return CldsEvent.ACTION_UNDEPLOY;
        }

        throw new BadRequestException("event value not valid: " + event);
    }

    /**
     * Derive the controlName from the artifactName.
     *
     * @return the controlName
     */
    public String getControlName() {
        if (artifactName != null && artifactName.endsWith(ARTIFACT_NAME_SUFFIX)) {
            return artifactName.substring(0, artifactName.length() - ARTIFACT_NAME_SUFFIX.length());
        } else {
            throw new BadRequestException("artifactName value not valid (expecting it to end with "
                + ARTIFACT_NAME_SUFFIX + "): " + artifactName);
        }
    }

    /**
     * @return the event
     */
    public String getEvent() {
        return event;
    }

    /**
     * @param event
     *        the event to set
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * @return the serviceUUID
     */
    public String getServiceUUID() {
        return serviceUUID;
    }

    /**
     * @param serviceUUID
     *        the serviceUUID to set
     */
    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    /**
     * @return the resourceUUID
     */
    public String getResourceUUID() {
        return resourceUUID;
    }

    /**
     * @param resourceUUID
     *        the resourceUUID to set
     */
    public void setResourceUUID(String resourceUUID) {
        this.resourceUUID = resourceUUID;
    }

    /**
     * @return the artifactName
     */
    public String getArtifactName() {
        return artifactName;
    }

    /**
     * @param artifactName
     *        the artifactName to set
     */
    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public List<CldsModelInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<CldsModelInstance> instances) {
        this.instances = instances;
    }
}
