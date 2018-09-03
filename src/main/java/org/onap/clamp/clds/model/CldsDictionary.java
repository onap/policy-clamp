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

import java.util.ArrayList;
import java.util.List;

import org.onap.clamp.clds.dao.CldsDao;

/**
 * Represents a CLDS Dictionary.
 */
@JsonInclude(Include.NON_NULL)
public class CldsDictionary {

    private String dictionaryId;
    private String dictionaryName;
    private String createdBy;
    private String updatedBy;
    private String lastUpdatedDate;
    private List<CldsDictionaryItem> cldsDictionaryItems = new ArrayList<>();

    /**
     * Creates or updates dictionary item for a dictionary in DB
     *
     * @param dictionaryName
     * @param cldsDao
     * @param userId
     */
    public void save(String dictionaryName, CldsDao cldsDao, String userId) {
        List<CldsDictionary> list = cldsDao.getDictionary(this.getDictionaryId(), dictionaryName);
        if (list != null && !list.isEmpty()) {
            CldsDictionary cldsDictionary = list.stream().findFirst().get();
            if (!cldsDictionary.getDictionaryName().equalsIgnoreCase(this.getDictionaryName())) {
                cldsDao.updateDictionary(cldsDictionary.getDictionaryId(), this, userId);
                this.setCreatedBy(cldsDictionary.getCreatedBy());
            } else {
                this.setDictionaryId(cldsDictionary.getDictionaryId());
                this.setCreatedBy(cldsDictionary.getCreatedBy());
                this.setUpdatedBy(cldsDictionary.getUpdatedBy());
                this.setLastUpdatedDate(cldsDictionary.getLastUpdatedDate());
            }
        } else {
            this.setCreatedBy(userId);
            this.setUpdatedBy(userId);
            cldsDao.insDictionary(this);
        }
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
     * @return the dictionaryName
     */
    public String getDictionaryName() {
        return dictionaryName;
    }

    /**
     * @param dictionaryName
     *        the dictionaryName to set
     */
    public void setDictionaryName(String dictionaryName) {
        this.dictionaryName = dictionaryName;
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

    /**
     * @return the cldsDictionaryItems
     */
    public List<CldsDictionaryItem> getCldsDictionaryItems() {
        return cldsDictionaryItems;
    }

    /**
     * @param cldsDictionaryItems
     *        the cldsDictionaryItems to set
     */
    public void setCldsDictionaryItems(List<CldsDictionaryItem> cldsDictionaryItems) {
        this.cldsDictionaryItems = cldsDictionaryItems;
    }

}
