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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.onap.clamp.clds.util.JsonUtils;

public class ToscaConverterManager {

    private LinkedHashMap<String, Template> templates;
    private LinkedHashMap<String, ToscaElement> components;
    private ToscaConverterToJson toscaConverterToJson;
    private ToscaItemsParser toscaItemsParser;

    /**
     * Constructor.
     *
     * @param toscaYamlContent     Policy Tosca Yaml content as string
     * @param nativeToscaDatatypes The tosca yaml with tosca native datatypes
     * @param templateProperties   template properties as string
     * @throws IOException in case of failure
     */
    public ToscaConverterManager(String toscaYamlContent, String nativeToscaDatatypes, String templateProperties)
            throws IOException {
        if (toscaYamlContent != null && !toscaYamlContent.isEmpty()) {
            this.toscaItemsParser = new ToscaItemsParser(toscaYamlContent, nativeToscaDatatypes);
            this.components = toscaItemsParser.getAllItemsFound();
            this.templates = initializeTemplates(templateProperties);
        }
        else {
            components = null;
        }
    }

    //GETTERS & SETTERS
    public LinkedHashMap<String, ToscaElement> getComponents() {
        return components;
    }

    public void setComponents(LinkedHashMap<String, ToscaElement> components) {
        this.components = components;
    }

    public ToscaConverterToJson getParseToJson() {
        return toscaConverterToJson;
    }

    public void setParseToJson(ToscaConverterToJson toscaConverterToJson) {
        this.toscaConverterToJson = toscaConverterToJson;
    }

    public LinkedHashMap<String, Template> getTemplates() {
        return templates;
    }

    public void setTemplates(LinkedHashMap<String, Template> templates) {
        this.templates = templates;
    }

    public ToscaItemsParser getToscaItemsParser() {
        return toscaItemsParser;
    }

    /**
     * Add a template.
     *
     * @param name           name
     * @param templateFields fields
     */
    public void addTemplate(String name, List<TemplateField> templateFields) {
        Template template = new Template(name, templateFields);
        //If it is true, the operation does not have any interest :
        // replace OR put two different object with the same body
        if (!templates.containsKey(template.getName()) || !this.hasTemplate(template)) {
            this.templates.put(template.getName(), template);
        }
    }

    /**
     * By name, find and remove a given template.
     *
     * @param nameTemplate name template
     */
    public void removeTemplate(String nameTemplate) {
        this.templates.remove(nameTemplate);
    }

    /**
     * Update Template : adding with true flag, removing with false.
     *
     * @param nameTemplate  name template
     * @param templateField field name
     * @param operation     operation
     */
    public void updateTemplate(String nameTemplate, TemplateField templateField, Boolean operation) {
        // Operation = true && field is not present => add Field
        if (operation && !this.templates.get(nameTemplate).getTemplateFields().contains(templateField)) {
            this.templates.get(nameTemplate).addField(templateField);
        }
        // Operation = false && field is present => remove Field
        else if (!operation && this.templates.get(nameTemplate).getTemplateFields().contains(templateField)) {
            this.templates.get(nameTemplate).removeField(templateField);
        }
    }

    /**
     * Check if the JSONTemplates have the same bodies.
     *
     * @param template template
     * @return a boolean
     */
    public boolean hasTemplate(Template template) {
        boolean duplicateTemplate = false;
        Collection<String> templatesName = templates.keySet();
        if (templatesName.contains(template.getName())) {
            Template existingTemplate = templates.get(template.getName());
            duplicateTemplate = existingTemplate.checkFields(template);
        }
        return duplicateTemplate;
    }

    /**
     * For a given Component, get a corresponding JsonObject, through parseToJSON.
     *
     * @param componentName name
     * @return an json object
     */
    public JsonObject startConversionToJson(String componentName) throws UnknownComponentException {
        this.toscaConverterToJson = new ToscaConverterToJson(components, templates);
        if (toscaConverterToJson.matchComponent(componentName) == null) {
            throw new UnknownComponentException(componentName);
        }
        return toscaConverterToJson.getJsonProcess(componentName, "object");
    }

    /**
     * Create and complete several Templates from file.properties.
     *
     * @param jsonTemplates The template properties as String
     * @return a map
     */
    @SuppressWarnings("unused")
    private LinkedHashMap<String, Template> initializeTemplates(String jsonTemplates) {

        LinkedHashMap<String, Template> generatedTemplates = new LinkedHashMap<>();
        JsonObject templates = JsonUtils.GSON.fromJson(jsonTemplates, JsonObject.class);

        for (Map.Entry<String, JsonElement> templateAsJson : templates.entrySet()) {
            Template template = new Template(templateAsJson.getKey());
            JsonObject templateBody = (JsonObject) templateAsJson.getValue();
            for (Map.Entry<String, JsonElement> field : templateBody.entrySet()) {
                String fieldName = field.getKey();
                JsonObject bodyFieldAsJson = (JsonObject) field.getValue();
                Object fieldValue = bodyFieldAsJson.get("defaultValue").getAsString();
                Boolean fieldVisible = bodyFieldAsJson.get("visible").getAsBoolean();
                Boolean fieldStatic = bodyFieldAsJson.get("static").getAsBoolean();
                TemplateField bodyTemplateField = new TemplateField(fieldName, fieldValue, fieldVisible, fieldStatic);
                template.getTemplateFields().add(bodyTemplateField);
            }
            generatedTemplates.put(template.getName(), template);
        }
        return generatedTemplates;
    }

}
