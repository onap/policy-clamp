/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CldsDbServiceCacheTest {

    @Test
    public void testConstructor() throws IOException, ClassNotFoundException {
        CldsServiceData cldsServiceData = new CldsServiceData();
        cldsServiceData.setServiceUUID("testUUID");
        cldsServiceData.setAgeOfRecord(Long.valueOf(100));
        cldsServiceData.setServiceInvariantUUID("testInvariantUUID");
        CldsVfData cldsVfData = new CldsVfData();
        cldsVfData.setVfName("vf");
        CldsVfKPIData cldsVfKpiData = new CldsVfKPIData();
        cldsVfKpiData.setFieldPath("fieldPath");
        cldsVfKpiData.setFieldPathValue("fieldValue");
        List<CldsVfKPIData> cldsKpiList = new ArrayList<>();
        cldsKpiList.add(cldsVfKpiData);
        cldsVfData.setCldsKPIList(cldsKpiList);
        List<CldsVfData> cldsVfs = new ArrayList<>();
        cldsVfs.add(cldsVfData);
        cldsServiceData.setCldsVfs(cldsVfs);
        CldsDbServiceCache cldsDbServiceCache = new CldsDbServiceCache(cldsServiceData);
        ObjectInputStream reader = new ObjectInputStream(cldsDbServiceCache.getCldsDataInstream());
        CldsServiceData cldsServiceDataResult = (CldsServiceData) reader.readObject();
        assertNotNull(cldsServiceDataResult);
        assertNotNull(cldsServiceDataResult.getCldsVfs());
        assertEquals(cldsServiceDataResult.getCldsVfs().size(), 1);
        assertNotNull(cldsServiceDataResult.getCldsVfs().get(0).getCldsKPIList());
        assertEquals(cldsServiceDataResult.getCldsVfs().get(0).getCldsKPIList().size(), 1);
        assertEquals(cldsServiceDataResult.getServiceInvariantUUID(), "testInvariantUUID");
        assertEquals(cldsServiceDataResult.getServiceUUID(), "testUUID");
        assertEquals(cldsServiceDataResult.getAgeOfRecord(), Long.valueOf(100L));
    }
}
