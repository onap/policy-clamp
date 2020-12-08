/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 Huawei Technologies Co., Ltd.
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
 * ============LICENSE_END=========================================================
 * ================================================================================
 *
 */

package org.onap.clamp.clds.model.cds;

import com.google.gson.annotations.Expose;

import java.util.LinkedList;
import java.util.List;

/**
 * This class maps the CDS response to a pojo.
 */
public class CdsBpWorkFlowListResponse {

    @Expose
    private String blueprintName;

    @Expose
    private String version;

    @Expose
    private List<String> workflows = new LinkedList<String>();

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<String> workflows) {
        this.workflows = workflows;
    }
}
