/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.common.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

public class ControlLoopHandlerTest {

    @Test
    public void testControlLoopHandler() {
        assertThatThrownBy(() -> new DummyControlLoopHandler(null)).isInstanceOf(NullPointerException.class);

        assertNotNull(new DummyControlLoopHandler(new PolicyModelsProviderParameters()));
        Registry.unregister(DummyControlLoopHandler.class.getName());

        String dummyClassName = DummyControlLoopHandler.class.getName();
        assertThatThrownBy(() -> Registry.get(dummyClassName)).isInstanceOf(IllegalArgumentException.class);

        PolicyModelsProviderParameters pars = new PolicyModelsProviderParameters();

        DummyControlLoopHandler dclh = new DummyControlLoopHandler(pars);
        assertNotNull(dclh);

        assertEquals(pars, dclh.getDatabaseProviderParameters());
        assertEquals(0, dclh.getProviderClasses().size());

        dclh.close();
        assertThatThrownBy(() -> Registry.get(dummyClassName)).isInstanceOf(IllegalArgumentException.class);
    }
}
