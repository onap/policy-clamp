/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.main.rest;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.ParticipantMonitoringApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.acm.runtime.participants.AcmParticipantProvider;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("default")
public class ParticipantController extends AbstractRestController implements ParticipantMonitoringApi {

    private final AcmParticipantProvider acmParticipantProvider;

    @Override
    public ResponseEntity<ParticipantInformation> getParticipant(UUID participantId, UUID requestId) {
        ParticipantInformation participantInformation = acmParticipantProvider
            .getParticipantById(participantId.toString());
        return ResponseEntity.ok().body(participantInformation);
    }

    @Override
    public ResponseEntity<Void> orderAllParticipantsReport(UUID requestId) {
        acmParticipantProvider.sendAllParticipantStatusRequest();
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Void> orderParticipantReport(UUID participantId, UUID requestId) {
        acmParticipantProvider.sendParticipantStatusRequest(participantId.toString());
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<List<ParticipantInformation>> queryParticipants(String name, String version,
                                                                          UUID requestId) {
        List<ParticipantInformation> participantInformationList = acmParticipantProvider.getAllParticipants();
        return ResponseEntity.ok().body(participantInformationList);
    }
}
