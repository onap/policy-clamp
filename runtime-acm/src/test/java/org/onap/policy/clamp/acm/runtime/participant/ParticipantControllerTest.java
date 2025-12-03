/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.rest.ParticipantController;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.common.acm.utils.resources.ResourceUtils;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Class to perform unit test of {@link ParticipantController}.
 *
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "default" })
class ParticipantControllerTest extends CommonRestController {
    private static final String PARTICIPANTS_ENDPOINT = "participants";
    private static final int NUMBER_RECORDS = 10;

    @Autowired
    private AcDefinitionProvider acDefinitionProvider;

    @Autowired
    private AutomationCompositionInstantiationProvider instantiationProvider;

    @LocalServerPort
    private int randomServerPort;

    private static final Coder CODER = new StandardCoder();
    private static final String PARTICIPANT_JSON = "src/test/resources/providers/TestParticipant.json";
    private static final String PARTICIPANT_JSON2 = "src/test/resources/providers/TestParticipant2.json";
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final String NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition";
    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();
    private static AutomationComposition automationComposition = new AutomationComposition();
    private static final List<Participant> inputParticipants = new ArrayList<>();
    private static final String ORIGINAL_JSON = ResourceUtils.getResourceAsString(PARTICIPANT_JSON);
    private static final String ORIGINAL_JSON2 = ResourceUtils.getResourceAsString(PARTICIPANT_JSON2);

    @Autowired
    private ParticipantProvider participantProvider;

    /**
     * Adds participants to the db from json file.
     */
    @BeforeAll
    static void setUpBeforeClass() throws CoderException {
        inputParticipants.add(CODER.decode(ORIGINAL_JSON, Participant.class));
        inputParticipants.add(CODER.decode(ORIGINAL_JSON2, Participant.class));
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        automationComposition =
            InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
    }

    @BeforeEach
    void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    @Test
    void testSwagger() {
        super.testSwagger(PARTICIPANTS_ENDPOINT);
    }

    @Test
    void testUnauthorizedQuery() {
        assertUnauthorizedGet(PARTICIPANTS_ENDPOINT);
    }

    @Test
    void testQueryParticipant() {
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        var replica = CommonTestData.createParticipantReplica(CommonTestData.getParticipantId());
        participant.getReplicas().put(replica.getReplicaId(), replica);
        participantProvider.saveParticipant(participant);
        for (var i = 0; i < NUMBER_RECORDS; i++) {
            createAcDefinitionInDB("QueryParticipant" + i);
        }
        // valid pagination
        validateParticipantPageable("?page=1&size=4", 4);

        // not valid pagination
        validateParticipantNotPageable("?page=0", NUMBER_RECORDS);
        validateParticipantNotPageable("?size=5", NUMBER_RECORDS);
        validateParticipantNotPageable("", NUMBER_RECORDS);
    }

    private void validateParticipantPageable(String url, int size) {
        var participantInfo = getParticipantInformation(url);
        assertThat(participantInfo.getAcNodeTemplateStateDefinitionMap()).hasSize(size);
        assertThat(participantInfo.getAcElementInstanceMap()).hasSize(size);
    }

    private void validateParticipantNotPageable(String url, int size) {
        var participantInfo = getParticipantInformation(url);
        assertThat(participantInfo.getAcNodeTemplateStateDefinitionMap()).hasSizeGreaterThanOrEqualTo(size);
        assertThat(participantInfo.getAcElementInstanceMap()).hasSizeGreaterThanOrEqualTo(size);
    }

    private ParticipantInformation getParticipantInformation(String url) {
        var invocationBuilder = super.sendRequest(
                PARTICIPANTS_ENDPOINT + "/" + CommonTestData.getParticipantId() + url);
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            var participantInfo = response.readEntity(ParticipantInformation.class);
            assertNotNull(participantInfo);
            return participantInfo;
        }
    }

    @Test
    void testBadQueryParticipant() {
        participantProvider.saveParticipant(inputParticipants.get(0));
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT + "/" + UUID.randomUUID());
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void getAllParticipants() {
        inputParticipants.forEach(participantProvider::saveParticipant);
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT);
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            List<ParticipantInformation> entityList = response.readEntity(new GenericType<>() {
            });
            assertThat(entityList).isNotEmpty();
            var participantIds =
                    entityList.stream().map(ParticipantInformation::getParticipant).map(Participant::getParticipantId)
                            .collect(Collectors.toSet());
            inputParticipants.forEach(p -> assertThat(participantIds).contains(p.getParticipantId()));
        }
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        var replica = CommonTestData.createParticipantReplica(CommonTestData.getParticipantId());
        participant.getReplicas().put(replica.getReplicaId(), replica);
        participantProvider.saveParticipant(participant);
        for (var i = 0; i < NUMBER_RECORDS; i++) {
            createAcDefinitionInDB("AllParticipants" + i);
        }
        // valid pagination
        validateAllParticipantsPageable("?page=1&size=4", 4);

        // not valid pagination
        validateAllParticipantsNotPageable("?page=0", NUMBER_RECORDS);
        validateAllParticipantsNotPageable("?size=5", NUMBER_RECORDS);
        validateAllParticipantsNotPageable("", NUMBER_RECORDS);
    }

    private void validateAllParticipantsNotPageable(String url, int size) {
        var participantInfo = getFirstParticipantInformation(url);
        assertThat(participantInfo.getAcNodeTemplateStateDefinitionMap()).hasSizeGreaterThanOrEqualTo(size);
        assertThat(participantInfo.getAcElementInstanceMap()).hasSizeGreaterThanOrEqualTo(size);
    }

    private void validateAllParticipantsPageable(String url, int size) {
        var participantInfo = getFirstParticipantInformation(url);
        assertThat(participantInfo.getAcNodeTemplateStateDefinitionMap()).hasSize(size);
        assertThat(participantInfo.getAcElementInstanceMap()).hasSize(size);
    }

    private ParticipantInformation getFirstParticipantInformation(String url) {
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT + url);
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            List<ParticipantInformation> entityList = response.readEntity(new GenericType<>() {});
            var participantInfoOpt = entityList.stream()
                    .filter(p -> CommonTestData.getParticipantId().equals(p.getParticipant().getParticipantId()))
                    .findFirst();
            assertThat(participantInfoOpt).isPresent();
            return participantInfoOpt.get();
        }
    }

    private void createAcDefinitionInDB(String name) {
        var serviceTemplateCreate = new ToscaServiceTemplate(serviceTemplate);
        serviceTemplateCreate.setName(name);
        var acmDefinition = CommonTestData.createAcDefinition(serviceTemplateCreate, AcTypeState.PRIMED);
        acDefinitionProvider.updateAcDefinition(acmDefinition, NODE_TYPE);
        var automationCompositionCreate = new AutomationComposition(automationComposition);
        automationCompositionCreate.setCompositionId(acmDefinition.getCompositionId());
        automationCompositionCreate.setName(acmDefinition.getServiceTemplate().getName());
        var elements = new ArrayList<>(automationCompositionCreate.getElements().values());
        automationCompositionCreate.getElements().clear();
        for (var element : elements) {
            element.setId(UUID.randomUUID());
            automationCompositionCreate.getElements().put(element.getId(), element);
        }
        instantiationProvider
            .createAutomationComposition(acmDefinition.getCompositionId(), automationCompositionCreate);
    }

    @Test
    void testOrderParticipantReport() {
        participantProvider.saveParticipant(inputParticipants.get(0));
        var participantId = participantProvider.getParticipants().get(0).getParticipantId();
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT
            + "/"
            + participantId);
        try (var response = invocationBuilder.header("Content-Length", 0)
            .put(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testBadOrderParticipantReport() {
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT
            + "/"
            + UUID.randomUUID());
        try (var response = invocationBuilder.header("Content-Length", 0)
            .put(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testOrderAllParticipantReport() {
        inputParticipants.forEach(participantProvider::saveParticipant);
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT);
        try (var response = invocationBuilder.header("Content-Length", 0)
            .put(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testRestartParticipants() {
        inputParticipants.forEach(participantProvider::saveParticipant);
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT + "/sync");
        try (var response = invocationBuilder.header("Content-Length", 0)
                .put(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testRestartParticipantById() {
        inputParticipants.forEach(participantProvider::saveParticipant);
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT + "/sync/"
            + "82fd8ef9-1d1e-4343-9b28-7f9564ee3de6");
        try (var response = invocationBuilder.header("Content-Length", 0)
                .put(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testRestartParticipantFailure() {
        inputParticipants.forEach(participantProvider::saveParticipant);
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT + "/sync/"
                + UUID.randomUUID());
        try (var response = invocationBuilder.header("Content-Length", 0)
                .put(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }
}
