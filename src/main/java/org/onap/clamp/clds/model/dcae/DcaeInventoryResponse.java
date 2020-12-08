
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

import com.google.gson.annotations.Expose;

/**
 * This class maps the DCAE inventory answer to a nice pojo.
 */
public class DcaeInventoryResponse implements Comparable<DcaeInventoryResponse> {

    @Expose
    private String typeName;

    @Expose
    private String typeId;

    @Expose
    private String blueprintTemplate;

    /**
     * This field will be used to know all blueprints associated a loop.
     */
    @Expose
    private String asdcServiceId;

    /**
     * This field will be used to know to order of each blueprint microservice in a
     * loop.
     */
    @Expose
    private String asdcResourceId;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getBlueprintTemplate() {
        return blueprintTemplate;
    }

    public void setBlueprintTemplate(String blueprintTemplate) {
        this.blueprintTemplate = blueprintTemplate;
    }

    public String getAsdcServiceId() {
        return asdcServiceId;
    }

    public void setAsdcServiceId(String asdcServiceId) {
        this.asdcServiceId = asdcServiceId;
    }

    public String getAsdcResourceId() {
        return asdcResourceId;
    }

    public void setAsdcResourceId(String asdcResourceId) {
        this.asdcResourceId = asdcResourceId;
    }

    @Override
    public int compareTo(DcaeInventoryResponse otherResponse) {
        int thisResourceId = Integer.parseInt(this.asdcResourceId);
        int otherResourceId = Integer.parseInt(otherResponse.getAsdcResourceId());
        return (thisResourceId < otherResourceId ? -1 : (thisResourceId > otherResourceId ? 1 : 0));
    }
}
