/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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
 * Modifications copyright (c) 2019-2020 AT&T
 * ===================================================================
 *
 */

package org.onap.clamp.clds.sdc.controller.installer;

import java.util.Objects;

public class BlueprintMicroService {
    private final String name;
    private final String modelType;
    private final String inputFrom;
    private final String modelVersion;

    /**
     * The Micro service constructor.
     * 
     * @param name      The name in String
     * @param modelType The model type
     * @param inputFrom Comes from (single chained)
     */
    public BlueprintMicroService(String name, String modelType, String inputFrom, String modelVersion) {
        this.name = name;
        this.inputFrom = inputFrom;
        this.modelType = modelType;
        this.modelVersion = modelVersion;
    }

    public String getName() {
        return name;
    }

    public String getModelType() {
        return modelType;
    }

    public String getInputFrom() {
        return inputFrom;
    }

    /**
     * modelVerrsion getter.
     * 
     * @return the modelVersion
     */
    public String getModelVersion() {
        return modelVersion;
    }

    @Override
    public String toString() {
        return "MicroService {" + "name='" + name + '\'' + ", modelType='" + modelType + '\'' + ", inputFrom='"
                + inputFrom + '\'' + ", modelVersion='" + modelVersion + '\'' + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BlueprintMicroService that = (BlueprintMicroService) obj;
        return name.equals(that.name) && modelType.equals(that.modelType) && inputFrom.equals(that.inputFrom)
                && modelVersion.equals(that.modelVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, modelType, inputFrom, modelVersion);
    }
}
