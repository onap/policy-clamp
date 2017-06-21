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

app.factory('Datafactory', function () {
    console.log("/////////Datafactory");

    var dbDataList = [];
    var checkBoxListActivitytestCase=[];
    var generateTstMultipleFlag="";
    var writeFileDataList=[];
    var fileStreamWriterList=[];
    var xmlValidatorList=[];
    var xmlAsserter={};
    var runtimePythonScriptList=[];
    var xmlValidatorDecisionLevel=[];
    var runtimePythonScriptProjectLevelList=[];
    var commonPythonScriptList=[];
    var dbToolProjectLevelList=[];
    var soapClientOption={};
	var selectedTestCase={};
	var executeResultset={};
	var projectPreferenceInfo={};
	var modelPreferenceInfo ={};
    

    return {
        getDbDataList: function () {
            console.log("getDbDataList");
            return dbDataList;
        },
        setDbDataList: function (dbRequestList) {
            console.log("setDbDataList");
        	dbDataList = dbRequestList;
        },
        getCheckBoxListActivitytestCase: function () {
            console.log("getCheckBoxListActivitytestCase");
            return checkBoxListActivitytestCase;
        },
        setCheckBoxListActivitytestCase: function (checkBoxListTestCase) {
            console.log("setCheckBoxListActivitytestCase");
        	checkBoxListActivitytestCase = checkBoxListTestCase;
        },
        getGenerateTstMultipleFlag: function () {
            console.log("getGenerateTstMultipleFlag");
            return generateTstMultipleFlag;
        },
        setGenerateTstMultipleFlag: function (generateTstFlag) {
            console.log("setGenerateTstMultipleFlag");
        	generateTstMultipleFlag = generateTstFlag;
        },
        getWriteFileDataList: function () {
            console.log("getWriteFileDataList");
            return writeFileDataList;
        },
        setWriteFileDataList: function (writeFileDataListData) {
            console.log("setWriteFileDataList");
        	writeFileDataList = writeFileDataListData;
        },
        getFileStreamWriterList: function () {
            console.log("getFileStreamWriterList");
            return fileStreamWriterList;
        },
        setFileStreamWriterList: function (fileStreamWriterData) {
            console.log("setFileStreamWriterList");
        	fileStreamWriterList = fileStreamWriterData;
        },
        getXmlValidatorList: function () {
            console.log("getXmlValidatorList");
            return xmlValidatorList;
        },
        setXmlValidatorList: function (xmlValidatorListData) {
            console.log("setXmlValidatorList");
        	xmlValidatorList = xmlValidatorListData;
        },
        getXmlAsserter: function () {
            console.log("getXmlAsserter");
            return xmlAsserter;
        },
        setXmlAsserter: function (xmlAsserterData) {
            console.log("setXmlAsserter");
        	xmlAsserter = xmlAsserterData;
        },
        getRuntimePythonScriptList: function () {
            console.log("getRuntimePythonScriptList");
            return runtimePythonScriptList;
        },
        setRuntimePythonScriptList: function (runtimePythonScriptListData) {
            console.log("setRuntimePythonScriptList");
        	runtimePythonScriptList = runtimePythonScriptListData;
        },
        getXmlValidatorDecisionLevel: function(){
            console.log("getXmlValidatorDecisionLevel");
        	return xmlValidatorDecisionLevel;
        },
        setXmlValidatorDecisionLevel:function(xmlValidatorDecisionLevelData){
            console.log("setXmlValidatorDecisionLevel");
        	xmlValidatorDecisionLevel=xmlValidatorDecisionLevelData;
        },
        getRuntimePythonScriptProjectLevelList: function () {
            console.log("getRuntimePythonScriptProjectLevelList");
            return runtimePythonScriptProjectLevelList;
        },
        setRuntimePythonScriptProjectLevelList: function (runtimePythonScriptListData) {
            console.log("setRuntimePythonScriptProjectLevelList");
        	runtimePythonScriptProjectLevelList = runtimePythonScriptListData;
        },
        
        getCommonPythonScriptList: function () {
            console.log("getCommonPythonScriptList");
            return commonPythonScriptList;
        },
        setCommonPythonScriptList: function (commonPythonScriptListData) {
            console.log("setCommonPythonScriptList");
        	commonPythonScriptList = commonPythonScriptListData;
        },
        
        getDbToolProjectLevelList: function () {
            console.log("getDbToolProjectLevelList");
            return dbToolProjectLevelList;
        },
        setDbToolProjectLevelList: function (dbToolProjectLevelListData) {
            console.log("setDbToolProjectLevelList");
        	dbToolProjectLevelList = dbToolProjectLevelListData;
        },
        getSoapClientOption: function () {
            console.log("getSoapClientOption");
            return soapClientOption;
        },
        setSoapClientOption: function (soapClientOptionData) {
            console.log("setSoapClientOption");
        	soapClientOption = soapClientOptionData;
        },
		getSelectedTestCase: function () {
            console.log("getSelectedTestCase");
            return selectedTestCase;
        },
        setSelectedTestCase: function (selectedTestCaseData) {
            console.log("setSelectedTestCase");
        	selectedTestCase = selectedTestCaseData;
        },
        
        getExecuteResultset: function () {
            console.log("getExecuteResultset");
            return executeResultset;
        },
        setExecuteResultset: function (executeResultsetData) {
            console.log("setExecuteResultset");
        	executeResultset = executeResultsetData;
        },
        
        getProjectPreferenceInfo: function () {
            console.log("getProjectPreferenceInfo");
            return projectPreferenceInfo;
        },
        setProjectPreferenceInfo: function (projectPreferenceInfoData) {
            console.log("setProjectPreferenceInfo");
        	projectPreferenceInfo = projectPreferenceInfoData;
        },
        
        getModelPreferenceInfo: function () {
            console.log("getModelPreferenceInfo");
            return modelPreferenceInfo;
        },
        setModelPreferenceInfo: function (modelPreferenceInfoData) {
            console.log("setModelPreferenceInfo");
        	modelPreferenceInfo = modelPreferenceInfoData;
        }
        
        
    };
});
