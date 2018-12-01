/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.tosca;

public class ToscaSchemaConstants {

	//Data types in TOSCA Schema
	public static final String TYPE_LIST = "list";
	public static final String TYPE_MAP = "map";
	public static final String TYPE_STRING = "string";
	public static final String TYPE_INTEGER = "integer";
	public static final String TYPE_FLOAT = "float";
	public static final String TYPE_BOOLEAN = "boolean";
	
	//Key elements in Tosca
	public static final String NODE_TYPES = "node_types";
	public static final String DATA_TYPES = "data_types";
	public static final String TYPE = "type";
	public static final String DESCRIPTION = "description";
	public static final String DEFAULT = "default";
	public static final String PROPERTIES = "properties";
	public static final String REQUIRED = "required";
	public static final String ENTRY_SCHEMA = "entry_schema";

	//Constraints
	public static final String CONSTRAINTS = "constraints";
	public static final String VALID_VALUES = "valid_values";
	public static final String EQUAL = "equal";
	public static final String GREATER_THAN = "greater_than";
	public static final String GREATER_OR_EQUAL = "greater_or_equal";
	public static final String LESS_THAN = "less_than";
	public static final String LESS_OR_EQUAL = "less_or_equal";
	public static final String IN_RANGE = "in_range";
	public static final String  LENGTH = "length";
	public static final String MIN_LENGTH = "min_length";
	public static final String MAX_LENGTH = "max_length";
	public static final String PATTERN = "pattern";
	
	//Prefix for policy nodes
	public static final String POLICY_NODE = "policy.nodes.";
	
	//Prefix for data nodes
	public static final String POLICY_DATA = "policy.data.";
	
	//Prefix for dictionary elements
	public static final String DICTIONARY = "Dictionary:";
	
	//Custom Elements that must exist in the Tosca models
	public static final String NAME = "name";
	public static final String CONTEXT = "context";

	
	
	
}
