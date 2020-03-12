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

package org.onap.clamp.clds.tosca.update.elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ToscaElement {

    /**
     * name parameter is used as "key", in the LinkedHashMap of Components.
     */
    private String name;
    private String derivedFrom;
    private String version;
    private String typeVersion;
    private String description;
    private LinkedHashMap<String, ToscaElementProperty> properties;

    public ToscaElement() {
    }

    /**
     * Constructor.
     *
     * @param name name
     * @param derivedFrom derivedFrom
     * @param description description
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ToscaElement(String name, String derivedFrom, String description) {
        super();
        this.name = name;
        this.derivedFrom = derivedFrom;
        this.description = description;
        this.properties = new LinkedHashMap();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDerivedFrom() {
        return derivedFrom;
    }

    public void setDerivedFrom(String derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTypeVersion() {
        return typeVersion;
    }

    public void setTypeVersion(String typeVersion) {
        this.typeVersion = typeVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LinkedHashMap<String, ToscaElementProperty> getProperties() {
        return properties;
    }

    public void setProperties(LinkedHashMap<String, ToscaElementProperty> properties) {
        this.properties = properties;
    }

    public void addProperties(ToscaElementProperty toscaElementProperty) {
        this.properties.put(toscaElementProperty.getName(), toscaElementProperty);
    }

    public ArrayList<String> propertiesNames() {
        return new ArrayList<>(properties.keySet());
    }

    @Override
    public String toString() {
        return name + ": " + description + ", version: " + version + ", nb de properties: " + properties.size()
                + System.getProperty("line.separator") + propertiesNames();
    }
}
