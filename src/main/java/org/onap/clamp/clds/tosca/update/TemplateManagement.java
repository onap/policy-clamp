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

public class TemplateManagement {

    private LinkedHashMap<String, Template> templates;
    private LinkedHashMap<String, Component> components;
    private ParserToJson parserToJson;
    private Extractor extractor;

    /**
     * Constructor.
     *
     * @param yamlContent        Yaml content as string
     * @param templateProperties template properties as string
     * @throws IOException in case of failure
     */
    public TemplateManagement(String yamlContent, String nativeComponent, String templateProperties)
            throws IOException {
        if (yamlContent != null && !yamlContent.isEmpty()) {
            this.extractor = new Extractor(yamlContent, nativeComponent);
            this.components = extractor.getAllItems();
            this.templates = initializeTemplates(templateProperties);
        }
        else {
            components = null;
        }
    }

    //GETTERS & SETTERS
    public LinkedHashMap<String, Component> getComponents() {
        return components;
    }

    public void setComponents(LinkedHashMap<String, Component> components) {
        this.components = components;
    }

    public ParserToJson getParseToJson() {
        return parserToJson;
    }

    public void setParseToJson(ParserToJson parserToJson) {
        this.parserToJson = parserToJson;
    }

    public LinkedHashMap<String, Template> getTemplates() {
        return templates;
    }

    public void setTemplates(LinkedHashMap<String, Template> templates) {
        this.templates = templates;
    }

    public Extractor getExtractor() {
        return extractor;
    }

    /**
     * Add a template.
     *
     * @param name   name
     * @param fields fields
     */
    public void addTemplate(String name, List<Field> fields) {
        Template template = new Template(name, fields);
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
     * @param nameTemplate name template
     * @param field        field name
     * @param operation    operation
     */
    public void updateTemplate(String nameTemplate, Field field, Boolean operation) {
        // Operation = true && field is not present => add Field
        if (operation && !this.templates.get(nameTemplate).getFields().contains(field)) {
            this.templates.get(nameTemplate).addField(field);
        }
        // Operation = false && field is present => remove Field
        else if (!operation && this.templates.get(nameTemplate).getFields().contains(field)) {
            this.templates.get(nameTemplate).removeField(field);
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
    public JsonObject launchTranslation(String componentName) throws UnknownComponentException {
        this.parserToJson = new ParserToJson(components, templates);
        if (parserToJson.matchComponent(componentName) == null) {
            throw new UnknownComponentException(componentName);
        }
        return parserToJson.getJsonProcess(componentName, "object");
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
                Field bodyField = new Field(fieldName, fieldValue, fieldVisible, fieldStatic);
                template.getFields().add(bodyField);
            }
            generatedTemplates.put(template.getName(), template);
        }
        return generatedTemplates;
    }

}
