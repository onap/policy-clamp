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

import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.clamp.models.acm.utils.StateDefinition;
import org.springframework.stereotype.Component;

@Component
public class AcTypeStateResolver {

    private final StateDefinition<PrimeOrder> graph;

    private static final String PRIME = PrimeOrder.PRIME.toString();
    private static final String DEPRIME = PrimeOrder.DEPRIME.toString();
    private static final String PRIMED = AcTypeState.PRIMED.toString();
    private static final String COMMISSIONED = AcTypeState.COMMISSIONED.toString();

    /**
     * Construct.
     */
    public AcTypeStateResolver() {
        this.graph = new StateDefinition<>(2, PrimeOrder.NONE);
        this.graph.put(new String[] {PRIME, PRIMED}, PrimeOrder.PRIME);
        this.graph.put(new String[] {PRIME, COMMISSIONED}, PrimeOrder.PRIME);
        this.graph.put(new String[] {DEPRIME, PRIMED}, PrimeOrder.DEPRIME);
        this.graph.put(new String[] {DEPRIME, COMMISSIONED}, PrimeOrder.DEPRIME);
    }

    /**
     * Check if Prime Order is consistent with current AcTypeState.
     *
     * @param primeOrder the PrimeOrder
     * @param acTypeState then current AcTypeState
     * @return primeOrder or NONE if the primeOrder is not consistent
     */
    public PrimeOrder resolve(PrimeOrder primeOrder, AcTypeState acTypeState) {
        var po = primeOrder != null ? primeOrder : PrimeOrder.NONE;
        var state = acTypeState != null ? acTypeState : AcTypeState.COMMISSIONED;
        return this.graph.get(new String[] {po.toString(), state.toString()});
    }
}
