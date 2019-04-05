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
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.onap.clamp.clds.client.DcaeDispatcherServices;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.ONAPLogConstants;
import org.onap.clamp.exception.OperationException;
import org.onap.clamp.util.HttpConnectionManager;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Closed loop operations
 */
@Component
public class LoopOperation {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(LoopOperation.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getMetricsLogger();
    private final DcaeDispatcherServices dcaeDispatcherServices;
    private final LoopService loopService;
    private LoggingUtils util = new LoggingUtils(logger);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    public LoopOperation(LoopService loopService, DcaeDispatcherServices dcaeDispatcherServices,
        ClampProperties refProp, HttpConnectionManager httpConnectionManager) {
        this.loopService = loopService;
        this.dcaeDispatcherServices = dcaeDispatcherServices;
    }

    /**
     * Deploy the closed loop.
     *
     * @param loopName
     *        the loop name
     * @return the updated loop
     * @throws Exceptions
     *         during the operation
     */
    public Loop deployLoop(Exchange camelExchange, String loopName) throws OperationException {
        util.entering(request, "CldsService: Deploy model");
        Date startTime = new Date();
        Loop loop = loopService.getLoop(loopName);

        if (loop == null) {
            String msg = "Deploy loop exception: Not able to find closed loop:" + loopName;
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), msg, Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // verify the current closed loop state
        if (loop.getLastComputedState() != LoopState.SUBMITTED) {
            String msg = "Deploy loop exception: This closed loop is in state:" + loop.getLastComputedState()
                + ". It could be deployed only when it is in SUBMITTED state.";
            util.exiting(HttpStatus.CONFLICT.toString(), msg, Level.INFO, ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // Set the deploymentId if not present yet
        String deploymentId = "";
        // If model is already deployed then pass same deployment id
        if (loop.getDcaeDeploymentId() != null && !loop.getDcaeDeploymentId().isEmpty()) {
            deploymentId = loop.getDcaeDeploymentId();
        } else {
            loop.setDcaeDeploymentId(deploymentId = "closedLoop_" + loopName + "_deploymentId");
        }

        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(loop.getBlueprint());
        JsonObject bluePrint = wrapSnakeObject(yamlMap).getAsJsonObject();

        loop.setDcaeDeploymentStatusUrl(
            dcaeDispatcherServices.createNewDeployment(deploymentId, loop.getDcaeBlueprintId(), bluePrint));
        loop.setLastComputedState(LoopState.DEPLOYED);
        // save the updated loop
        loopService.saveOrUpdateLoop(loop);

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("Deploy model completed");
        util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return loop;
    }

    /**
     * Un deploy closed loop.
     *
     * @param loopName
     *        the loop name
     * @return the updated loop
     */
    public Loop unDeployLoop(String loopName) throws OperationException {
        util.entering(request, "LoopOperation: Undeploy the closed loop");
        Date startTime = new Date();
        Loop loop = loopService.getLoop(loopName);

        if (loop == null) {
            String msg = "Undeploy loop exception: Not able to find closed loop:" + loopName;
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), msg, Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // verify the current closed loop state
        if (loop.getLastComputedState() != LoopState.DEPLOYED) {
            String msg = "Unploy loop exception: This closed loop is in state:" + loop.getLastComputedState()
                + ". It could be undeployed only when it is in DEPLOYED state.";
            util.exiting(HttpStatus.CONFLICT.toString(), msg, Level.INFO, ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        loop.setDcaeDeploymentStatusUrl(
            dcaeDispatcherServices.deleteExistingDeployment(loop.getDcaeDeploymentId(), loop.getDcaeBlueprintId()));

        // clean the deployment ID
        loop.setDcaeDeploymentId(null);
        loop.setLastComputedState(LoopState.SUBMITTED);

        // save the updated loop
        loopService.saveOrUpdateLoop(loop);

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("Undeploy model completed");
        util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return loop;
    }

    private JsonElement wrapSnakeObject(Object o) {
        // NULL => JsonNull
        if (o == null)
            return JsonNull.INSTANCE;

        // Collection => JsonArray
        if (o instanceof Collection) {
            JsonArray array = new JsonArray();
            for (Object childObj : (Collection<?>) o)
                array.add(wrapSnakeObject(childObj));
            return array;
        }

        // Array => JsonArray
        if (o.getClass().isArray()) {
            JsonArray array = new JsonArray();

            int length = Array.getLength(array);
            for (int i = 0; i < length; i++)
                array.add(wrapSnakeObject(Array.get(array, i)));
            return array;
        }

        // Map => JsonObject
        if (o instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) o;

            JsonObject jsonObject = new JsonObject();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                final String name = String.valueOf(entry.getKey());
                final Object value = entry.getValue();
                jsonObject.add(name, wrapSnakeObject(value));
            }
            return jsonObject;
        }

        // otherwise take it as a string
        return new JsonPrimitive(String.valueOf(o));
    }

    /**
     * Submit the Ms policies.
     *
     * @param loopName
     *        the loop name
     * @return the updated loop
     * @throws IOException
     *         IO exception
     * @throws Exceptions
     *         during the operation
     */
    public Loop submitMsPolicies(String loopName) throws OperationException, IOException {
        util.entering(request, "LoopOperation: delete microservice policies");
        Date startTime = new Date();
        Loop loop = loopService.getLoop(loopName);

        if (loop == null) {
            String msg = "Submit MS policies exception: Not able to find closed loop:" + loopName;
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), msg, Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // verify the current closed loop state
        if (loop.getLastComputedState() != LoopState.SUBMITTED && loop.getLastComputedState() != LoopState.DESIGN) {
            String msg = "Submit MS policies exception: This closed loop is in state:" + loop.getLastComputedState()
                + ". It could be deleted only when it is in SUBMITTED state.";
            util.exiting(HttpStatus.CONFLICT.toString(), msg, Level.INFO, ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // Establish the api call to Policy to create the ms services
        // policyOp.createMsPolicy(loop.getMicroServicePolicies());

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("Deletion of MS policies completed");
        util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return loop;
    }

    /**
     * Delete the Ms policies.
     *
     * @param loopName
     *        the loop name
     * @return the updated loop
     * @throws IOException
     *         IO exception
     * @throws Exceptions
     *         during the operation
     */
    public Loop deleteMsPolicies(Exchange camelExchange, String loopName) throws OperationException, IOException {
        util.entering(request, "LoopOperation: delete microservice policies");
        Date startTime = new Date();
        Loop loop = loopService.getLoop(loopName);

        if (loop == null) {
            String msg = "Delete MS policies exception: Not able to find closed loop:" + loopName;
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), msg, Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // verify the current closed loop state
        if (loop.getLastComputedState() != LoopState.SUBMITTED) {
            String msg = "Delete MS policies exception: This closed loop is in state:" + loop.getLastComputedState()
                + ". It could be deleted only when it is in SUBMITTED state.";
            util.exiting(HttpStatus.CONFLICT.toString(), msg, Level.INFO, ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // Establish the api call to Policy to create the ms services
        // policyOp.deleteMsPolicy(loop.getMicroServicePolicies());

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("Deletion of MS policies completed");
        util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return loop;
    }

    /**
     * Delete the operational policy.
     *
     * @param loopName
     *        the loop name
     * @return the updated loop
     * @throws Exceptions
     *         during the operation
     */
    public Loop deleteOpPolicy(Exchange camelExchange, String loopName) throws OperationException {
        util.entering(request, "LoopOperation: delete guard policy");
        Date startTime = new Date();
        Loop loop = loopService.getLoop(loopName);

        if (loop == null) {
            String msg = "Delete guard policy exception: Not able to find closed loop:" + loopName;
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), msg, Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // verify the current closed loop state
        if (loop.getLastComputedState() != LoopState.SUBMITTED) {
            String msg = "Delete MS policies exception: This closed loop is in state:" + loop.getLastComputedState()
                + ". It could be deleted only when it is in SUBMITTED state.";
            util.exiting(HttpStatus.CONFLICT.toString(), msg, Level.INFO, ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // Establish the api call to Policy to delete operational policy
        // client.deleteOpPolicy();

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("Deletion of Guard policy completed");
        util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return loop;
    }

    /**
     * Delete the Guard policy.
     *
     * @param loopName
     *        the loop name
     * @return the updated loop
     * @throws Exceptions
     *         during the operation
     */
    public Loop deleteGuardPolicy(Exchange camelExchange, String loopName) throws OperationException {
        util.entering(request, "LoopOperation: delete operational policy");
        Date startTime = new Date();
        Loop loop = loopService.getLoop(loopName);

        if (loop == null) {
            String msg = "Delete operational policy exception: Not able to find closed loop:" + loopName;
            util.exiting(HttpStatus.INTERNAL_SERVER_ERROR.toString(), msg, Level.INFO,
                ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // verify the current closed loop state
        if (loop.getLastComputedState() != LoopState.SUBMITTED) {
            String msg = "Delete MS policies exception: This closed loop is in state:" + loop.getLastComputedState()
                + ". It could be deleted only when it is in SUBMITTED state.";
            util.exiting(HttpStatus.CONFLICT.toString(), msg, Level.INFO, ONAPLogConstants.ResponseStatus.ERROR);
            throw new OperationException(msg);
        }

        // Establish the api call to Policy to delete Guard policy
        // client.deleteOpPolicy();

        // audit log
        LoggingUtils.setTimeContext(startTime, new Date());
        auditLogger.info("Deletion of operational policy completed");
        util.exiting(HttpStatus.OK.toString(), "Successful", Level.INFO, ONAPLogConstants.ResponseStatus.COMPLETED);
        return loop;
    }
}
