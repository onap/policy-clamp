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

package org.onap.clamp.loop.service;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.onap.clamp.dao.model.jsontype.StringJsonUserType;


@Entity
@Table(name = "services")
@TypeDefs({ @TypeDef(name = "json", typeClass = StringJsonUserType.class) })
public class Service implements Serializable {

    /**
     * The serial version id.
     */
    private static final long serialVersionUID = 1331119060272760758L;

    @Transient
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(Service.class);

    @Id
    @Column(name = "service_uuid", unique = true)
    private String serviceUuid;

    @Column(nullable = false, name = "name")
    private String name;

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "service_details")
    private JsonObject serviceDetails;

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "resource_details")
    private JsonObject resourceDetails;

    /**
     * Public constructor.
     */
    public Service() {
    }

    /**
     * Constructor.
     */
    public Service(JsonObject serviceDetails, JsonObject resourceDetails) {
        this.name = serviceDetails.get("name").getAsString();
        this.serviceUuid = serviceDetails.get("UUID").getAsString();
        this.serviceDetails = serviceDetails;
        this.resourceDetails = resourceDetails;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public JsonObject getServiceDetails() {
        return serviceDetails;
    }

    public JsonObject getResourceDetails() {
        return resourceDetails;
    }

    public JsonObject getResourceByType(String type) {
        return (JsonObject) resourceDetails.get(type);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((serviceUuid == null) ? 0 : serviceUuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Service other = (Service) obj;
        if (serviceUuid == null) {
            if (other.serviceUuid != null) {
                return false;
            }
        } else if (!serviceUuid.equals(other.serviceUuid)) {
            return false;
        }
        return true;
    }

}
