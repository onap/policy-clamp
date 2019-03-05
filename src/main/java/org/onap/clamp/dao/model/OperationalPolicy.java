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

package org.onap.clamp.dao.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.onap.clamp.dao.model.jsontype.StringJsonUserType;

@Entity
@Table(name = "operational_policies")
@TypeDefs({ @TypeDef(name = "json", typeClass = StringJsonUserType.class) })
public class OperationalPolicy implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6117076450841538255L;

    @Expose
    @Id
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "configurations_json")
    private JsonObject configurationsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loop_id", nullable = false)
    private Loop loop;

    public Loop getLoop() {
        return loop;
    }

    public void setLoop(Loop loop) {
        this.loop = loop;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonObject getConfigurationsJson() {
        return configurationsJson;
    }

    public void setConfigurationsJson(JsonObject configurationsJson) {
        this.configurationsJson = configurationsJson;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OperationalPolicy other = (OperationalPolicy) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
