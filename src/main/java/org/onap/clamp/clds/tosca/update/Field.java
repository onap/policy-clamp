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

public class Field {
    private String title;
    private Object value;
    private Boolean visible;
    private Boolean staticValue;

    public Field(String title) {
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
    public Field(String title, Object value, Boolean visible, Boolean staticValue) {
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

        Field field = (Field) otherField;

        if (title != null ? !title.equals(field.title) : field.title != null) {
            return false;
        }
        if (value != null ? !value.equals(field.value) : field.value != null) {
            return false;
        }
        if (visible != null ? !visible.equals(field.visible) : field.visible != null) {
            return false;
        }
        return staticValue != null ? staticValue.equals(field.staticValue) : field.staticValue == null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Field field = (Field) object;

        return title != null ? title.equals(field.title) : field.title == null;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }

    /**
     * This method test the entire equality.
     *
     * @param field1 object one
     * @param field2 object two
     * @return true if they are totally equals (all attributes, false otherwise
     */
    public static boolean fieldsEquals(Field field1, Field field2) {
        return (field2.getTitle().equals(field1.getTitle()) && field2.getValue().equals(field1.getValue())
                && field2.getVisible().equals(field1.getVisible())
                && field2.getStaticValue().equals(field1.getStaticValue()));
    }

}
