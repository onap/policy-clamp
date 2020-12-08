/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */

package org.onap.clamp.clds.sdc.controller.installer;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ChainGenerator {

    ChainGenerator() {
    }

    /**
     * Get list of microservices chain.
     * 
     * @param input A set of microservices
     * @return The list of microservice chained
     */
    public List<BlueprintMicroService> getChainOfMicroServices(Set<BlueprintMicroService> input) {
        LinkedList<BlueprintMicroService> returnList = new LinkedList<>();
        if (preValidate(input)) {
            LinkedList<BlueprintMicroService> theList = new LinkedList<>();
            for (BlueprintMicroService ms : input) {
                insertNodeTemplateIntoChain(ms, theList);
            }
            if (postValidate(theList)) {
                returnList = theList;
            }
        }
        return returnList;
    }

    private boolean preValidate(Set<BlueprintMicroService> input) {
        List<BlueprintMicroService> noInputs = input.stream().filter(ms -> "".equals(ms.getInputFrom()))
                .collect(Collectors.toList());
        return noInputs.size() == 1;
    }

    private boolean postValidate(LinkedList<BlueprintMicroService> microServices) {
        for (int i = 1; i < microServices.size() - 1; i++) {
            BlueprintMicroService prev = microServices.get(i - 1);
            BlueprintMicroService current = microServices.get(i);
            if (!current.getInputFrom().equals(prev.getName())) {
                return false;
            }
        }
        return true;
    }

    private void insertNodeTemplateIntoChain(BlueprintMicroService microServicetoInsert,
            LinkedList<BlueprintMicroService> chainOfMicroServices) {
        int insertIndex = 0;
        for (int i = 0; i < chainOfMicroServices.size(); i++) {
            BlueprintMicroService current = chainOfMicroServices.get(i);
            if (microServicetoInsert.getName().equals(current.getInputFrom())) {
                insertIndex = i;
                break;
            } else if (current.getName().equals(microServicetoInsert.getInputFrom())) {
                insertIndex = i + 1;
                break;
            }
        }
        chainOfMicroServices.add(insertIndex, microServicetoInsert);
    }
}
