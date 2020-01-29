/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model.dcae;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class stores the multiple DcaeInventoryResponse coming back from DCAE.
 * The structure is a map of list indexed by asdcServiceId. The list is sorted
 * by asdcResourceId. Therefore it's possible to retrieve all the loops defined
 * in the DCAE inventory and created by DCAE Mod.
 */
public class DcaeInventoryCache {

    private static Map<String, Set<DcaeInventoryResponse>> blueprintsMap = new ConcurrentHashMap<>();

    /**
     * Add Dcae inventory response.
     * 
     * @param inventoryResponse the Dcae inventory response
     */
    public void addDcaeInventoryResponse(DcaeInventoryResponse inventoryResponse) {
        Set<DcaeInventoryResponse> responsesSet = blueprintsMap.get(inventoryResponse.getAsdcServiceId());
        if (responsesSet == null) {
            responsesSet = new TreeSet<DcaeInventoryResponse>();
            blueprintsMap.put(inventoryResponse.getAsdcServiceId(), responsesSet);
        }
        responsesSet.add(inventoryResponse);
    }

    public Set<String> getAllLoopIds() {
        return this.blueprintsMap.keySet();
    }

    public Set<DcaeInventoryResponse> getAllBlueprintsPerLoopId(String loopId) {
        return blueprintsMap.getOrDefault(loopId, new TreeSet<>());
    }
}
