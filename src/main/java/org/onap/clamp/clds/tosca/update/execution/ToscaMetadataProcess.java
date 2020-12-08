/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.tosca.update.execution;

import com.google.gson.JsonObject;
import org.onap.clamp.loop.service.Service;

/**
 * This code is the interface that must be implemented to have a tosca process.
 */
public abstract class ToscaMetadataProcess {

    /**
     * This method add some elements to the JsonObject childObject passed in argument.
     * The process can take multiple parameters in arguments.
     *
     * @param parameters   The parameters required by the process
     * @param childObject  The Json Object modified by the current process
     * @param serviceModel The service model associated to do clamp enrichment
     */
    public abstract void executeProcess(String parameters, JsonObject childObject, Service serviceModel);
}
