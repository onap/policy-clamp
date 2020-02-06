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

package org.onap.clamp.clds.tosca.update;

import com.google.gson.JsonArray;
import java.util.ArrayList;

public class ArrayField {

    private ArrayList<Object> complexFields;

    /**
     * Constructor from arraryList.
     *
     * @param arrayProperties the array properties
     */
    public ArrayField(ArrayList<Object> arrayProperties) {
        this.complexFields = arrayProperties;
    }

    /**
     * Each LinkedHashMap is parsed to extract the Array and each of its value. They are casted for the JsonObject.
     *
     * @return JsonArray
     */
    public JsonArray deploy() {

        JsonArray subPropertyValuesArray = new JsonArray();
        for (Object arrayElement : complexFields) {
            //Cast for each Primitive Type
            String typeValue = arrayElement.getClass().getSimpleName();
            switch (typeValue) {
                case "String":
                    subPropertyValuesArray.add((String) arrayElement);
                    break;
                case "Boolean":
                    subPropertyValuesArray.add((Boolean) arrayElement);
                    break;
                case "Double":
                    subPropertyValuesArray.add((Number) arrayElement);
                    break;
                case "Integer":
                    subPropertyValuesArray.add((Number) arrayElement);
                    break;
                default:
                    break;
            }
        }
        return subPropertyValuesArray;
    }
}
