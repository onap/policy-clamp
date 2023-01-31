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

package org.onap.policy.clamp.acm.runtime.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.main.rest.ParticipantController;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
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

    @LocalServerPort
    private int randomServerPort;

    private static final Coder CODER = new StandardCoder();
    private static final String PARTICIPANT_JSON = "src/test/resources/providers/TestParticipant.json";
    private static final String PARTICIPANT_JSON2 = "src/test/resources/providers/TestParticipant2.json";

    private static final List<Participant> inputParticipants = new ArrayList<>();
    private static final String originalJson = ResourceUtils.getResourceAsString(PARTICIPANT_JSON);
    private static final String originalJson2 = ResourceUtils.getResourceAsString(PARTICIPANT_JSON2);

    @Autowired
    private ParticipantProvider participantProvider;

    /**
     * Adds participants to the db from json file.
     */
    @BeforeAll
    public static void setUpBeforeClass() throws CoderException {
        inputParticipants.add(CODER.decode(originalJson, Participant.class));
        inputParticipants.add(CODER.decode(originalJson2, Participant.class));
    }

    @BeforeEach
    public void setUpPort() {
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
        participantProvider.saveParticipant(inputParticipants.get(0));
        UUID participantId = participantProvider.getParticipants().get(0).getParticipantId();
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT + "/" + participantId);
        var response = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        var entityList = response.readEntity(ParticipantInformation.class);
        assertNotNull(entityList);
    }

    @Test
    void testBadQueryParticipant() {
        participantProvider.saveParticipant(inputParticipants.get(0));
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT + "/" + UUID.randomUUID());
        var response = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void getAllParticipants() {
        inputParticipants.forEach(p -> {
            participantProvider.saveParticipant(p);
        });
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT);
        var response = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ParticipantInformation> entityList = response.readEntity(new GenericType<>() {});
        assertThat(entityList.size() == inputParticipants.size());
    }

    @Test
    void testOrderParticipantReport() throws PfModelException {
        participantProvider.saveParticipant(inputParticipants.get(0));
        UUID participantId = participantProvider.getParticipants().get(0).getParticipantId();
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT
            + "/"
            + participantId);
        var response = invocationBuilder.header("Content-Length", 0).put(Entity.entity(""
            +
            "", MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }

    @Test
    void testBadOrderParticipantReport() throws PfModelException {
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT
            + "/"
            + UUID.randomUUID());
        var response = invocationBuilder.header("Content-Length", 0).put(Entity.entity(""
            +
            "", MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testOrderAllParticipantReport() {
        inputParticipants.forEach(p -> {
            participantProvider.saveParticipant(p);
        });
        var invocationBuilder = super.sendRequest(PARTICIPANTS_ENDPOINT);
        var response = invocationBuilder.header("Content-Length", 0).put(Entity.entity(""
            +
            "", MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }
}
