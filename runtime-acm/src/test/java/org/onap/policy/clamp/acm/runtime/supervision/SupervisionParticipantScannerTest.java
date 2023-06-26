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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.base.PfModelException;

class SupervisionParticipantScannerTest {

    @Test
    void testScanParticipant() throws PfModelException {
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanParticipant");
        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);

        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        participant.setParticipantState(ParticipantState.OFF_LINE);
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getParticipants()).thenReturn(List.of(participant));

        var supervisionScanner = new SupervisionPartecipantScanner(participantProvider, acRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getParticipantId());
        supervisionScanner.run();
        verify(participantProvider, times(0)).saveParticipant(any());

        supervisionScanner.run();
        verify(participantProvider, times(1)).updateParticipant(any());
    }
}
