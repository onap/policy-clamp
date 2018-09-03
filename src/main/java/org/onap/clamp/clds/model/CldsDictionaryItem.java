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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.List;

import org.onap.clamp.clds.dao.CldsDao;

/**
 * Represents a CLDS Dictionary Item.
 */
@JsonInclude(Include.NON_NULL)
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
     * @return the dictElementId
     */
    public String getDictElementId() {
        return dictElementId;
    }

    /**
     * @param dictElementId
     *        the dictElementId to set
     */
    public void setDictElementId(String dictElementId) {
        this.dictElementId = dictElementId;
    }

    /**
     * @return the dictionaryId
     */
    public String getDictionaryId() {
        return dictionaryId;
    }

    /**
     * @param dictionaryId
     *        the dictionaryId to set
     */
    public void setDictionaryId(String dictionaryId) {
        this.dictionaryId = dictionaryId;
    }

    /**
     * @return the dictElementName
     */
    public String getDictElementName() {
        return dictElementName;
    }

    /**
     * @param dictElementName
     *        the dictElementName to set
     */
    public void setDictElementName(String dictElementName) {
        this.dictElementName = dictElementName;
    }

    /**
     * @return the dictElementShortName
     */
    public String getDictElementShortName() {
        return dictElementShortName;
    }

    /**
     * @param dictElementShortName
     *        the dictElementShortName to set
     */
    public void setDictElementShortName(String dictElementShortName) {
        this.dictElementShortName = dictElementShortName;
    }

    /**
     * @return the dictElementDesc
     */
    public String getDictElementDesc() {
        return dictElementDesc;
    }

    /**
     * @param dictElementDesc
     *        the dictElementDesc to set
     */
    public void setDictElementDesc(String dictElementDesc) {
        this.dictElementDesc = dictElementDesc;
    }

    /**
     * @return the dictElementType
     */
    public String getDictElementType() {
        return dictElementType;
    }

    /**
     * @param dictElementType
     *        the dictElementType to set
     */
    public void setDictElementType(String dictElementType) {
        this.dictElementType = dictElementType;
    }

    /**
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy
     *        the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the updatedBy
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * @param updatedby
     *        the updatedBy to set
     */
    public void setUpdatedBy(String updatedby) {
        updatedBy = updatedby;
    }

    /**
     * @return the lastUpdatedDate
     */
    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    /**
     * @param lastUpdatedDate
     *        the lastUpdatedDate to set
     */
    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

}
