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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;

class AcTypeStateResolverTest {

    @Test
    void testAcTypeState() {
        var acTypeStateResolver = new AcTypeStateResolver();
        var result = acTypeStateResolver.resolve(PrimeOrder.PRIME, AcTypeState.COMMISSIONED);
        assertThat(result).isEqualTo(PrimeOrder.PRIME);
        result = acTypeStateResolver.resolve(PrimeOrder.PRIME, AcTypeState.PRIMED);
        assertThat(result).isEqualTo(PrimeOrder.PRIME);
        result = acTypeStateResolver.resolve(PrimeOrder.PRIME, AcTypeState.PRIMING);
        assertThat(result).isEqualTo(PrimeOrder.NONE);
        result = acTypeStateResolver.resolve(PrimeOrder.PRIME, AcTypeState.DEPRIMING);
        assertThat(result).isEqualTo(PrimeOrder.NONE);

        result = acTypeStateResolver.resolve(PrimeOrder.DEPRIME, AcTypeState.COMMISSIONED);
        assertThat(result).isEqualTo(PrimeOrder.DEPRIME);
        result = acTypeStateResolver.resolve(PrimeOrder.DEPRIME, AcTypeState.PRIMED);
        assertThat(result).isEqualTo(PrimeOrder.DEPRIME);
        result = acTypeStateResolver.resolve(PrimeOrder.DEPRIME, AcTypeState.PRIMING);
        assertThat(result).isEqualTo(PrimeOrder.NONE);
        result = acTypeStateResolver.resolve(PrimeOrder.DEPRIME, AcTypeState.DEPRIMING);
        assertThat(result).isEqualTo(PrimeOrder.NONE);

        result = acTypeStateResolver.resolve(null, null);
        assertThat(result).isEqualTo(PrimeOrder.NONE);
    }
}
