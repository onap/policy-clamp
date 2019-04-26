/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.loop;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.config.ClampProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Closed loop operations.
 */
@Component
public class LoopOperation {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(LoopOperation.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getMetricsLogger();
    private static final String DCAE_LINK_FIELD = "links";
    private static final String DCAE_STATUS_FIELD = "status";
    private static final String DCAE_DEPLOYMENT_TEMPLATE = "dcae.deployment.template";
    private static final String DCAE_SERVICETYPE_ID = "serviceTypeId";
    private static final String DCAE_INPUTS = "inputs";
    private static final String DCAE_DEPLOYMENT_PREFIX = "closedLoop_";
    private static final String DCAE_DEPLOYMENT_SUFIX = "_deploymentId";
    private final LoopService loopService;
    private final ClampProperties refProp;

    public enum TempLoopState {
        NOT_SUBMITTED, SUBMITTED, DEPLOYED, NOT_DEPLOYED, PROCESSING, IN_ERROR;
    }

    /**
     * The constructor.
     * @param loopService The loop service
     * @param refProp The clamp properties
     */
    @Autowired
    public LoopOperation(LoopService loopService, ClampProperties refProp) {
        this.loopService = loopService;
        this.refProp = refProp;
    }

    /**
     * Get the payload used to send the deploy closed loop request.
     *
     * @param loop The loop
     * @return The payload used to send deploy closed loop request
     * @throws IOException IOException
     */
    public String getDeployPayload(Loop loop) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(loop.getBlueprint());
        JsonObject bluePrint = wrapSnakeObject(yamlMap).getAsJsonObject();

        String serviceTypeId = loop.getDcaeBlueprintId();

        JsonObject rootObject = refProp.getJsonTemplate(DCAE_DEPLOYMENT_TEMPLATE).getAsJsonObject();
        rootObject.addProperty(DCAE_SERVICETYPE_ID, serviceTypeId);
        if (bluePrint != null) {
            rootObject.add(DCAE_INPUTS, bluePrint);
        }
        String apiBodyString = rootObject.toString();
        logger.info("Dcae api Body String - " + apiBodyString);

        return apiBodyString;
    }

    /**
     * Get the deployment id.
     *
     * @param loop The loop
     * @return The deployment id
     * @throws IOException IOException
     */
    public String getDeploymentId(Loop loop) {
        // Set the deploymentId if not present yet
        String deploymentId = "";
        // If model is already deployed then pass same deployment id
        if (loop.getDcaeDeploymentId() != null && !loop.getDcaeDeploymentId().isEmpty()) {
            deploymentId = loop.getDcaeDeploymentId();
        } else {
            deploymentId = DCAE_DEPLOYMENT_PREFIX + loop.getName() + DCAE_DEPLOYMENT_SUFIX;
        }
        return deploymentId;
    }

    /**
     * Update the loop info.
     *
     * @param camelExchange The camel exchange
     * @param loop The loop
     * @param deploymentId The deployment id
     * @throws ParseException The parse exception
     */
    public void updateLoopInfo(Exchange camelExchange, Loop loop, String deploymentId) throws ParseException {
        Message in = camelExchange.getIn();
        String msg = in.getBody(String.class);

        JSONParser parser = new JSONParser();
        Object obj0 = parser.parse(msg);
        JSONObject jsonObj = (JSONObject) obj0;

        JSONObject linksObj = (JSONObject) jsonObj.get(DCAE_LINK_FIELD);
        String statusUrl = (String) linksObj.get(DCAE_STATUS_FIELD);

        // use http4 instead of http, because camel http4 component is used to do the http call
        statusUrl.replace("http", "http4");

        loop.setDcaeDeploymentId(deploymentId);
        loop.setDcaeDeploymentStatusUrl(statusUrl);
        loopService.saveOrUpdateLoop(loop);
    }

    /**
     * Get the Closed Loop status based on the reply from Policy.
     *
     * @param statusCode The status code
     * @return The state based on policy response
     * @throws ParseException The parse exception
     */
    public String analysePolicyResponse(int statusCode) throws ParseException {
        if (statusCode == 200) {
            return TempLoopState.SUBMITTED.toString();
        } else if (statusCode == 404) {
            return TempLoopState.NOT_SUBMITTED.toString();
        }
        return TempLoopState.IN_ERROR.toString();
    }

    /**
     * Get the Closed Loop status based on the reply from DCAE.
     *
     * @param camelExchange The camel exchange
     * @return The state based on DCAE response
     * @throws ParseException The parse exception
     */
    public String analyseDcaeResponse(Exchange camelExchange, Integer statusCode) throws ParseException {
        if (statusCode == null) {
            return TempLoopState.NOT_DEPLOYED.toString();
        }
        if (statusCode == 200) {
            Message in = camelExchange.getIn();
            String msg = in.getBody(String.class);

            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(msg);
            JSONObject jsonObj = (JSONObject) obj0;

            String opType = (String) jsonObj.get("operationType");
            String status = (String) jsonObj.get("status");

            // status = processing/successded/failed
            if (status == "successed") {
                if (opType == "install") {
                    return TempLoopState.DEPLOYED.toString();
                } else if (status == "successed") {
                    return TempLoopState.NOT_DEPLOYED.toString();
                }
            } else if (status == "processing") {
                return TempLoopState.PROCESSING.toString();
            }
        } else if (statusCode == 404) {
            return TempLoopState.NOT_DEPLOYED.toString();
        }
        return TempLoopState.IN_ERROR.toString();
    }

    /**
     * Get the status of the closed loop based on the response from Policy and DCAE.
     *
     * @param policyState The state get from Policy
     * @param dcaeState The state get from DCAE
     * @throws ParseException The parse exception
     */
    public LoopState updateLoopStatus(TempLoopState policyState, TempLoopState dcaeState) {
        LoopState clState = LoopState.IN_ERROR;
        if (policyState == TempLoopState.SUBMITTED) {
            if (dcaeState == TempLoopState.DEPLOYED) {
                clState = LoopState.DEPLOYED;
            } else if (dcaeState == TempLoopState.PROCESSING) {
                clState = LoopState.WAITING;
            } else if (dcaeState == TempLoopState.NOT_DEPLOYED) {
                clState = LoopState.SUBMITTED;
            }
        } else if (policyState == TempLoopState.NOT_SUBMITTED) {
            if (dcaeState == TempLoopState.NOT_DEPLOYED) {
                clState = LoopState.DESIGN;
            }
        }
        return clState;
    }

    private JsonElement wrapSnakeObject(Object obj) {
        // NULL => JsonNull
        if (obj == null) {
            return JsonNull.INSTANCE;
        }

        // Collection => JsonArray
        if (obj instanceof Collection) {
            JsonArray array = new JsonArray();
            for (Object childObj : (Collection<?>) obj) {
                array.add(wrapSnakeObject(childObj));
            }
            return array;
        }

        // Array => JsonArray
        if (obj.getClass().isArray()) {
            JsonArray array = new JsonArray();

            int length = Array.getLength(array);
            for (int i = 0; i < length; i++) {
                array.add(wrapSnakeObject(Array.get(array, i)));
            }
            return array;
        }

        // Map => JsonObject
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;

            JsonObject jsonObject = new JsonObject();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                final String name = String.valueOf(entry.getKey());
                final Object value = entry.getValue();
                jsonObject.add(name, wrapSnakeObject(value));
            }
            return jsonObject;
        }

        // otherwise take it as a string
        return new JsonPrimitive(String.valueOf(obj));
    }

}
