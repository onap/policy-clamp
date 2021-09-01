/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.controlloop.runtime.supervision;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionAspectTest {

    @Test
    void testSchedule() throws Exception {
        var supervisionScanner = spy(mock(SupervisionScanner.class));
        try (var supervisionAspect = new SupervisionAspect(supervisionScanner)) {
            supervisionAspect.schedule();
            verify(supervisionScanner, timeout(500)).run(true);
        }
    }

    @Test
    void testDoCheck() throws Exception {
        var supervisionScanner = spy(mock(SupervisionScanner.class));
        try (var supervisionAspect = new SupervisionAspect(supervisionScanner)) {
            supervisionAspect.doCheck();
            supervisionAspect.doCheck();
            verify(supervisionScanner, timeout(500).times(2)).run(false);
        }
    }

    @Test
    void testHandleParticipantStatus() throws Exception {
        var supervisionScanner = spy(mock(SupervisionScanner.class));
        var participantStatusMessage = new ParticipantStatus();
        var identifier = new ToscaConceptIdentifier("abc", "1.0.0");
        participantStatusMessage.setParticipantId(identifier);

        try (var supervisionAspect = new SupervisionAspect(supervisionScanner)) {
            supervisionAspect.handleParticipantStatus(participantStatusMessage);
            verify(supervisionScanner, timeout(500)).handleParticipantStatus(identifier);
        }
    }
}
