/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

var uploadfile=null;
var modelXML = '';
var list_models = {};
var selected_model = '';
var selected_template='';
var list_model_test_sets={};
var list_model_path_details={};
var list_model_schema_extensions={};
var list_model_test_management_details={};
var isImportSchema=false;
var selected_decison_element='';
var selected_element_name = '';
var list_model_repeatable_heirarchical_elements={};
var map_model_repeatable_heirarchical_elements={};
var serviceName=null;
var workspaceType='env';
var environment_selected_file_id ='';
var old_new_model_name={};
var isModelRenamed = false;
var isModelfrmClick = false;
var autoSaveRevision =-1;
var commandStackList = [];

var defaults_props=null
var elementMap={}
var lastElementSelected=null
var isTemplate=null;
var vf_Services=null;
var asdc_Services=null;
var readOnly=false;//for when the user select read only on clamp app
var runningInstances={}
