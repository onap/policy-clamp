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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.model;

import javax.ws.rs.NotFoundException;

import org.onap.clamp.clds.dao.CldsDao;

/**
 * Represent a CLDS Model.
 */
public class CldsTemplate {

    public static final String STATUS_DESIGN   = "DESIGN";
    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_STOPPED  = "STOPPED";
    public static final String STATUS_DELETING = "DELETING";
    public static final String STATUS_ERROR    = "ERROR";   // manual
                                                            // intervention
                                                            // required
    public static final String STATUS_UNKNOWN  = "UNKNOWN";

    private String             id;
    private String             name;
    private String             controlNamePrefix;
    private String             controlNameUuid;
    private String             bpmnId;
    private String             bpmnUserid;
    private String             bpmnText;
    private String             imageId;
    private String             imageUserid;
    private String             imageText;
    private String             propId;
    private String             propUserid;
    private String             propText;

    private boolean            userAuthorizedToUpdate;

    /**
     * Save template to DB.
     *
     * @param cldsDao
     * @param userid
     */
    public void save(CldsDao cldsDao, String userid) {
        cldsDao.setTemplate(this, userid);
    }

    /**
     * Retrieve from DB.
     *
     * @param cldsDao
     * @param name
     * @return
     */
    public static CldsTemplate retrieve(CldsDao cldsDao, String name, boolean okIfNotFound) {
        // get from db
        CldsTemplate template = cldsDao.getTemplate(name);
        if (template.getId() == null && !okIfNotFound) {
            throw new NotFoundException();
        }
        return template;
    }

    public String getBpmnUserid() {
        return bpmnUserid;
    }

    public void setBpmnUserid(String bpmnUserid) {
        this.bpmnUserid = bpmnUserid;
    }

    public String getBpmnText() {
        return bpmnText;
    }

    public void setBpmnText(String bpmnText) {
        this.bpmnText = bpmnText;
    }

    public String getImageUserid() {
        return imageUserid;
    }

    public void setImageUserid(String imageUserid) {
        this.imageUserid = imageUserid;
    }

    public String getImageText() {
        return imageText;
    }

    public void setImageText(String imageText) {
        this.imageText = imageText;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getControlNamePrefix() {
        return controlNamePrefix;
    }

    public void setControlNamePrefix(String controlNamePrefix) {
        this.controlNamePrefix = controlNamePrefix;
    }

    public String getControlNameUuid() {
        return controlNameUuid;
    }

    public void setControlNameUuid(String controlNameUuid) {
        this.controlNameUuid = controlNameUuid;
    }

    public String getPropId() {
        return propId;
    }

    public void setPropId(String propId) {
        this.propId = propId;
    }

    public String getPropUserid() {
        return propUserid;
    }

    public void setPropUserid(String propUserid) {
        this.propUserid = propUserid;
    }

    public String getPropText() {
        return propText;
    }

    public void setPropText(String propText) {
        this.propText = propText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBpmnId() {
        return bpmnId;
    }

    public void setBpmnId(String bpmnId) {
        this.bpmnId = bpmnId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public boolean isUserAuthorizedToUpdate() {
        return userAuthorizedToUpdate;
    }

    public void setUserAuthorizedToUpdate(boolean userAuthorizedToUpdate) {
        this.userAuthorizedToUpdate = userAuthorizedToUpdate;
    }
}
