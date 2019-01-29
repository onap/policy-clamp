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

package org.onap.clamp.clds.model.actions;

import com.att.eelf.configuration.EELFLogger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Arrays;
import java.util.List;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.util.JsonUtils;

/**
 * Interface for actions that the user can do according to the last event.
 *
 *
 */
public interface ActionsHandler {

    enum ModelType {
        SIMPLE_MODEL("simpleModel"), POLICY_MODEL("policyModel");
        private final String type;

        ModelType(String type) {
            this.type = type;
        }

        public final String getType() {
            return this.type;
        }
    }

    EELFLogger getLogger();

    /**
     * This method determines a list of actions that the user can do according to
     * the last event.
     *
     * @param event
     *        The last event
     * @param propText
     *        The Json properties string
     * @return A list of actions
     */
    default List<String> determinePermittedActionsOnLastEvent(CldsEvent event, String propText) {
        List<String> permittedActions;
        String actionCd = getCurrentActionCd(event);
        switch (actionCd) {
        case CldsEvent.ACTION_CREATE:
            permittedActions = Arrays.asList(CldsEvent.ACTION_SUBMIT, CldsEvent.ACTION_TEST, CldsEvent.ACTION_DELETE);
            if (isTypeModel(propText, ModelType.SIMPLE_MODEL)) {
                permittedActions = Arrays.asList(CldsEvent.ACTION_SUBMITDCAE, CldsEvent.ACTION_SUBMITPOLICY,
                    CldsEvent.ACTION_TEST, CldsEvent.ACTION_DELETE);
            }
            break;
        case CldsEvent.ACTION_SUBMIT:
        case CldsEvent.ACTION_RESUBMIT:
        case CldsEvent.ACTION_DISTRIBUTE:
            permittedActions = Arrays.asList(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_RESUBMIT,
                CldsEvent.ACTION_DELETE);
            if (isTypeModel(propText, ModelType.SIMPLE_MODEL)) {
                permittedActions = Arrays.asList(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_SUBMITDCAE,
                    CldsEvent.ACTION_DELETE);
            }
            break;
        case CldsEvent.ACTION_SUBMITDCAE:
            permittedActions = Arrays.asList(CldsEvent.ACTION_SUBMITDCAE, CldsEvent.ACTION_DELETE);
            break;
        case CldsEvent.ACTION_SUBMITPOLICY:
            permittedActions = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STOP);
            break;
        case CldsEvent.ACTION_UNDEPLOY:
            permittedActions = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_DEPLOY,
                CldsEvent.ACTION_RESUBMIT, CldsEvent.ACTION_DELETE);
            if (isTypeModel(propText, ModelType.SIMPLE_MODEL)) {
                permittedActions = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_DEPLOY,
                    CldsEvent.ACTION_SUBMITDCAE, CldsEvent.ACTION_DELETE);
            }
            break;
        case CldsEvent.ACTION_DEPLOY:
            permittedActions = Arrays.asList(CldsEvent.ACTION_UNDEPLOY, CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STOP);
            break;
        case CldsEvent.ACTION_RESTART:
        case CldsEvent.ACTION_UPDATE:
            permittedActions = Arrays.asList(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STOP,
                CldsEvent.ACTION_UNDEPLOY);
            if (isTypeModel(propText, ModelType.POLICY_MODEL)) {
                permittedActions = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STOP);
            }
            break;
        case CldsEvent.ACTION_STOP:
            permittedActions = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_RESTART,
                CldsEvent.ACTION_UNDEPLOY);
            if (isTypeModel(propText, ModelType.POLICY_MODEL)) {
                permittedActions = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_RESTART,
                    CldsEvent.ACTION_DELETE);
            }
            break;
        default:
            getLogger().warn("Invalid current actionCd: " + actionCd);
            permittedActions = Arrays.asList();
        }
        return permittedActions;
    }

    /**
     * This method returns the action of the event or a default one if not found.
     *
     * @param event
     *        The last event
     * @return The action
     */
    default String getCurrentActionCd(CldsEvent event) {
        // current default actionCd is CREATE
        String actionCd = CldsEvent.ACTION_CREATE;
        if (event != null && event.getActionCd() != null) {
            actionCd = event.getActionCd();
        }
        return actionCd;
    }

    /**
     * Check whether the text properties is of specified type ModelType.
     *
     * @param propText
     *        The Clamp Json properties
     * @param key
     *        The model type
     * @return True if matches the right type specified
     */
    default boolean isTypeModel(String propText, ModelType key) {
        boolean result = false;
        try {
            if (propText != null) {
                JsonObject modelJson = JsonUtils.GSON.fromJson(propText, JsonObject.class);
                JsonElement modelJsonOfType = modelJson.get(key.getType());
                if (modelJsonOfType != null
                    && modelJsonOfType.isJsonPrimitive()
                    && modelJsonOfType.getAsJsonPrimitive().getAsBoolean()) {
                    result = true;
                }
            }
        } catch (JsonParseException e) {
            getLogger().error("Error while parsing propText json", e);
        }
        return result;
    }
}
