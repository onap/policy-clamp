/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.loop.components.external;

import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Transient;
import org.apache.camel.Exchange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.onap.policy.clamp.clds.model.dcae.DcaeInventoryResponse;
import org.onap.policy.clamp.clds.model.dcae.DcaeOperationStatusResponse;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.loop.Loop;
import org.onap.policy.clamp.policy.microservice.MicroServicePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DcaeComponent extends ExternalComponent {
    private static final String INSTALL = "install";
    private static final String PROCESSING = "processing";
    private static final String SUCCEEDED = "succeeded";
    private static final String UNINSTALL = "uninstall";

    @Transient
    private static final Logger logger = LoggerFactory.getLogger(DcaeComponent.class);

    private static final String DCAE_DEPLOYMENT_PREFIX = "CLAMP_";
    private static final String DEPLOYMENT_PARAMETER = "dcaeDeployParameters";
    private static final String DCAE_SERVICETYPE_ID = "serviceTypeId";
    private static final String DCAE_INPUTS = "inputs";
    public static final String UNIQUE_BLUEPRINT_PARAMETERS = "uniqueBlueprintParameters";

    private String name;

    public static final ExternalComponentState BLUEPRINT_DEPLOYED = new ExternalComponentState("BLUEPRINT_DEPLOYED",
            "The DCAE blueprint has been found in the DCAE inventory but not yet instancianted for this loop");
    public static final ExternalComponentState PROCESSING_MICROSERVICE_INSTALLATION = new ExternalComponentState(
            "PROCESSING_MICROSERVICE_INSTALLATION", "Clamp has requested DCAE to install the microservices "
            + "defined in the DCAE blueprint and it's currently processing the request");
    public static final ExternalComponentState MICROSERVICE_INSTALLATION_FAILED = new ExternalComponentState(
            "MICROSERVICE_INSTALLATION_FAILED",
            "Clamp has requested DCAE to install the microservices defined in the DCAE blueprint and it failed");
    public static final ExternalComponentState MICROSERVICE_INSTALLED_SUCCESSFULLY = new ExternalComponentState(
            "MICROSERVICE_INSTALLED_SUCCESSFULLY",
            "Clamp has requested DCAE to install the DCAE blueprint and it has been installed successfully");
    public static final ExternalComponentState PROCESSING_MICROSERVICE_UNINSTALLATION = new ExternalComponentState(
            "PROCESSING_MICROSERVICE_UNINSTALLATION", "Clamp has requested DCAE to uninstall the microservices "
            + "defined in the DCAE blueprint and it's currently processing the request");
    public static final ExternalComponentState MICROSERVICE_UNINSTALLATION_FAILED = new ExternalComponentState(
            "MICROSERVICE_UNINSTALLATION_FAILED",
            "Clamp has requested DCAE to uninstall the microservices defined in the DCAE blueprint and it failed");
    public static final ExternalComponentState MICROSERVICE_UNINSTALLED_SUCCESSFULLY = new ExternalComponentState(
            "MICROSERVICE_UNINSTALLED_SUCCESSFULLY",
            "Clamp has requested DCAE to uninstall the DCAE blueprint and it has been uninstalled successfully");
    public static final ExternalComponentState IN_ERROR = new ExternalComponentState("IN_ERROR",
            "There was an error during the request done to DCAE, look at the logs or try again");

    public DcaeComponent() {
        super(BLUEPRINT_DEPLOYED);
        this.name = "DCAE";
    }

    public DcaeComponent(String name) {
        super(BLUEPRINT_DEPLOYED);
        this.name = "DCAE_" + name;
    }

    @Override
    public String getComponentName() {
        return name;
    }


    /**
     * Convert the json response to a DcaeOperationStatusResponse.
     *
     * @param responseBody The DCAE response Json paylaod
     * @return The dcae object provisioned
     */
    public static DcaeOperationStatusResponse convertDcaeResponse(String responseBody) {
        if (responseBody != null && !responseBody.isEmpty()) {
            return JsonUtils.GSON_JPA_MODEL.fromJson(responseBody, DcaeOperationStatusResponse.class);
        } else {
            return null;
        }
    }

    /**
     * Generate the deployment id, it's random.
     *
     * @return The deployment id
     */
    public static String generateDeploymentId() {
        return DCAE_DEPLOYMENT_PREFIX + UUID.randomUUID();
    }

    /**
     * This method prepare the url returned by DCAE to check the status if fine. It
     * extracts it from the dcaeResponse.
     *
     * @param dcaeResponse The dcae response object
     * @return the Right Url modified if needed
     */
    public static String getStatusUrl(DcaeOperationStatusResponse dcaeResponse) {
        return dcaeResponse.getLinks().getStatus();
    }

    /**
     * Return the deploy payload for DCAE.
     *
     * @param loop The loop object
     * @return The payload used to send deploy closed loop request
     */
    public static String getDeployPayload(Loop loop) {
        JsonObject globalProp = loop.getGlobalPropertiesJson();
        var deploymentProp = globalProp.getAsJsonObject(DEPLOYMENT_PARAMETER).getAsJsonObject(
                UNIQUE_BLUEPRINT_PARAMETERS);

        String serviceTypeId = loop.getLoopTemplate().getDcaeBlueprintId();

        var rootObject = new JsonObject();
        rootObject.addProperty(DCAE_SERVICETYPE_ID, serviceTypeId);
        if (deploymentProp != null) {
            rootObject.add(DCAE_INPUTS, deploymentProp);
        }
        logger.info("DCAE Deploy payload for unique blueprint: {}", rootObject);
        return rootObject.toString();
    }

    /**
     * Return the deploy payload for DCAE.
     *
     * @param loop               The loop object
     * @param microServicePolicy The micro service policy
     * @return The payload used to send deploy closed loop request
     */
    public static String getDeployPayload(Loop loop, MicroServicePolicy microServicePolicy) {
        JsonObject globalProp = loop.getGlobalPropertiesJson();
        var deploymentProp =
                globalProp.getAsJsonObject(DEPLOYMENT_PARAMETER).getAsJsonObject(microServicePolicy.getName());

        String serviceTypeId = microServicePolicy.getDcaeBlueprintId();

        var rootObject = new JsonObject();
        rootObject.addProperty(DCAE_SERVICETYPE_ID, serviceTypeId);
        if (deploymentProp != null) {
            rootObject.add(DCAE_INPUTS, deploymentProp);
        }
        logger.info("DCAE Deploy payload for multiple blueprints: {}", rootObject);
        return rootObject.toString();
    }

    /**
     * Return the uninstallation payload for DCAE.
     *
     * @param loop The loop object
     * @return The payload in string (json)
     */
    public static String getUndeployPayload(Loop loop) {
        var rootObject = new JsonObject();
        rootObject.addProperty(DCAE_SERVICETYPE_ID, loop.getLoopTemplate().getDcaeBlueprintId());
        logger.info("DCAE Undeploy payload for unique blueprint: {}", rootObject);
        return rootObject.toString();
    }

    /**
     * Return the uninstallation payload for DCAE.
     *
     * @param policy The microServicePolicy object
     * @return The payload in string (json)
     */
    public static String getUndeployPayload(MicroServicePolicy policy) {
        var rootObject = new JsonObject();
        rootObject.addProperty(DCAE_SERVICETYPE_ID, policy.getDcaeBlueprintId());
        logger.info("DCAE Undeploy payload for multiple blueprints: {}", rootObject);
        return rootObject.toString();
    }

    @Override
    public ExternalComponentState computeState(Exchange camelExchange) {

        DcaeOperationStatusResponse dcaeResponse = (DcaeOperationStatusResponse) camelExchange.getIn().getExchange()
                .getProperty("dcaeResponse");

        if (dcaeResponse == null) {
            setState(BLUEPRINT_DEPLOYED);
        } else {
            if (dcaeResponse.getOperationType().equals(INSTALL) && dcaeResponse.getStatus().equals(SUCCEEDED)) {
                setState(MICROSERVICE_INSTALLED_SUCCESSFULLY);
            } else {
                if (dcaeResponse.getOperationType().equals(INSTALL) && dcaeResponse.getStatus()
                        .equals(PROCESSING)) {
                    setState(PROCESSING_MICROSERVICE_INSTALLATION);
                } else {
                    if (dcaeResponse.getOperationType().equals(INSTALL) && dcaeResponse.getStatus()
                            .equals("failed")) {
                        setState(MICROSERVICE_INSTALLATION_FAILED);
                    } else {
                        if (dcaeResponse.getOperationType().equals(UNINSTALL)
                                && dcaeResponse.getStatus().equals(SUCCEEDED)) {
                            setState(MICROSERVICE_UNINSTALLED_SUCCESSFULLY);
                        } else {
                            if (dcaeResponse.getOperationType().equals(UNINSTALL)
                                    && dcaeResponse.getStatus().equals(PROCESSING)) {
                                setState(PROCESSING_MICROSERVICE_UNINSTALLATION);
                            } else {
                                if (dcaeResponse.getOperationType().equals(UNINSTALL) && dcaeResponse.getStatus()
                                        .equals("failed")) {
                                    setState(MICROSERVICE_UNINSTALLATION_FAILED);
                                } else {
                                    setState(IN_ERROR);
                                }
                            }
                        }
                    }
                }
            }
        }
        return this.getState();
    }

    /**
     * Convert the json response to a DcaeInventoryResponse.
     *
     * @param responseBody The DCAE response Json paylaod
     * @return list of DcaeInventoryResponse
     * @throws ParseException In case of issues with the Json parsing
     */
    public static List<DcaeInventoryResponse> convertToDcaeInventoryResponse(String responseBody)
            throws ParseException {
        var parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(responseBody);
        JSONArray itemsArray = (JSONArray) jsonObj.get("items");
        Iterator<?> it = itemsArray.iterator();
        List<DcaeInventoryResponse> inventoryResponseList = new LinkedList<>();
        while (it.hasNext()) {
            JSONObject item = (JSONObject) it.next();
            DcaeInventoryResponse response = JsonUtils.GSON.fromJson(item.toString(), DcaeInventoryResponse.class);
            inventoryResponseList.add(response);
        }
        return inventoryResponseList;
    }
}
