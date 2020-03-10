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

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class Template {

    /**
     * name parameter is used as "key", in the LinkedHashMap of Templates.
     */
    private String name;
    private List<TemplateField> templateFields;

    public Template(String name) {
        this.name = name;
        this.templateFields = new ArrayList<>();
    }

    public Template(String name, List<TemplateField> templateFields) {
        this.name = name;
        this.templateFields = templateFields;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TemplateField> getTemplateFields() {
        return templateFields;
    }

    public void setTemplateFields(List<TemplateField> templateFields) {
        this.templateFields = templateFields;
    }

    /**
     * Search in fields if fieldName exists.
     *
     * @param fieldName The field name
     * @return Ture if it exists, false otherwise
     */
    public boolean hasFields(String fieldName) {
        for (TemplateField templateField : this.getTemplateFields()) {
            if (templateField.getTitle().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a specific Field.
     *
     * @param fieldName The field name
     * @return THe Field found
     */
    public TemplateField getSpecificField(String fieldName) {
        for (TemplateField templateField : this.getTemplateFields()) {
            if (templateField.getTitle().equals(fieldName)) {
                return templateField;
            }
        }
        return null;
    }

    public void addField(TemplateField templateField) {
        templateFields.add(templateField);
    }

    public void removeField(TemplateField templateField) {
        templateFields.remove(templateField);
    }

    /**
     * Enable or disable the visibility.
     *
     * @param nameField THe field name
     * @param state True or false
     */
    public void setVisibility(String nameField, boolean state) {
        for (TemplateField templateField : this.templateFields) {
            if (templateField.getTitle().equals(nameField)) {
                templateField.setVisible(state);
            }
        }
    }

    /**
     * This method defines if a field is static or not.
     *
     * @param nameField The name of the field
     * @param state true or false
     */
    public void setStatic(String nameField, boolean state) {
        for (TemplateField templateField : this.templateFields) {
            if (templateField.getTitle().equals(nameField)) {
                templateField.setStaticValue(state);
            }
        }
    }

    /**
     * This method updates the value of a specfic field.
     *
     * @param nameField The name of the field
     * @param newValue The new value as Object
     */
    public void updateValueField(String nameField, Object newValue) {
        for (TemplateField templateField : this.templateFields) {
            if (templateField.getTitle().equals(nameField)) {
                templateField.setValue(newValue);
            }
        }
    }

    /**
     * Compare two templates : size and their contents.
     *
     * @param template the template
     * @return a boolean
     */
    public boolean checkFields(Template template) {
        boolean duplicateFields = false;
        if (template.getTemplateFields().size() == this.getTemplateFields().size()) {
            int countMatchingFields = 0;
            //loop each component of first
            for (TemplateField templateFieldToCheck : template.getTemplateFields()) {
                for (TemplateField templateField : this.getTemplateFields()) {
                    if (templateFieldToCheck.compareWithField(templateField)) {
                        countMatchingFields++;
                    }
                }
            }

            if (template.getTemplateFields().size() == countMatchingFields) {
                duplicateFields = true;
            }
        }
        return duplicateFields;
    }

    /**
     * This method gets the specific field status.
     *
     * @param field The field name
     * @return true or false
     */
    public boolean fieldStaticStatus(String field) {
        if (this.hasFields(field) && this.getSpecificField(field).getStaticValue().equals(true)
                && this.getSpecificField(field).getValue() != null) {
            return true;
        }
        return false;
    }

    public boolean isVisible(String field) {
        return this.getSpecificField(field).getVisible();
    }

    /**
     * Set the value of a property of the Field in the json.
     *
     * @param jsonSchema The Json schema
     * @param fieldName The Field name
     * @param value The value
     */
    public void setValue(JsonObject jsonSchema, String fieldName, String value) {
        if (isVisible(fieldName)) {
            if (fieldStaticStatus(fieldName)) {
                String defaultValue = (String) this.getSpecificField(fieldName).getValue();
                jsonSchema.addProperty(fieldName, defaultValue);
            }
            else {
                jsonSchema.addProperty(fieldName, value);
            }
        }
    }

    /**
     * Inject a static value in the json.
     *
     * @param jsonSchema The json schema object
     * @param fieldName The field name
     */
    public void injectStaticValue(JsonObject jsonSchema, String fieldName) {
        if (isVisible(fieldName)) {
            TemplateField toInject = this.getSpecificField(fieldName);
            jsonSchema.addProperty(fieldName, (String) toInject.getValue());
        }
    }

    @Override
    public String toString() {
        return " templateFields : " + templateFields;
    }
}
