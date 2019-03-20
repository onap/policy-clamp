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

import java.util.List;

import org.onap.clamp.clds.dao.CldsDao;

/**
 * Represents a CLDS Dictionary Item.
 */
public class CldsDictionaryItem {

    private String dictElementId;
    private String dictionaryId;
    private String dictElementName;
    private String dictElementShortName;
    private String dictElementDesc;
    private String dictElementType;
    private String createdBy;
    private String updatedBy;
    private String lastUpdatedDate;

    /**
     * Save the dictionary item.
     * @param dictionaryName The name of the dictionary
     * @param cldsDao The cldsDao
     * @param userId The user id
     */
    public void save(String dictionaryName, CldsDao cldsDao, String userId) {
        // Check if dictionary exists.
        List<CldsDictionary> list = cldsDao.getDictionary(this.getDictionaryId(), dictionaryName);
        if (list != null && !list.isEmpty()) {
            // Dictionary found. We can add or update the dictionary element
            CldsDictionary cldsDictionary = list.stream().findFirst().get();
            List<CldsDictionaryItem> dictionaryItems = cldsDao.getDictionaryElements(dictionaryName,
                cldsDictionary.getDictionaryId(), this.getDictElementShortName());
            if (dictionaryItems != null && !dictionaryItems.isEmpty()) {
                CldsDictionaryItem item = dictionaryItems.stream().findFirst().get();
                cldsDao.updateDictionaryElements(item.getDictElementId(), this, userId);
                this.setCreatedBy(item.getCreatedBy());

            } else {
                this.setCreatedBy(userId);
                this.setUpdatedBy(userId);
                cldsDao.insDictionarElements(this, userId);
            }
        }
    }

    /**
     * Get the dictionary element id.
     * @return the dictElementId
     */
    public String getDictElementId() {
        return dictElementId;
    }

    /**
     * Set the dictionary element id.
     * @param dictElementId
     *        the dictElementId to set
     */
    public void setDictElementId(String dictElementId) {
        this.dictElementId = dictElementId;
    }

    /**
     * Get the dictionary id.
     * @return the dictionaryId
     */
    public String getDictionaryId() {
        return dictionaryId;
    }

    /**
     * Set the dictionary id.
     * @param dictionaryId
     *        the dictionaryId to set
     */
    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    /**
     * Get the dictionary name.
     * @return the dictElementName
     */
    public String getDictElementName() {
        return dictElementName;
    }

    /**
     * Set the dictionary name.
     * @param dictElementName
     *        the dictElementName to set
     */
    public void setDictElementName(String dictElementName) {
        this.dictElementName = dictElementName;
    }

    /**
     * Get the dictionary element short name.
     * @return the dictElementShortName
     */
    public String getDictElementShortName() {
        return dictElementShortName;
    }

    /**
     * Set the dictionary element short name.
     * @param dictElementShortName
     *        the dictElementShortName to set
     */
    public void setDictElementShortName(String dictElementShortName) {
        this.dictElementShortName = dictElementShortName;
    }

    /**
     * Get the dictionary element description.
     * @return the dictElementDesc
     */
    public String getDictElementDesc() {
        return dictElementDesc;
    }

    /**
     * Set the dictionary element description.
     * @param dictElementDesc
     *        the dictElementDesc to set
     */
    public void setDictElementDesc(String dictElementDesc) {
        this.dictElementDesc = dictElementDesc;
    }

    /**
     * Get the dictionary element type.
     * @return the dictElementType
     */
    public String getDictElementType() {
        return dictElementType;
    }

    /**
     * Set the dictionary element type.
     * @param dictElementType
     *        the dictElementType to set
     */
    public void setDictElementType(String dictElementType) {
        this.dictElementType = dictElementType;
    }

    /**
     * Get the createdBy info.
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Set the createdBy info.
     * @param createdBy
     *        the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Get the updatedBy info.
     * @return the updatedBy
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Set the updatedBy info.
     * @param updatedby
     *        the updatedBy to set
     */
    public void setUpdatedBy(String updatedby) {
        updatedBy = updatedby;
    }

    /**
     * Get the last updated date.
     * @return the lastUpdatedDate
     */
    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    /**
     * Set the last updated date.
     * @param lastUpdatedDate
     *        the lastUpdatedDate to set
     */
    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

}
