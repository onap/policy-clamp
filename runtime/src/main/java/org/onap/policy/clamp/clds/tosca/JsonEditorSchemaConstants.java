/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018-2019, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.tosca;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonEditorSchemaConstants {

    //Data types in JSON Schema
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_MAP = "map";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_DATE_TIME = "datetime";

    //Key elements in JSON Schema
    public static final String TYPE = "type";
    public static final String TITLE = "title";
    public static final String REQUIRED = "required";
    public static final String DEFAULT = "default";
    public static final String ENUM = "enum";
    public static final String ENUM_TITLES = "enum_titles";
    public static final String OPTIONS = "options";
    public static final String FORMAT = "format";
    public static final String ITEMS = "items";
    public static final String PROPERTIES = "properties";
    public static final String PROPERTY_ORDER = "propertyOrder";
    public static final String VALUES = "values";
    public static final String HEADER_TEMPLATE = "headerTemplate";
    public static final String HEADER_TEMPLATE_VALUE = "{{self.name}}";

    public static final String MINIMUM = "minimum";
    public static final String MAXIMUM = "maximum";
    public static final String MIN_LENGTH = "minLength";
    public static final String MAX_LENGTH = "maxLength";
    public static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    public static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    public static final String MINITEMS = "minItems";
    public static final String MAXITEMS = "maxItems";

    public static final String CUSTOM_KEY_FORMAT = "format";
    public static final String CUSTOM_KEY_FORMAT_TABS_TOP = "tabs-top";
    public static final String CUSTOM_KEY_FORMAT_TABS = "tabs";
    public static final String CUSTOM_KEY_FORMAT_INPUT = "input";
    public static final String FORMAT_SELECT = "select";
    public static final String UNIQUE_ITEMS = "uniqueItems";
    public static final String TRUE = "true";
    public static final String QSSCHEMA = "qschema";
    public static final String TYPE_QBLDR = "qbldr";

    public static final String ID = "id";
    public static final String LABEL = "label";
    public static final String OPERATORS = "operators";
    public static final String FILTERS = "filters";

    public static final String SCHEMA = "schema";
    public static final String CURRENT_VALUES = "currentValues";

    public static final String PLUGIN = "plugin";
    public static final String DATE_TIME_PICKER = "datetimepicker";
    public static final String VALIDATION = "validation";
    public static final String DATE_TIME_FORMAT = "YYYY/MM/DD HH:mm:ss";
    public static final String INPUT_EVENT = "input_event";
    public static final String DP_CHANGE = "dp.change";

}
