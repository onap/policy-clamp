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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.handler.DummyAcElementListener;
import org.onap.policy.models.base.PfModelException;

class AutomationCompositionElementListenerTest {

    @Test
    void defaultTest() throws PfModelException {
        var listener = new DummyAcElementListener();
        assertThat(listener.getStatusProperties(UUID.randomUUID(), UUID.randomUUID())).isNotNull().isEmpty();
        assertThat(listener.getOperationalState(UUID.randomUUID(), UUID.randomUUID())).isNotNull().isEmpty();
        assertThat(listener.getUseState(UUID.randomUUID(), UUID.randomUUID())).isNotNull().isEmpty();
        assertThatCode(() -> listener.lock(UUID.randomUUID(), UUID.randomUUID())).doesNotThrowAnyException();
        assertThatCode(() -> listener.unlock(UUID.randomUUID(), UUID.randomUUID())).doesNotThrowAnyException();
    }
}
