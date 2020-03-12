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

public class JsonTemplateField {
    private String title;
    private Object value;
    private Boolean visible;
    private Boolean staticValue;

    public JsonTemplateField(String title) {
        this.title = title;
    }

    /**
     * Constructor.
     *
     * @param title       The title
     * @param value       The value
     * @param visible     visible or not
     * @param staticValue The static value
     */
    public JsonTemplateField(String title, Object value, Boolean visible, Boolean staticValue) {
        this.title = title;
        this.value = value;
        this.visible = visible;
        this.staticValue = staticValue;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Boolean getStaticValue() {
        return staticValue;
    }

    public void setStaticValue(Boolean staticValue) {
        this.staticValue = staticValue;
    }

    public String toString() {
        return title + " " + value + " " + visible + " " + staticValue;
    }

    /**
     * This method compares two fields.
     *
     * @param otherField Compare the current object with the one specified
     * @return true if they are totally equals, false otherwise
     */
    public boolean compareWithField(Object otherField) {
        if (this == otherField) {
            return true;
        }
        if (otherField == null || getClass() != otherField.getClass()) {
            return false;
        }

        JsonTemplateField jsonTemplateField = (JsonTemplateField) otherField;

        if (title != null ? !title.equals(jsonTemplateField.title) : jsonTemplateField.title != null) {
            return false;
        }
        if (value != null ? !value.equals(jsonTemplateField.value) : jsonTemplateField.value != null) {
            return false;
        }
        if (visible != null ? !visible.equals(jsonTemplateField.visible) : jsonTemplateField.visible != null) {
            return false;
        }
        return staticValue != null ? staticValue.equals(jsonTemplateField.staticValue) :
                jsonTemplateField.staticValue == null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        JsonTemplateField jsonTemplateField = (JsonTemplateField) object;

        return title != null ? title.equals(jsonTemplateField.title) : jsonTemplateField.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }

    /**
     * This method test the entire equality.
     *
     * @param jsonTemplateField1 object one
     * @param jsonTemplateField2 object two
     * @return true if they are totally equals (all attributes, false otherwise
     */
    public static boolean fieldsEquals(JsonTemplateField jsonTemplateField1, JsonTemplateField jsonTemplateField2) {
        return (jsonTemplateField2.getTitle().equals(jsonTemplateField1.getTitle())
                && jsonTemplateField2.getValue().equals(jsonTemplateField1.getValue())
                && jsonTemplateField2.getVisible().equals(jsonTemplateField1.getVisible())
                && jsonTemplateField2.getStaticValue().equals(jsonTemplateField1.getStaticValue()));
    }

}
