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

public class TemplateField {
    private String title;
    private Object value;
    private Boolean visible;
    private Boolean staticValue;

    public TemplateField(String title) {
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
    public TemplateField(String title, Object value, Boolean visible, Boolean staticValue) {
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

        TemplateField templateField = (TemplateField) otherField;

        if (title != null ? !title.equals(templateField.title) : templateField.title != null) {
            return false;
        }
        if (value != null ? !value.equals(templateField.value) : templateField.value != null) {
            return false;
        }
        if (visible != null ? !visible.equals(templateField.visible) : templateField.visible != null) {
            return false;
        }
        return staticValue != null ? staticValue.equals(templateField.staticValue) : templateField.staticValue == null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        TemplateField templateField = (TemplateField) object;

        return title != null ? title.equals(templateField.title) : templateField.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }

    /**
     * This method test the entire equality.
     *
     * @param templateField1 object one
     * @param templateField2 object two
     * @return true if they are totally equals (all attributes, false otherwise
     */
    public static boolean fieldsEquals(TemplateField templateField1, TemplateField templateField2) {
        return (templateField2.getTitle().equals(templateField1.getTitle())
                && templateField2.getValue().equals(templateField1.getValue())
                && templateField2.getVisible().equals(templateField1.getVisible())
                && templateField2.getStaticValue().equals(templateField1.getStaticValue()));
    }

}
