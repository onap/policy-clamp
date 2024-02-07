/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.sim.rest;

import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.sim.controller.genapi.SimulatorParticipantControllerApi;
import org.onap.policy.clamp.acm.participant.sim.main.handler.AutomationCompositionElementHandler;
import org.onap.policy.clamp.acm.participant.sim.model.InternalData;
import org.onap.policy.clamp.acm.participant.sim.model.InternalDatas;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/v2", produces = {MediaType.APPLICATION_JSON})
public class SimulatorController implements SimulatorParticipantControllerApi {

    private final AutomationCompositionElementHandler automationCompositionElementHandler;

    @Override
    public ResponseEntity<SimConfig> getConfig(UUID xonapRequestId) {
        return new ResponseEntity<>(automationCompositionElementHandler.getConfig(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> setConfig(UUID xonapRequestId, @Valid @RequestBody SimConfig body) {
        automationCompositionElementHandler.setConfig(body);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AutomationCompositions> getAutomationCompositions(UUID xonapRequestId) {
        return new ResponseEntity<>(automationCompositionElementHandler.getAutomationCompositions(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AutomationComposition> getAutomationComposition(UUID instanceId, UUID xonapRequestId) {
        return new ResponseEntity<>(automationCompositionElementHandler.getAutomationComposition(instanceId),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<InternalDatas> getDatas(UUID xonapRequestId) {
        return new ResponseEntity<>(automationCompositionElementHandler.getDataList(), HttpStatus.OK);
    }

    /**
     * Set instance Data.
     *
     * @param body the Data
     * @return Void
     */
    @Override
    public ResponseEntity<Void> setData(UUID xonapRequestId, @Valid @RequestBody InternalData body) {
        automationCompositionElementHandler.setOutProperties(body.getAutomationCompositionId(),
                body.getAutomationCompositionElementId(), body.getUseState(), body.getOperationalState(),
                body.getOutProperties());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<InternalDatas> getCompositionDatas(UUID xonapRequestId) {
        return new ResponseEntity<>(automationCompositionElementHandler.getCompositionDataList(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> setCompositionData(UUID xonapRequestId, @Valid InternalData body) {
        automationCompositionElementHandler.setCompositionOutProperties(body.getCompositionId(),
                body.getCompositionDefinitionElementId(), body.getOutProperties());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
