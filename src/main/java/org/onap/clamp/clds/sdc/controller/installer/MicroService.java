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
 * Modifications copyright (c) 2019 AT&T
 * ===================================================================
 *
 */

package org.onap.clamp.clds.sdc.controller.installer;

import java.util.Objects;

public class MicroService {
    private final String name;
    private final String modelType;
    private final String inputFrom;
    private String mappedNameJpa;

    /**
     * The Micro service constructor.
     * 
     * @param name          The name in String
     * @param modelType     The model type
     * @param inputFrom     Comes from (single chained)
     * @param mappedNameJpa Name in database
     */
    public MicroService(String name, String modelType, String inputFrom, String mappedNameJpa) {
        this.name = name;
        this.inputFrom = inputFrom;
        this.mappedNameJpa = mappedNameJpa;
        this.modelType = modelType;
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

    @Override
    public String toString() {
        return "MicroService{" + "name='" + name + '\'' + ", modelType='" + modelType + '\'' + ", inputFrom='"
                + inputFrom + '\'' + ", mappedNameJpa='" + mappedNameJpa + '\'' + '}';
    }

    public String getMappedNameJpa() {
        return mappedNameJpa;
    }

    public void setMappedNameJpa(String mappedNameJpa) {
        this.mappedNameJpa = mappedNameJpa;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MicroService that = (MicroService) obj;
        return name.equals(that.name) && modelType.equals(that.modelType) && inputFrom.equals(that.inputFrom)
                && mappedNameJpa.equals(that.mappedNameJpa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, modelType, inputFrom, mappedNameJpa);
    }
}
