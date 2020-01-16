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

package org.onap.clamp.loop.common;

import com.google.gson.annotations.Expose;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * This class is the parent of the hibernate entities requiring to be audited.
 *
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class AuditEntity {

    @Expose
    @CreatedDate
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private Instant createdDate;

    @Expose
    @LastModifiedDate
    @Column(name = "updated_timestamp", nullable = false)
    private Instant updatedDate;

    @Expose
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Expose
    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * createdDate setter.
     * 
     * @param createdDate The created Date object
     */
    public void setCreatedDate(Instant createdDate) {
        if (createdDate != null) {
            this.createdDate = createdDate.truncatedTo(ChronoUnit.SECONDS);
        } else {
            this.createdDate = null;
        }
    }

    /**
     * updatedDate getter.
     * 
     * @return the updatedDate
     */
    public Instant getUpdatedDate() {
        return updatedDate;
    }

    /**
     * updatedDate setter.
     * 
     * @param updatedDate updatedDate to set
     */
    public void setUpdatedDate(Instant updatedDate) {
        if (updatedDate != null) {
            this.updatedDate = updatedDate.truncatedTo(ChronoUnit.SECONDS);
        } else {
            this.updatedDate = null;
        }
    }

    /**
     * updatedBy getter.
     * 
     * @return the updatedBy
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * updatedBy setter.
     * 
     * @param updatedBy the updatedBy
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * createdBy getter.
     * 
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * createdBy setter.
     * 
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Empty constructor.
     */
    public AuditEntity() {
    }

}
