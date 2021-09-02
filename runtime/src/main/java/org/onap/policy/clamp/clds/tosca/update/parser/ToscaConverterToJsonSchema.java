/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.tosca.update.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.onap.policy.clamp.clds.tosca.update.elements.ToscaElement;
import org.onap.policy.clamp.clds.tosca.update.elements.ToscaElementProperty;
import org.onap.policy.clamp.clds.tosca.update.parser.metadata.ToscaMetadataParser;
import org.onap.policy.clamp.clds.tosca.update.templates.JsonTemplate;
import org.onap.policy.clamp.loop.service.Service;

/**
 * This class can be used to convert a tosca to a json schema.
 * This class is not supposed to be used directly because it requires the json Schema templates
 * (template conversion tosca type to json schema entry) but also the supported Tosca main type file.
 * The class ToscaConverterWithDictionarySupport is more complete for the end user to be used (in the clamp context).
 *
 * @see org.onap.policy.clamp.clds.tosca.update.ToscaConverterWithDictionarySupport#convertToscaToJsonSchemaObject
 * @see org.onap.policy.clamp.clds.tosca.update.parser.ToscaConverterToJsonSchema#getJsonSchemaOfToscaElement
 */
public class ToscaConverterToJsonSchema {
    private static final String ARRAY = "array";
    private static final String CONSTRAINTS = "constraints";
    private static final String DESCRIPTION = "description";
    private static final String ENTRY_SCHEMA = "entry_schema";
    private static final String FORMAT = "format";
    private static final String LIST = "list";
    private static final String MAP = "map";
    private static final String METADATA = "metadata";
    private static final String OBJECT = "object";
    private static final String PROPERTIES = "properties";
    private static final String REQUIRED = "required";
    private static final String TITLE = "title";
    private static final String TYPE = "type";

    private Map<String, ToscaElement> components;
    private Map<String, JsonTemplate> templates;

    private ToscaMetadataParser metadataParser;

    private Service serviceModel;

    /**
     * Constructor.
     *
     * @param toscaElementsMap All the tosca elements found (policy type + data types + native tosca datatypes)
     * @param jsonSchemaTemplates All Json schema templates to use
     * @param metadataParser The metadata parser to use for metadata section
     * @param serviceModel The service model for clamp enrichment
     */
    public ToscaConverterToJsonSchema(Map<String, ToscaElement> toscaElementsMap,
            Map<String, JsonTemplate> jsonSchemaTemplates, ToscaMetadataParser metadataParser,
            Service serviceModel) {
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

        var globalFields = new JsonObject();
        if (templates.get(OBJECT).hasFields(TITLE)) {
            globalFields.addProperty(TITLE, toscaElement.getName());
        }
        if (templates.get(OBJECT).hasFields(TYPE)) {
            globalFields.addProperty(TYPE, OBJECT);
        }
        if (templates.get(OBJECT).hasFields(DESCRIPTION) && (toscaElement.getDescription() != null)) {
            globalFields.addProperty(DESCRIPTION, toscaElement.getDescription());
        }
        if (templates.get(OBJECT).hasFields(REQUIRED)) {
            globalFields.add(REQUIRED, this.getRequirements(toscaElement.getName()));
        }
        if (templates.get(OBJECT).hasFields(PROPERTIES)) {
            globalFields.add(PROPERTIES, this.deploy(toscaElement.getName()));
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
        var requirements = new JsonArray();
        ToscaElement toParse = components.get(nameComponent);
        // Check for a father component, and launch the same process
        if (!"tosca.datatypes.Root".equals(toParse.getDerivedFrom())
                && !"tosca.policies.Root".equals(toParse.getDerivedFrom())) {
            requirements.addAll(getRequirements(toParse.getDerivedFrom()));
        }
        // Each property is checked, and add to the requirement array if it's required
        Collection<ToscaElementProperty> properties = toParse.getProperties().values();
        for (ToscaElementProperty toscaElementProperty : properties) {
            if (toscaElementProperty.getItems().containsKey(REQUIRED)
                    && toscaElementProperty.getItems().get(REQUIRED).equals(true)) {
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
        var jsonSchema = new JsonObject();
        ToscaElement toParse = components.get(nameComponent);
        // Check for a father component, and launch the same process
        if (!toParse.getDerivedFrom().equals("tosca.datatypes.Root")
                && !toParse.getDerivedFrom().equals("tosca.policies.Root")) {
            jsonSchema = this.getParent(toParse.getDerivedFrom());
        }
        // For each component property, check if its a complex properties (a component) or not. In that case,
        // launch the analyse of the property.
        for (Entry<String, ToscaElementProperty> property : toParse.getProperties().entrySet()) {
            if (getToscaElement((String) property.getValue().getItems().get(TYPE)) != null) {
                jsonSchema.add(property.getValue().getName(),
                        this.getJsonSchemaOfToscaElement((String) property.getValue().getItems().get(TYPE)));
            } else {
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
        var propertiesInJson = new JsonObject();
        JsonTemplate currentPropertyJsonTemplate;
        String typeProperty = (String) toscaElementProperty.getItems().get(TYPE);
        if (LIST.equalsIgnoreCase(typeProperty) || MAP.equalsIgnoreCase(typeProperty)) {
            currentPropertyJsonTemplate = templates.get(OBJECT);
        } else {
            String propertyType = (String) toscaElementProperty.getItems().get(TYPE);
            currentPropertyJsonTemplate = templates.get(propertyType.toLowerCase());
        }
        // Each "special" field is analysed, and has a specific treatment
        for (String propertyField : toscaElementProperty.getItems().keySet()) {
            switch (propertyField) {
                case TYPE:
                    parseType(toscaElementProperty, propertyField, propertiesInJson, currentPropertyJsonTemplate);
                    break;
                case METADATA:
                    if (metadataParser != null) {
                        metadataParser.processAllMetadataElement(toscaElementProperty, serviceModel).entrySet()
                                .forEach(jsonEntry -> propertiesInJson.add(jsonEntry.getKey(), jsonEntry.getValue()));
                    }
                    break;
                case CONSTRAINTS:
                    toscaElementProperty.addConstraintsAsJson(propertiesInJson,
                            (ArrayList<Object>) toscaElementProperty.getItems().get(CONSTRAINTS),
                            currentPropertyJsonTemplate);
                    break;
                case ENTRY_SCHEMA:
                    parseEntrySchema(toscaElementProperty, propertiesInJson, currentPropertyJsonTemplate);
                    break;
                default:
                    // Each classical field : type, description, default..
                    if (currentPropertyJsonTemplate.hasFields(propertyField) && !propertyField.equals(REQUIRED)) {
                        toscaElementProperty.addFieldToJson(propertiesInJson, propertyField,
                                toscaElementProperty.getItems().get(propertyField));
                    }
                    break;
            }
        }
        return propertiesInJson;
    }

    private void parseType(ToscaElementProperty toscaElementProperty, String propertyField, JsonObject propertiesInJson,
                    JsonTemplate currentPropertyJsonTemplate) {
        if (currentPropertyJsonTemplate.hasFields(propertyField)) {
            String fieldtype = (String) toscaElementProperty.getItems().get(propertyField);
            switch (fieldtype.toLowerCase()) {
                case LIST:
                    propertiesInJson.addProperty(TYPE, ARRAY);
                    break;
                case MAP:
                    propertiesInJson.addProperty(TYPE, OBJECT);
                    break;
                case "scalar-unit.time":
                case "scalar-unit.frequency":
                case "scalar-unit.size":
                    propertiesInJson.addProperty(TYPE, "string");
                    break;
                case "timestamp":
                    propertiesInJson.addProperty(TYPE, "string");
                    propertiesInJson.addProperty(FORMAT, "date-time");
                    break;
                case "float":
                    propertiesInJson.addProperty(TYPE, "number");
                    break;
                case "range":
                    propertiesInJson.addProperty(TYPE, "integer");
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
                    propertiesInJson.addProperty(TYPE, currentPropertyJsonTemplate.getName());
                    break;
            }
        }
    }

    private void parseEntrySchema(ToscaElementProperty toscaElementProperty, JsonObject propertiesInJson,
                    JsonTemplate currentPropertyJsonTemplate) {
        // Here, a way to check if entry is a component (datatype) or a simple string
        if (getToscaElement(this.extractSpecificFieldFromMap(toscaElementProperty, ENTRY_SCHEMA)) != null) {
            String nameComponent = this.extractSpecificFieldFromMap(toscaElementProperty, ENTRY_SCHEMA);
            var child = new ToscaConverterToJsonSchema(components, templates, metadataParser, serviceModel);
            var propertiesContainer = new JsonObject();

            if (((String) toscaElementProperty.getItems().get(TYPE)).equals(MAP)) {
                JsonObject componentAsProperty = child.getJsonSchemaOfToscaElement(nameComponent);
                propertiesContainer.add(nameComponent, componentAsProperty);
                if (currentPropertyJsonTemplate.hasFields(PROPERTIES)) {
                    propertiesInJson.add(PROPERTIES, propertiesContainer);
                }
            } else {
                JsonObject componentAsItem = child.getJsonSchemaOfToscaElement(nameComponent);
                if (currentPropertyJsonTemplate.hasFields(PROPERTIES)) {
                    propertiesInJson.add("items", componentAsItem);
                    propertiesInJson.addProperty(FORMAT, "tabs-top");
                }
            }
        } else if (toscaElementProperty.getItems().get(TYPE).equals(LIST)) {
            // Native cases
            var itemContainer = new JsonObject();
            String valueInEntrySchema =
                    this.extractSpecificFieldFromMap(toscaElementProperty, ENTRY_SCHEMA);
            itemContainer.addProperty(TYPE, valueInEntrySchema);
            propertiesInJson.add("items", itemContainer);
            propertiesInJson.addProperty(FORMAT, "tabs-top");
        }

        // MAP Case, for now nothing
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
     * @param fieldName the fieldname
     * @return a string
     */
    @SuppressWarnings("unchecked")
    public String extractSpecificFieldFromMap(ToscaElementProperty toscaElementProperty, String fieldName) {
        Map<String, String> entrySchemaFields =
                (Map<String, String>) toscaElementProperty.getItems().get(fieldName);
        return entrySchemaFields.get(TYPE);
    }

    /**
     * Check if a constraint, for a specific property, is there.
     *
     * @param toscaElementProperty property
     * @param nameConstraint name constraint
     * @return a flag boolean
     */
    public boolean checkConstraintPresence(ToscaElementProperty toscaElementProperty, String nameConstraint) {
        var presentConstraint = false;
        if (toscaElementProperty.getItems().containsKey(CONSTRAINTS)) {
            @SuppressWarnings("unchecked")
            ArrayList<Object> constraints = (ArrayList<Object>) toscaElementProperty.getItems().get(CONSTRAINTS);
            for (Object constraint : constraints) {
                if (constraint instanceof Map
                        && ((Map<?, ?>) constraint).containsKey(nameConstraint)) {
                    presentConstraint = true;
                }
            }
        }
        return presentConstraint;
    }
}
