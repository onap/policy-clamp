/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023,2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.main.rest.stub;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.ParticipantMonitoringApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("stub")
@RequiredArgsConstructor
public class ParticipantControllerStub extends AbstractRestController implements ParticipantMonitoringApi {

    private final StubUtils stubUtils;
    @Value("${stub.getSingleParticipantResponse}")
    private String pathToSingleParticipant;

    @Value("${stub.getMultipleParticipantResponse}")
    private String pathToParticipantList;

    @Override
    public ResponseEntity<ParticipantInformation> getParticipant(UUID participantId, Integer page, Integer size,
        UUID xonaprequestid) {
        return stubUtils.getResponse(pathToSingleParticipant, ParticipantInformation.class);
    }

    @Override
    public ResponseEntity<Void> orderAllParticipantsReport(UUID xonaprequestid) {
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Void> orderParticipantReport(UUID participantId, UUID xonaprequestid) {
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<List<ParticipantInformation>> queryParticipants(String name, String version,
        Integer page, Integer size, UUID xonaprequestid) {
        return stubUtils.getResponseList(pathToParticipantList);
    }
}
