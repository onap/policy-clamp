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

package org.onap.clamp.clds.tosca.update.templates;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.onap.clamp.clds.tosca.update.UnknownComponentException;
import org.onap.clamp.clds.tosca.update.elements.ToscaElement;
import org.onap.clamp.clds.tosca.update.parser.ToscaConverterToJsonSchema;
import org.onap.clamp.clds.tosca.update.parser.ToscaElementParser;
import org.onap.clamp.clds.tosca.update.parser.metadata.ToscaMetadataParser;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.service.Service;

public class JsonTemplateManager {
    private LinkedHashMap<String, JsonTemplate> jsonSchemaTemplates;
    private LinkedHashMap<String, ToscaElement> toscaElements;

    /**
     * Constructor.
     *
     * @param toscaYamlContent     Policy Tosca Yaml content as string
     * @param nativeToscaDatatypes The tosca yaml with tosca native datatypes
     * @param jsonSchemaTemplates  template properties as string
     */
    public JsonTemplateManager(String toscaYamlContent, String nativeToscaDatatypes, String jsonSchemaTemplates) {
        if (toscaYamlContent != null && !toscaYamlContent.isEmpty()) {
            this.toscaElements = ToscaElementParser.searchAllToscaElements(toscaYamlContent, nativeToscaDatatypes);
            this.jsonSchemaTemplates = initializeTemplates(jsonSchemaTemplates);
        }
        else {
            toscaElements = null;
        }
    }

    //GETTERS & SETTERS
    public LinkedHashMap<String, ToscaElement> getToscaElements() {
        return toscaElements;
    }

    public void setToscaElements(LinkedHashMap<String, ToscaElement> toscaElements) {
        this.toscaElements = toscaElements;
    }

    public LinkedHashMap<String, JsonTemplate> getJsonSchemaTemplates() {
        return jsonSchemaTemplates;
    }

    public void setJsonSchemaTemplates(LinkedHashMap<String, JsonTemplate> jsonSchemaTemplates) {
        this.jsonSchemaTemplates = jsonSchemaTemplates;
    }

    /**
     * Add a template.
     *
     * @param name               name
     * @param jsonTemplateFields fields
     */
    public void addTemplate(String name, List<JsonTemplateField> jsonTemplateFields) {
        JsonTemplate jsonTemplate = new JsonTemplate(name, jsonTemplateFields);
        //If it is true, the operation does not have any interest :
        // replace OR put two different object with the same body
        if (!jsonSchemaTemplates.containsKey(jsonTemplate.getName()) || !this.hasTemplate(jsonTemplate)) {
            this.jsonSchemaTemplates.put(jsonTemplate.getName(), jsonTemplate);
        }
    }

    /**
     * By name, find and remove a given template.
     *
     * @param nameTemplate name template
     */
    public void removeTemplate(String nameTemplate) {
        this.jsonSchemaTemplates.remove(nameTemplate);
    }

    /**
     * Update Template : adding with true flag, removing with false.
     *
     * @param nameTemplate      name template
     * @param jsonTemplateField field name
     * @param operation         operation
     */
    public void updateTemplate(String nameTemplate, JsonTemplateField jsonTemplateField, Boolean operation) {
        // Operation = true && field is not present => add Field
        if (operation
                && !this.jsonSchemaTemplates.get(nameTemplate).getJsonTemplateFields().contains(jsonTemplateField)) {
            this.jsonSchemaTemplates.get(nameTemplate).addField(jsonTemplateField);
        }
        // Operation = false && field is present => remove Field
        else if (!operation
                && this.jsonSchemaTemplates.get(nameTemplate).getJsonTemplateFields().contains(jsonTemplateField)) {
            this.jsonSchemaTemplates.get(nameTemplate).removeField(jsonTemplateField);
        }
    }

    /**
     * Check if the JSONTemplates have the same bodies.
     *
     * @param jsonTemplate template
     * @return a boolean
     */
    public boolean hasTemplate(JsonTemplate jsonTemplate) {
        boolean duplicateTemplate = false;
        Collection<String> templatesName = jsonSchemaTemplates.keySet();
        if (templatesName.contains(jsonTemplate.getName())) {
            JsonTemplate existingJsonTemplate = jsonSchemaTemplates.get(jsonTemplate.getName());
            duplicateTemplate = existingJsonTemplate.checkFields(jsonTemplate);
        }
        return duplicateTemplate;
    }

    /**
     * For a given policy type, get a corresponding JsonObject from the tosca model.
     *
     * @param policyType          The policy type in the tosca
     * @param toscaMetadataParser The MetadataParser class that must be used if metadata section are encountered, if null
     *                            they will be skipped
     * @return an json object defining the equivalent json schema from the tosca for a given policy type
     */
    public JsonObject getJsonSchemaForPolicyType(String policyType, ToscaMetadataParser toscaMetadataParser,
                                                 Service serviceModel)
            throws UnknownComponentException {
        ToscaConverterToJsonSchema
                toscaConverterToJsonSchema = new ToscaConverterToJsonSchema(toscaElements, jsonSchemaTemplates,
                toscaMetadataParser, serviceModel);
        if (toscaConverterToJsonSchema.getToscaElement(policyType) == null) {
            throw new UnknownComponentException(policyType);
        }
        return toscaConverterToJsonSchema.getJsonSchemaOfToscaElement(policyType);
    }

    /**
     * Create and complete several Templates from file.properties.
     *
     * @param jsonTemplates The template properties as String
     * @return a map
     */
    @SuppressWarnings("unused")
    private LinkedHashMap<String, JsonTemplate> initializeTemplates(String jsonTemplates) {

        LinkedHashMap<String, JsonTemplate> generatedTemplates = new LinkedHashMap<>();
        JsonObject templates = JsonUtils.GSON.fromJson(jsonTemplates, JsonObject.class);

        for (Map.Entry<String, JsonElement> templateAsJson : templates.entrySet()) {
            JsonTemplate jsonTemplate = new JsonTemplate(templateAsJson.getKey());
            JsonObject templateBody = (JsonObject) templateAsJson.getValue();
            for (Map.Entry<String, JsonElement> field : templateBody.entrySet()) {
                String fieldName = field.getKey();
                JsonObject bodyFieldAsJson = (JsonObject) field.getValue();
                Object fieldValue = bodyFieldAsJson.get("defaultValue").getAsString();
                Boolean fieldVisible = bodyFieldAsJson.get("visible").getAsBoolean();
                Boolean fieldStatic = bodyFieldAsJson.get("static").getAsBoolean();
                JsonTemplateField
                        bodyJsonTemplateField = new JsonTemplateField(fieldName, fieldValue, fieldVisible, fieldStatic);
                jsonTemplate.getJsonTemplateFields().add(bodyJsonTemplateField);
            }
            generatedTemplates.put(jsonTemplate.getName(), jsonTemplate);
        }
        return generatedTemplates;
    }
}