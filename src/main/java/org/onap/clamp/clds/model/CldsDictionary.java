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

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a CLDS Dictionary.
 */

public class CldsDictionary {

    @Expose
    private String dictionaryId;
    @Expose
    private String dictionaryName;

    @Expose
    private String createdBy;
    @Expose
    private String updatedBy;
    @Expose
    private String lastUpdatedDate;
    @Expose
    private List<CldsDictionaryItem> cldsDictionaryItems = new ArrayList<>();

    /**
     * Get the dictionary ID.
     * 
     * @return the dictionaryId
     */
    public String getDictionaryId() {
        return dictionaryId;
    }

    /**
     * Set the dictionary Id.
     * 
     * @param dictionaryId the dictionaryId to set
     */
    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    /**
     * Get the dictionary name.
     * 
     * @return the dictionaryName
     */
    public String getDictionaryName() {
        return dictionaryName;
    }

    /**
     * Set the dictionary name.
     * 
     * @param dictionaryName the dictionaryName to set
     */
    public void setDictionaryName(String dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    /**
     * Get the createdBy info.
     * 
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Set the createdBy info.
     * 
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Get the updatedBy info.
     * 
     * @return the updatedBy
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Set the updatedBy info.
     * 
     * @param updatedby the updatedBy to set
     */
    public void setUpdatedBy(String updatedby) {
        updatedBy = updatedby;
    }

    /**
     * Get the last updated date.
     * 
     * @return the lastUpdatedDate
     */
    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    /**
     * Set the last updated date.
     * 
     * @param lastUpdatedDate the lastUpdatedDate to set
     */
    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    /**
     * Get all the dictionary items.
     * 
     * @return the cldsDictionaryItems
     */
    public List<CldsDictionaryItem> getCldsDictionaryItems() {
        return cldsDictionaryItems;
    }

    /**
     * Set the whole dictionary items.
     * 
     * @param cldsDictionaryItems the cldsDictionaryItems to set
     */
    public void setCldsDictionaryItems(List<CldsDictionaryItem> cldsDictionaryItems) {
        this.cldsDictionaryItems = cldsDictionaryItems;
    }

}
