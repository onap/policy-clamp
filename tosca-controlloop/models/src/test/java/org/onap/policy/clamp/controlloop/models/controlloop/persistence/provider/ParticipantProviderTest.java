/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;

public class ParticipantProviderTest {

    private static final Coder CODER = new StandardCoder();
    private static final String PARTICIPANT_JSON =
        "src/test/resources/providers/TestParticipant.json";
    private static final String LIST_IS_NULL = ".*. is marked .*ull but is null";

    private PolicyModelsProviderParameters parameters;
    private ParticipantProvider participantProvider;
    private List<Participant> inputParticipants = new ArrayList<>();
    private Participant updateParticipants;
    private String originalJson = ResourceUtils.getResourceAsString(PARTICIPANT_JSON);

    /**
     * Set up test Participant provider.
     */
    @Before
    public void setupDao() throws Exception {

        parameters = new PolicyModelsProviderParameters();
        parameters.setDatabaseDriver("org.h2.Driver");
        parameters.setName("PolicyProviderParameterGroup");
        parameters.setImplementation("org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl");
        parameters.setDatabaseUrl("jdbc:h2:mem:participantProviderTestDb");
        parameters.setDatabaseUser("policy");
        parameters.setDatabasePassword("P01icY");
        parameters.setPersistenceUnit("ToscaConceptTest");

        participantProvider = new ParticipantProvider(parameters);
        inputParticipants.add(CODER.decode(originalJson, Participant.class));

    }

    @After
    public void teardown() {
        participantProvider.close();
    }

    @Test
    public void testParticipantCreate() throws Exception {
        assertThatThrownBy(() -> {
            participantProvider.createParticipants(null);
        }).hasMessageMatching(LIST_IS_NULL);

        List<Participant> createdParticipants = new ArrayList<>();
        createdParticipants.addAll(participantProvider
            .createParticipants(inputParticipants));

        assertEquals(createdParticipants.get(0),
            inputParticipants.get(0));
    }


    @Test
    public void testGetControlLoops() throws Exception {

        List<Participant> getResponse;

        //Return empty list when no data present in db
        getResponse = participantProvider.getParticipants(null, null);
        assertThat(getResponse).isEmpty();

        participantProvider.createParticipants(inputParticipants);
        String name = inputParticipants.get(0).getName();
        String version = inputParticipants.get(0).getVersion();
        assertEquals(1, participantProvider.getParticipants(name, version).size());

        assertThat(participantProvider.getParticipants("invalid_name",
            "1.0.1")).isEmpty();

        assertThatThrownBy(() -> {
            participantProvider.getFilteredParticipants(null);
        }).hasMessageMatching("filter is marked .*ull but is null");

        final ToscaTypedEntityFilter<Participant> filter = ToscaTypedEntityFilter.<Participant>builder()
            .type("org.onap.domain.pmsh.PMSHControlLoopDefinition").build();
        assertEquals(1, participantProvider.getFilteredParticipants(filter).size());
    }

    @Test
    public void testUpdateParticipant() throws Exception {
        assertThatThrownBy(() -> {
            participantProvider.updateParticipants(null);
        }).hasMessageMatching("participants is marked .*ull but is null");

        participantProvider.createParticipants(inputParticipants);
        updateParticipants = inputParticipants.get(0);
        updateParticipants.setParticipantState(ParticipantState.ACTIVE);
        List<Participant> participantList = new ArrayList<>();
        participantList.add(updateParticipants);
        List<Participant> updateResponse = new ArrayList<>();
        updateResponse = participantProvider.updateParticipants(participantList);

        assertEquals(ParticipantState.ACTIVE, updateResponse.get(0).getParticipantState());
    }

    @Test
    public void testDeleteParticipant() throws Exception {
        assertThatThrownBy(() -> {
            participantProvider.deleteParticipant("Invalid_name", "1.0.1");
        }).hasMessageMatching(".*.failed, participant does not exist");

        Participant deletedParticipant;
        List<Participant> participantList = participantProvider.createParticipants(inputParticipants);
        String name = inputParticipants.get(0).getName();
        String version = inputParticipants.get(0).getVersion();

        deletedParticipant = participantProvider.deleteParticipant(name, version);
        assertEquals(participantList.get(0), deletedParticipant);

    }
}
