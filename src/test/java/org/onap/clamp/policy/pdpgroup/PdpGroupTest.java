/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.policy.pdpgroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class PdpGroupTest {


    @Test
    public void testGetSupportedSubgroups() throws IOException {
        PdpGroup pdpGroup1 = new PdpGroup();
        pdpGroup1.setName("pdpGroup1");
        pdpGroup1.setPdpGroupState("INACTIVE");
        assertThat(pdpGroup1.getSupportedSubgroups("test", "1.0.0")).isNull();

        PdpGroup pdpGroup2 = new PdpGroup();
        pdpGroup2.setName("pdpGroup2");
        pdpGroup2.setPdpGroupState("ACTIVE");

        PolicyModelKey type1 = new PolicyModelKey("type1", "1.0.0");
        PolicyModelKey type2 = new PolicyModelKey("type2", "2.0.0");

        PdpSubgroup pdpSubgroup1 = new PdpSubgroup();
        pdpSubgroup1.setPdpType("subGroup1");
        List<PolicyModelKey> pdpTypeList = new LinkedList<PolicyModelKey>();
        pdpTypeList.add(type1);
        pdpTypeList.add(type2);
        pdpSubgroup1.setSupportedPolicyTypes(pdpTypeList);

        PolicyModelKey type3 = new PolicyModelKey("type3", "1.0.0");
        PdpSubgroup pdpSubgroup2 = new PdpSubgroup();
        pdpSubgroup2.setPdpType("subGroup2");
        List<PolicyModelKey> pdpTypeList2 = new LinkedList<PolicyModelKey>();
        pdpTypeList2.add(type2);
        pdpTypeList2.add(type3);
        pdpSubgroup2.setSupportedPolicyTypes(pdpTypeList2);

        List<PdpSubgroup> pdpSubgroupList = new LinkedList<PdpSubgroup>();
        pdpSubgroupList.add(pdpSubgroup1);
        pdpSubgroupList.add(pdpSubgroup2);
        pdpGroup2.setPdpSubgroups(pdpSubgroupList);

        JsonObject res1 = pdpGroup2.getSupportedSubgroups("type2", "2.0.0");
        assertThat(res1.get("pdpGroup2")).isNotNull();
        JsonArray resSubList = res1.getAsJsonArray("pdpGroup2");
        assertThat(resSubList.size()).isEqualTo(2);
        assertThat(resSubList.toString().contains("subGroup1")).isTrue();
        assertThat(resSubList.toString().contains("subGroup2")).isTrue();

        JsonObject res2 = pdpGroup2.getSupportedSubgroups("type1", "1.0.0");
        assertThat(res2.get("pdpGroup2")).isNotNull();
        JsonArray resSubList2 = res2.getAsJsonArray("pdpGroup2");
        assertThat(resSubList2.size()).isEqualTo(1);

        assertThat(pdpGroup2.getSupportedSubgroups("type3", "1.0.1")).isNull();
    }
}
