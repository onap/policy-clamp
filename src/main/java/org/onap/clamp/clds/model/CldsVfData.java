/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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

import java.io.Serializable;
import java.util.List;

public class CldsVfData implements Serializable {

    private static final long   serialVersionUID = -5069670140274203606L;
    private String              vfName;
    private String              vfInvariantResourceUuid;
    private List<CldsVfcData>   cldsVfcs;

    private List<CldsVfKPIData> cldsKpiList;

    public List<CldsVfKPIData> getCldsKPIList() {
        return cldsKpiList;
    }

    public void setCldsKPIList(List<CldsVfKPIData> cldsKpiList) {
        this.cldsKpiList = cldsKpiList;
    }

    public String getVfName() {
        return vfName;
    }

    public void setVfName(String vfName) {
        this.vfName = vfName;
    }

    public List<CldsVfcData> getCldsVfcs() {
        return cldsVfcs;
    }

    public void setCldsVfcs(List<CldsVfcData> cldsVfcs) {
        this.cldsVfcs = cldsVfcs;
    }

    public String getVfInvariantResourceUUID() {
        return vfInvariantResourceUuid;
    }

    public void setVfInvariantResourceUUID(String vfInvariantResourceUuid) {
        this.vfInvariantResourceUuid = vfInvariantResourceUuid;
    }

}
