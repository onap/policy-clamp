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

package org.onap.clamp.clds.tosca.update.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.onap.clamp.clds.tosca.update.elements.ToscaElement;
import org.onap.clamp.clds.tosca.update.elements.ToscaElementProperty;
import org.onap.clamp.clds.tosca.update.parser.metadata.ToscaMetadataParser;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplate;
import org.onap.clamp.loop.service.Service;

public class ToscaConverterToJsonSchema {
    private LinkedHashMap<String, ToscaElement> components;
    private LinkedHashMap<String, JsonTemplate> templates;

    private ToscaMetadataParser metadataParser;

    private Service serviceModel;

    /**
     * Constructor.
     *
     * @param toscaElementsMap    All the tosca elements found (policy type + data types + native tosca datatypes)
     * @param jsonSchemaTemplates All Json schema templates to use
     * @param metadataParser      The metadata parser to use for metadata section
     * @param serviceModel        The service model for clamp enrichment
     */
    public ToscaConverterToJsonSchema(LinkedHashMap<String, ToscaElement> toscaElementsMap,
                                      LinkedHashMap<String, JsonTemplate> jsonSchemaTemplates,
                                      ToscaMetadataParser metadataParser, Service serviceModel) {
        this.components = toscaElementsMap;
        this.templates = jsonSchemaTemplates;
        this.metadataParser = metadataParser;
        this.serviceModel = serviceModel;
    }

    /**
     * For a given component, launch process to parse it in Json.
     *
     * @param toscaElementKey name components
     * @return return
     */
    public JsonObject getJsonSchemaOfToscaElement(String toscaElementKey) {
        return this.getFieldAsObject(getToscaElement(toscaElementKey));
    }

    /**
     * Return the classical/general fields of the component, & launch the properties deployment.
     *
     * @param toscaElement the compo
     * @return a json object
     */
    public JsonObject getFieldAsObject(ToscaElement toscaElement) {

        JsonObject globalFields = new JsonObject();
        if (templates.get("object").hasFields("title")) {
            globalFields.addProperty("title", toscaElement.getName());
        }
        if (templates.get("object").hasFields("type")) {
            globalFields.addProperty("type", "object");
        }
        if (templates.get("object").hasFields("description")) {
            if (toscaElement.getDescription() != null) {
                globalFields.addProperty("description", toscaElement.getDescription());
            }
        }
        if (templates.get("object").hasFields("required")) {
            globalFields.add("required", this.getRequirements(toscaElement.getName()));
        }
        if (templates.get("object").hasFields("properties")) {
            globalFields.add("properties", this.deploy(toscaElement.getName()));
        }
        return globalFields;
    }

    /**
     * Get the required properties of the Component, including the parents properties requirements.
     *
     * @param nameComponent name component
     * @return a json array
     */
    public JsonArray getRequirements(String nameComponent) {
        JsonArray requirements = new JsonArray();
        ToscaElement toParse = components.get(nameComponent);
        //Check for a father component, and launch the same process
        if (!toParse.getDerivedFrom().equals("tosca.datatypes.Root")
                && !toParse.getDerivedFrom().equals("tosca.policies.Root")) {
            requirements.addAll(getRequirements(toParse.getDerivedFrom()));
        }
        //Each property is checked, and add to the requirement array if it's required
        Collection<ToscaElementProperty> properties = toParse.getProperties().values();
        for (ToscaElementProperty toscaElementProperty : properties) {
            if (toscaElementProperty.getItems().containsKey("required")
                    && toscaElementProperty.getItems().get("required").equals(true)) {
                requirements.add(toscaElementProperty.getName());
            }
        }
        return requirements;
    }

    /**
     * The beginning of the recursive process. Get the parents (or not) to launch the same process, and otherwise
     * deploy and parse the properties.
     *
     * @param nameComponent name component
     * @return a json object
     */
    public JsonObject deploy(String nameComponent) {
        JsonObject jsonSchema = new JsonObject();
        ToscaElement toParse = components.get(nameComponent);
        //Check for a father component, and launch the same process
        if (!toParse.getDerivedFrom().equals("tosca.datatypes.Root")
                && !toParse.getDerivedFrom().equals("tosca.policies.Root")) {
            jsonSchema = this.getParent(toParse.getDerivedFrom());
        }
        //For each component property, check if its a complex properties (a component) or not. In that case,
        //launch the analyse of the property.
        for (Entry<String, ToscaElementProperty> property : toParse.getProperties().entrySet()) {
            if (getToscaElement((String) property.getValue().getItems().get("type")) != null) {
                jsonSchema.add(property.getValue().getName(),
                        this.getJsonSchemaOfToscaElement((String) property.getValue().getItems().get("type")));
            }
            else {
                jsonSchema.add(property.getValue().getName(), this.complexParse(property.getValue()));
            }
        }
        return jsonSchema;
    }

    /**
     * If a component has a parent, it is deploy in the same way.
     *
     * @param nameComponent name component
     * @return a json object
     */
    public JsonObject getParent(String nameComponent) {
        return deploy(nameComponent);
    }

    /**
     * to be done.
     *
     * @param toscaElementProperty property
     * @return a json object
     */
    @SuppressWarnings("unchecked")
    public JsonObject complexParse(ToscaElementProperty toscaElementProperty) {
        JsonObject propertiesInJson = new JsonObject();
        JsonTemplate currentPropertyJsonTemplate;
        String typeProperty = (String) toscaElementProperty.getItems().get("type");
        if (typeProperty.toLowerCase().equals("list") || typeProperty.toLowerCase().equals("map")) {
            currentPropertyJsonTemplate = templates.get("object");
        }
        else {
            String propertyType = (String) toscaElementProperty.getItems().get("type");
            currentPropertyJsonTemplate = templates.get(propertyType.toLowerCase());
        }
        //Each "special" field is analysed, and has a specific treatment
        for (String propertyField : toscaElementProperty.getItems().keySet()) {
            switch (propertyField) {
                case "type":
                    if (currentPropertyJsonTemplate.hasFields(propertyField)) {
                        String fieldtype = (String) toscaElementProperty.getItems().get(propertyField);
                        switch (fieldtype.toLowerCase()) {
                            case "list":
                                propertiesInJson.addProperty("type", "array");
                                break;
                            case "map":
                                propertiesInJson.addProperty("type", "object");
                                break;
                            case "scalar-unit.time":
                            case "scalar-unit.frequency":
                            case "scalar-unit.size":
                                propertiesInJson.addProperty("type", "string");
                                break;
                            case "timestamp":
                                propertiesInJson.addProperty("type", "string");
                                propertiesInJson.addProperty("format", "date-time");
                                break;
                            case "float":
                                propertiesInJson.addProperty("type", "number");
                                break;
                            case "range":
                                propertiesInJson.addProperty("type", "integer");
                                if (!checkConstraintPresence(toscaElementProperty, "greater_than")
                                        && currentPropertyJsonTemplate.hasFields("exclusiveMinimum")) {
                                    propertiesInJson.addProperty("exclusiveMinimum", false);
                                }
                                if (!checkConstraintPresence(toscaElementProperty, "less_than")
                                        && currentPropertyJsonTemplate.hasFields("exclusiveMaximum")) {
                                    propertiesInJson.addProperty("exclusiveMaximum", false);
                                }
                                break;
                            default:
                                propertiesInJson.addProperty("type", currentPropertyJsonTemplate.getName());
                                break;
                        }
                    }
                    break;
                case "metadata":
                    if (metadataParser != null) {
                        metadataParser.processAllMetadataElement(toscaElementProperty, serviceModel).entrySet()
                                .forEach((jsonEntry) -> {
                                    propertiesInJson.add(jsonEntry.getKey(),
                                            jsonEntry.getValue());

                                });
                    }
                    break;
                case "constraints":
                    toscaElementProperty.addConstraintsAsJson(propertiesInJson,
                            (ArrayList<Object>) toscaElementProperty.getItems().get("constraints"),
                            currentPropertyJsonTemplate);
                    break;
                case "entry_schema":
                    //Here, a way to check if entry is a component (datatype) or a simple string
                    if (getToscaElement(this.extractSpecificFieldFromMap(toscaElementProperty, "entry_schema"))
                            != null) {
                        String nameComponent = this.extractSpecificFieldFromMap(toscaElementProperty, "entry_schema");
                        ToscaConverterToJsonSchema child = new ToscaConverterToJsonSchema(components, templates,
                                metadataParser, serviceModel);
                        JsonObject propertiesContainer = new JsonObject();

                        switch ((String) toscaElementProperty.getItems().get("type")) {
                            case "map": // Get it as an object
                                JsonObject componentAsProperty = child.getJsonSchemaOfToscaElement(nameComponent);
                                propertiesContainer.add(nameComponent, componentAsProperty);
                                if (currentPropertyJsonTemplate.hasFields("properties")) {
                                    propertiesInJson.add("properties", propertiesContainer);
                                }
                                break;
                            default://list : get it as an Array
                                JsonObject componentAsItem = child.getJsonSchemaOfToscaElement(nameComponent);
                                if (currentPropertyJsonTemplate.hasFields("properties")) {
                                    propertiesInJson.add("items", componentAsItem);
                                    propertiesInJson.addProperty("format", "tabs-top");
                                }
                                break;
                        }

                    }
                    // Native cases
                    else if (toscaElementProperty.getItems().get("type").equals("list")) {
                        JsonObject itemContainer = new JsonObject();
                        String valueInEntrySchema =
                                this.extractSpecificFieldFromMap(toscaElementProperty, "entry_schema");
                        itemContainer.addProperty("type", valueInEntrySchema);
                        propertiesInJson.add("items", itemContainer);
                        propertiesInJson.addProperty("format", "tabs-top");
                    }
                    // MAP Case, for now nothing

                    break;
                default:
                    //Each classical field : type, description, default..
                    if (currentPropertyJsonTemplate.hasFields(propertyField) && !propertyField.equals("required")) {
                        toscaElementProperty.addFieldToJson(propertiesInJson, propertyField,
                                toscaElementProperty.getItems().get(propertyField));
                    }
                    break;
            }
        }
        return propertiesInJson;
    }

    /**
     * Look for a matching Component for the name parameter, in the components list.
     *
     * @param name the tosca element name to search for
     * @return a tosca element
     */
    public ToscaElement getToscaElement(String name) {
        ToscaElement correspondingToscaElement = null;
        if (components == null) {
            return null;
        }
        for (ToscaElement toscaElement : components.values()) {
            if (toscaElement.getName().equals(name)) {
                correspondingToscaElement = toscaElement;
            }
        }
        return correspondingToscaElement;
    }

    /**
     * Simple method to extract quickly a type field from particular property item.
     *
     * @param toscaElementProperty the property
     * @param fieldName            the fieldname
     * @return a string
     */
    @SuppressWarnings("unchecked")
    public String extractSpecificFieldFromMap(ToscaElementProperty toscaElementProperty, String fieldName) {
        LinkedHashMap<String, String> entrySchemaFields =
                (LinkedHashMap<String, String>) toscaElementProperty.getItems().get(fieldName);
        return entrySchemaFields.get("type");
    }

    /**
     * Check if a constraint, for a specific property, is there.
     *
     * @param toscaElementProperty property
     * @param nameConstraint       name constraint
     * @return a flag boolean
     */
    public boolean checkConstraintPresence(ToscaElementProperty toscaElementProperty, String nameConstraint) {
        boolean presentConstraint = false;
        if (toscaElementProperty.getItems().containsKey("constraints")) {
            ArrayList<Object> constraints = (ArrayList) toscaElementProperty.getItems().get("constraints");
            for (Object constraint : constraints) {
                if (constraint instanceof LinkedHashMap) {
                    if (((LinkedHashMap) constraint).containsKey(nameConstraint)) {
                        presentConstraint = true;
                    }
                }
            }
        }
        return presentConstraint;
    }
}
