/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaParticipant;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;

class ParticipantProviderTest {

    private static final Coder CODER = new StandardCoder();
    private static final String PARTICIPANT_JSON = "src/test/resources/providers/TestParticipant.json";
    private static final String LIST_IS_NULL = ".*. is marked .*ull but is null";

    private final List<Participant> inputParticipants = new ArrayList<>();
    private List<JpaParticipant> jpaParticipantList;
    private final String originalJson = ResourceUtils.getResourceAsString(PARTICIPANT_JSON);

    @BeforeEach
    void beforeSetupDao() throws Exception {
        inputParticipants.add(CODER.decode(originalJson, Participant.class));
        jpaParticipantList = ProviderUtils.getJpaAndValidateList(inputParticipants, JpaParticipant::new, "participant");
    }

    @Test
    void testParticipantSave() throws Exception {
        var participantRepository = mock(ParticipantRepository.class);
        for (var participant : jpaParticipantList) {
            when(participantRepository.getById(new PfConceptKey(participant.getName(), participant.getVersion())))
                    .thenReturn(participant);
        }
        var participantProvider = new ParticipantProvider(participantRepository);

        assertThatThrownBy(() -> participantProvider.saveParticipant(null))
            .hasMessageMatching(LIST_IS_NULL);

        when(participantRepository.save(any())).thenReturn(jpaParticipantList.get(0));

        Participant savedParticipant = participantProvider.saveParticipant(inputParticipants.get(0));
        assertEquals(savedParticipant, inputParticipants.get(0));

        when(participantRepository.save(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> participantProvider.saveParticipant(inputParticipants.get(0)))
            .hasMessageMatching("Error in save Participant");
    }

    @Test
    void testGetAutomationCompositions() throws Exception {
        var participantRepository = mock(ParticipantRepository.class);
        var participantProvider = new ParticipantProvider(participantRepository);

        // Return empty list when no data present in db
        List<Participant> getResponse = participantProvider.getParticipants(null, null);
        assertThat(getResponse).isEmpty();

        String name = inputParticipants.get(0).getName();
        String version = inputParticipants.get(0).getVersion();
        when(participantRepository.getFiltered(any(), eq(name), eq(version)))
                .thenReturn(List.of(jpaParticipantList.get(0)));
        assertEquals(1, participantProvider.getParticipants(name, version).size());

        assertThat(participantProvider.getParticipants("invalid_name", "1.0.1")).isEmpty();

        assertThat(participantProvider.findParticipant("invalid_name", "1.0.1")).isEmpty();

        when(participantRepository.findAll()).thenReturn(jpaParticipantList);
        assertThat(participantProvider.getParticipants()).hasSize(inputParticipants.size());

        when(participantRepository.findById(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> participantProvider.findParticipant("notValid", "notValid"))
            .hasMessageMatching("Error in find Participant");

        assertThatThrownBy(() -> participantProvider.getFilteredParticipants(null))
            .hasMessageMatching("filter is marked .*ull but is null");

        when(participantRepository.getFiltered((JpaParticipant.class), (null), (null)))
                .thenReturn(jpaParticipantList);

        final ToscaTypedEntityFilter<Participant> filter = ToscaTypedEntityFilter.<Participant>builder()
                .type("org.onap.domain.pmsh.PMSHAutomationCompositionDefinition").build();
        assertEquals(1, participantProvider.getFilteredParticipants(filter).size());

    }

    @Test
    void testDeleteParticipant() throws Exception {
        var participantRepository = mock(ParticipantRepository.class);
        var participantProvider = new ParticipantProvider(participantRepository);

        assertThatThrownBy(() -> participantProvider.deleteParticipant("Invalid_name", "1.0.1"))
            .hasMessageMatching(".*.failed, participant does not exist");

        String name = inputParticipants.get(0).getName();
        String version = inputParticipants.get(0).getVersion();

        when(participantRepository.findById(any())).thenReturn(Optional.of(jpaParticipantList.get(0)));

        Participant deletedParticipant = participantProvider.deleteParticipant(name, version);
        assertEquals(inputParticipants.get(0), deletedParticipant);

        when(participantRepository.findById(any())).thenThrow(IllegalArgumentException.class);
        assertThatThrownBy(() -> participantProvider.deleteParticipant(name, version))
            .hasMessageMatching("Error in delete Participant");
    }
}
