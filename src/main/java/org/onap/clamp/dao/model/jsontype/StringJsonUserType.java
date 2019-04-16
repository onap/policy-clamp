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

package org.onap.clamp.dao.model.jsontype;

import com.google.gson.JsonObject;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

public class StringJsonUserType extends AbstractSingleColumnStandardBasicType<JsonObject> {

    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = -7929809808079327767L;

    public StringJsonUserType() {
        super(JsonStringSqlTypeDescriptor.INSTANCE, JsonTypeDescriptor.INSTANCE);
    }

    public StringJsonUserType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<JsonObject> javaTypeDescriptor) {
        super(sqlTypeDescriptor, javaTypeDescriptor);
    }

    @Override
    public String getName() {
        return "json";
    }

}
