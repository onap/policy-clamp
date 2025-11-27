
/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.AcDefinitionScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.MonitoringScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.PhaseScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.SimpleScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.StageScanner;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;

/**
 * Extra tests to exercise additional branches in SupervisionScanner.
 */
class SupervisionScannerExtraTest {

    private AcDefinitionProvider acDefinitionProvider;
    private MessageProvider messageProvider;
    private MonitoringScanner monitoringScanner;
    private SupervisionScanner scanner;

    @BeforeEach
    void setUp() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        acDefinitionProvider = mock(AcDefinitionProvider.class);
        messageProvider = mock(MessageProvider.class);
        monitoringScanner = mock(MonitoringScanner.class,
            withSettings().useConstructor(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), mock(StageScanner.class), mock(SimpleScanner.class),
                mock(PhaseScanner.class), messageProvider));

        scanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
            messageProvider, monitoringScanner);
    }

    @Test
    void whenNoDefinitionsInTransition_thenRunDoesNothing() {
        when(acDefinitionProvider.getAllAcDefinitionsInTransition()).thenReturn(new HashSet<>());
        scanner.run();
        verify(messageProvider).removeOldJobs();
        verifyNoMoreInteractions(monitoringScanner);
    }

    @Test
    void whenJobCreationFails_scanAutomationCompositionReturnsEarly() {
        UUID compId = UUID.randomUUID();
        when(acDefinitionProvider.getAllAcDefinitionsInTransition()).thenReturn(new HashSet<>(Set.of(compId)));
        when(messageProvider.createJob(compId)).thenReturn(Optional.empty());

        scanner.run();

        // removeOldJobs is always called at the start
        verify(messageProvider).removeOldJobs();
        verify(messageProvider).createJob(compId);
        verify(messageProvider, never()).removeJob(any());
        verify(monitoringScanner, never()).scanAutomationComposition(any(), any());
    }

    @Test
    void whenJobCreated_thenMonitoringScannerCalled_and_jobRemoved() {
        UUID compId = UUID.randomUUID();
        when(messageProvider.getAllMessages(any(UUID.class))).thenReturn(new ArrayList<>());
        when(messageProvider.findCompositionMessages()).thenReturn(new HashSet<>(Set.of(compId)));
        doNothing().when(monitoringScanner).scanAcDefinition(any());
        when(acDefinitionProvider.getAllAcDefinitionsInTransition()).thenReturn(new HashSet<>(Set.of(compId)));
        when(messageProvider.findInstanceMessages()).thenReturn(new HashSet<>(Set.of(compId)));
        when(messageProvider.createJob(compId)).thenReturn(Optional.of(UUID.randomUUID().toString()));


        scanner.run();

        verify(messageProvider).removeOldJobs();
        verify(messageProvider, atLeastOnce()).createJob(compId);
        verify(monitoringScanner).scanAutomationComposition(eq(compId), anyMap());
        verify(messageProvider, atLeastOnce()).removeJob(any());
    }
}
