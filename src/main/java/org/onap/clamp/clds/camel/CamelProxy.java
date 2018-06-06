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

package org.onap.clamp.clds.camel;

import org.apache.camel.ExchangeProperty;

/**
 * This interface describes the CamelProxy parameters that must be passed to the
 * Camel flow.
 */
public interface CamelProxy {

    /**
     * This method is called when invoking a camel flow.
     * 
     * @param actionCommand
     *            The action coming from the Clamp UI (like SUBMIT, UPDATE,
     *            DELETE, ...)
     * @param modelProperties
     *            The Model properties created based on the BPMN Json and
     *            Properties Json
     * @param modelBpmnProperties
     *            The Json with all the properties describing the flow
     * @param modelName
     *            The model name
     * @param controlName
     *            The control loop name
     * @param docText
     *            The Global properties JSON containing YAML (coming from CLamp
     *            template)
     * @param isTest
     *            Is a test or not (flag coming from the UI)
     * @param userId
     *            The user ID coming from the UI
     * @param isInsertTestEvent
     *            Is a test or not (flag coming from the UI)
     * @param eventAction
     *            The latest event action in database (like CREATE, SUBMIT, ...)
     * @return A string containing the result of the Camel flow execution
     */
    String submit(@ExchangeProperty("actionCd") String actionCommand,
            @ExchangeProperty("modelProp") String modelProperties,
            @ExchangeProperty("modelBpmnProp") String modelBpmnProperties,
            @ExchangeProperty("modelName") String modelName, @ExchangeProperty("controlName") String controlName,
            @ExchangeProperty("docText") String docText, @ExchangeProperty("isTest") boolean isTest,
            @ExchangeProperty("userid") String userId, @ExchangeProperty("isInsertTestEvent") boolean isInsertTestEvent,
            @ExchangeProperty("eventAction") String eventAction);
}
