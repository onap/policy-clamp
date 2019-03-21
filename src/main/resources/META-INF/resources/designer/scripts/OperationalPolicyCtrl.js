/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
app
.controller(
'operationalPolicyCtrl',
[
    '$scope',
    '$rootScope',
    '$uibModalInstance',
    'data',
    'operationalPolicyService',
    'dialogs',
    function($scope, $rootScope, $uibModalInstance, data, operationalPolicyService, dialogs) {

	    console.log("//////operationalPolicyCtrl");
	    var parent_policy = {}
	    var policy_ids = {}
	    var loadingId = false;
	    var allPolicies = {};
	    var allPolIds = [];
	    
	    $scope.guardType="GUARD_MIN_MAX";
	    $scope.targetResource="";
	    function getAllFormId() {

		    return Array.from(document.getElementsByClassName("formId"));
	    }
	    function searchActiveFormId() {

		    var formArray = getAllFormId();
		    for (var i = 0, max = formArray.length; i < max; i++) {
			    console.log("Search active FormId, current element " + formArray[i].id);
			    if (formArray[i].style.display !== "none") {
				    console.log("Active form is:" + formArray[i].id);
				    return formArray[i];
			    }
		    }
		    console.log("No active formId found !");
	    }
	    function initTargetResourceId() {

		    if (vf_Services !== null && vf_Services !== undefined) {
			    // Set all the Resource Invariant UUID in the target resource ID
			    // list (+Empty and other)
			    Object.keys(vf_Services["shared"]["byVf"]).forEach(function(key) {

				    $("#targetResourceId").append($('<option></option>').val(key).html(key));
			    });
		    }
	    }
	    // load recipes for a chosen policy
	    function disperseConfig(policyObj, id) {

		    console.log("disperseConfig with id:" + id);
		    parent_policy = {};
		    // remove old gui forms
		    for (var i = 1; i < ($(".formId").length + 1); i++) {
			    $("#go_properties_tab" + i).parent().remove();
		    }
		    $(".formId").remove();
		    if (policyObj !== undefined && policyObj[id] !== undefined) {
			    var el = policyObj[id][3]['policyConfigurations']
			    for (var i = 0; i < el.length; i++) {
				    loadingId = true;
				    var num = add_one_more();
				    console.log("number is=:" + num);
				    loadingId = false;
				    for (var j = 0; j < el[i].length; j++) {
					    console.log("attr:" + el[i][j].name + "; value is:" + el[i][j].value);
					    if (el[i][j].hasOwnProperty("name")) {
						    $("#formId" + num + " #" + el[i][j].name).val(el[i][j].value);
						    if (el[i][j].name === "_id") {
							    console.log("formId num:" + num + "; value is:" + el[i][j].value);
							    policy_ids["#formId" + num] = el[i][j].value
						    }
						    if (el[i][j].name === 'parentPolicy')
							    parent_policy[num] = el[i][j].value
						    if (el[i][j].name === 'recipe' && el[i][j].value.toString() !== '') {
							    $("#go_properties_tab" + num).text(el[i][j].value)
						    }
						    if (el[i][j].name === "targetResourceIdOther" && el[i][j].value.toString() !== '') {
							    // Add the entry and set it
							    $("#formId" + num + " #targetResourceId").append(
							    $('<option></option>').val($("#formId" + num + " #targetResourceIdOther").val()).html(
							    $("#formId" + num + " #targetResourceIdOther").val()));
							    $("#formId" + num + " #targetResourceId").val(
							    $("#formId" + num + " #targetResourceIdOther").val());
						    }
						    $scope.changeGuardPolicyType();
					    }
				    }
			    }
			    // Adding all the ids for parent policy options
			    for (var i = 1; i <= $(".formId").length; i++) {
				    for (k in policy_ids) {
					    if ($("#formId" + i + " #_id").val() !== policy_ids[k].toString()
					    && $(k + " #recipe").val() !== undefined && $(k + " #recipe").val() !== "") {
						    $("#formId" + i + " #parentPolicy").append(
						    "<option value=\"" + policy_ids[k] + "\">" + $(k + " #recipe").val() + "</option>");
					    }
				    }
			    }
			    for (k in parent_policy) {
				    $("#formId" + k + " #parentPolicy").val(parent_policy[k]);
				    if ($("#formId" + k + " #parentPolicy").val() == "") {
					    $("#formId" + k + " #parentPolicyConditions").multiselect("disable");
				    } else {
					    $("#formId" + k + " #parentPolicyConditions").multiselect("enable");
				    }
				    // force the change event
				    $("#formId" + k + " #parentPolicy").change();
			    }
			    // Now load all component with the right value defined in
			    // policyObj JSON
			    for (headInd in policyObj[id]) {
				    if (!(policyObj[id][headInd].hasOwnProperty("policyConfigurations"))) {
					    $("#" + policyObj[id][headInd].name).val(policyObj[id][headInd].value);
				    }
			    }
		    }
		    setMultiSelect();
		    if (readMOnly) {
			    $('select[multiple] option').each(function() {

				    var input = $('input[value="' + $(this).val() + '"]');
				    input.prop('disabled', true);
				    input.parent('li').addClass('disabled');
			    });
			    $('input[value="multiselect-all"]').prop('disabled', true).parent('li').addClass('disabled');
		    }
	    }
	    function addSelectListen(count) {

		    var onSelectChange = function() {

			    var opselected = this.selectedOptions[0].text;
			    if (this.id == "recipe") {
				    if (opselected !== "") {
					    var polCount = $(this).closest("[id^='formId']").attr("id").substring(6);
					    $(this).closest(".policyPanel").find("#go_properties_tab" + polCount).text(opselected);
				    } else {
					    $(this).closest("[id^='go_properties_tab']").text("Policy");
				    }
			    }
			    if (this.id == "parentPolicy") {
				    var ppCond = $(this).closest("[id^='formId']").find("#parentPolicyConditions");
				    if (opselected == "") {
					    ppCond.multiselect("clearSelection");
					    ppCond.multiselect("disable");
				    } else {
					    ppCond.multiselect("enable");
				    }
			    }
		    };
		    $("#formId" + count + " select").each(function() {

			    this.change = onSelectChange;
		    });
	    }
	    // This is ensure there are no repeated keys in the map
	    function noRepeats(form) {

		    // triggered per policy.
		    var select = {};
		    for (var i = 0; i < form.length; i++) {
			    if (select[form[i].name] === undefined)
				    select[form[i].name] = []
			    select[form[i].name].push(form[i].value);
		    }
		    var arr = []
		    for (s in select) {
			    var f = {}
			    f.name = s
			    f.value = select[s]
			    arr.push(f)
		    }
		    return arr
	    }
	    function add_one_more() {

		    console.log("add one more");
		    setPolicyOptions();
		    $("#nav_Tabs li").removeClass("active");
		    // FormSpan contains a block of the form that is not being
		    // displayed. We will create clones of that and add them to tabs
		    var form = $($("#formSpan").children()[0]).clone()
		    var count = 0;
		    // Each new tab will have the formId class attached to it. This way
		    // we can track how many forms we currently have out there and
		    // assign listeners to them
		    if ($(".formId").length > 0) {
			    var greatest = 0;
			    var s = $(".formId");
			    for (var i = 0; i < s.length; i++) {
				    if (parseInt($(s[i]).attr("id").substring(6)) > greatest) {
					    greatest = parseInt($(s[i]).attr("id").substring(6))
				    }
			    }
			    count = greatest + 1;
			    $("#properties_tab").append(('<span class="formId" id="formId' + count + '"></span>'));
		    } else {
			    count++;
			    $("#properties_tab").append('<span class="formId" id="formId1"></span>');
		    }
		    // $(form).find("#policyName").val("Recipe "+makid(2))
		    // TODO change up how we auto assign policyName. There could be the
		    // case where we do this and it will have repeats
		    // alert($(form).find("#_id").val())
		    // policyNameChangeListener(form)
		    $("#add_one_more")
		    .parent()
		    .before(
		    ' <li class="active"><a id="go_properties_tab'
		    + count
		    + '">Policy</a><button id="tab_close'
		    + count
		    + '" type="button" class="close tab-close-popup" aria-hidden="true" style="margin-top: -30px;margin-right: 5px">&times;</button></li>');
		    $("#formId" + count).append(form);
		    $(".formId").not($("#formId" + count)).css("display", "none")
		    addCustListen(count)
		    addSelectListen(count);
		    // This is for when the process is not loading from map but being
		    // created
		    if (!loadingId) {
			    var l = makeid()
			    $(form).find("#_id").val(l)
			    policy_ids["#formId" + count] = l
			    var answers = {}
			    for (var i = 1; i <= greatestIdNum(); i++) {
				    if ($("#formId" + i).length > 0) {
					    answers["#formId" + i + " #parentPolicy"] = $("#formId" + i + " #parentPolicy").val();
					    $("#formId" + i + " #parentPolicy").empty();
					    for (k in policy_ids) {
						    if (($("#formId" + i + " #_id").val() !== policy_ids[k].toString())
						    && $(k + " #recipe").val() !== undefined && $(k + " #recipe").val() !== "") {
							    $("#formId" + i + " #parentPolicy").append(
							    "<option value='" + policy_ids[k] + "'>" + $(k + " #recipe").val() + "</option>")
						    }
					    }
					    $("#formId" + i + " #parentPolicy").prepend("<option value=''></option>")
				    }
			    }
			    $("#formId" + count + " #parentPolicyConditions").multiselect("disable");
			    // re-populate parent policy with chosen options
			    for (k in answers) {
				    $(k).val(answers[k])
			    }
		    }
		    return count;
	    }
	    function addCustListen(count) {

		    $('#go_properties_tab' + count).click(function(event) {

			    $("#nav_Tabs li").removeClass("active");
			    $(this).parent().addClass("active");
			    $("#formId" + count).css("display", "")
			    $(".formId").not($("#formId" + count)).css("display", "none")
		    })
		    $('#tab_close' + count).click(
		    function(event) {

			    if (document.getElementById("timeout").disabled) {
				    return false;
			    }
			    $(this).parent().remove();
			    for (var i = 1; i <= greatestIdNum(); i++) {
				    if ($("#formId" + i).length > 0) {
					    if (i !== count) {
						    if ($("#formId" + i + " #parentPolicy").val() === $("#formId" + count + " #_id").val()
						    .toString())
							    $("#formId" + i + " #parentPolicy").val("")
						    $(
						    "#formId" + i + " #parentPolicy option[value='"
						    + $("#formId" + count + " #_id").val().toString() + "']").remove();
					    }
				    }
			    }
			    delete policy_ids["#formId" + count + " #_id"]
			    $("#formId" + count).remove();
		    })
	    }
	    function greatestIdNum() {

		    var greatest = 0;
		    var s = $(".formId");
		    for (var i = 0; i < s.length; i++) {
			    if (parseInt($(s[i]).attr("id").substring(6)) > greatest) {
				    greatest = parseInt($(s[i]).attr("id").substring(6))
			    }
		    }
		    return greatest;
	    }
	    // Generate random id for each policy
	    // Also made sure ids couldnt be repeated
	    function makeid(num) {

		    var text = "";
		    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		    if (num == null)
			    num = 7;
		    for (var i = 0; i < 7; i++)
			    text += possible.charAt(Math.floor(Math.random() * possible.length));
		    var hasValue = false;
		    for (k in policy_ids) {
			    if (text === policy_ids[k])
				    hasValue = true
		    }
		    if (hasValue)
			    return makeid(num);
		    else
			    return text
	    }
	    // var ParentPolicy = function(id, name) {
	    //
	    // this.id = id
	    // this.name = name
	    // }
	    function saveLastPolicyLocally(lastPolicyId) {

		    console.log("last policy id is:" + lastPolicyId);
		    var polForm = []
		    var properties = $(".saveProps").not("#formSpan .saveProps");
		    var timeoutForm = $("#Timeoutform").serializeArray();
		    for (var i = 0; i < timeoutForm.length; i++) {
			    polForm.push(timeoutForm[i]);
		    }
		    var d = {}
		    d["policyConfigurations"] = [];
		    for (var i = 0; i < properties.length; i++) {
			    var ser = $(properties[i]).serializeArray();
			    var s = noRepeats(ser)
			    d["policyConfigurations"].push(s);
		    }
		    polForm.push(d);
		    for ( var x in allPolicies) {
			    if (x !== lastPolicyId) {
				    delete allPolicies[x];
				    console.log("remove old policy" + x);
			    }
		    }
		    allPolicies[lastPolicyId] = polForm;
	    }
	    function startNextItem() {

		    console.log("start next Item, policyname is:" + $("#pname").val());
		    // save last item before transitioning
		    var lastItem = $("#policyTable .highlight");
		    console.log("start next Item, lastitem is:" + lastItem);
		    if (lastItem.length > 0) {
			    console.log("start next Item length > 0");
			    saveLastPolicyLocally($("#pname").val());
			    // lastItem.attr("id", $("#pname").val());
			    lastItem.find("td").html($("#pname").val());
		    }
	    }
	    function add_new_policy(issueNewNames) {

		    console.log("add new policy");
		    // remove old gui forms
		    for (var i = 1; i < ($(".formId").length + 1); i++) {
			    $("#go_properties_tab" + i).parent().remove();
		    }
		    $(".formId").remove();
		    $("#pname").val("New_Policy");
		    $("#timeout").val(getOperationalPolicyProperty().timeout);
		    $("#add_one_more").click();
	    }
	    $scope.changeTargetResourceIdOther = function() {

		    var formItemActive = searchActiveFormId();
		    if (formItemActive === undefined)
			    return;
		    if ($("#" + formItemActive.id + " #targetResourceId").val() === "Other:") {
			    $("#" + formItemActive.id + " #targetResourceIdOther").show();
		    } else {
			    $("#" + formItemActive.id + " #targetResourceIdOther").hide();
			    $("#" + formItemActive.id + " #targetResourceIdOther").val("");
		    }
	    }
	    $scope.changeGuardPolicyType = function() {

		    var formItemActive = searchActiveFormId();
		    if (formItemActive === undefined)
			    return;
		    if ($("#" + formItemActive.id + " #guardPolicyType").val() === "GUARD_MIN_MAX") {
			    $("#" + formItemActive.id + " #minMaxGuardPolicyDiv").show();
			    $("#" + formItemActive.id + " #frequencyLimiterGuardPolicyDiv").hide();
		    } else if ($("#" + formItemActive.id + " #guardPolicyType").val() === "GUARD_YAML") {
			    $("#" + formItemActive.id + " #minMaxGuardPolicyDiv").hide();
			    $("#" + formItemActive.id + " #frequencyLimiterGuardPolicyDiv").show();
		    }
	    }
	    $scope.init = function() {

		    $(function() {

			    $("#add_one_more").click(function(event) {

				    console.log("add one more");
				    event.preventDefault();
				    var num = add_one_more();
				    setMultiSelect();
			    });
			    var obj = getOperationalPolicyProperty();
			    var loadPolicy;
			    console.log("lastElementSelected :" + lastElementSelected);
			    if (!($.isEmptyObject(obj))) {
				    allPolicies = jQuery.extend({}, obj);
				    for ( var x in allPolicies) {
					    $("#policyTable").prepend("<tr><td>" + x + "</td></tr>");
					    if (allPolicies[x][1]['value']) {
						    allPolIds.push(parseInt(allPolicies[x][1]['value']));
					    }
					    console.log("policies found :" + x);
					    loadPolicy = x;
				    }
			    }
			    if (loadPolicy !== undefined && loadPolicy !== null) {
				    // load properties
				    console.log("load properties");
				    disperseConfig(allPolicies, loadPolicy);
			    } else {
				    console.log("create new policy");
				    add_new_policy();
			    }
			    $("#savePropsBtn").click(
			    function(event) {

				    console.log("save properties triggered");
				    if ($("#targetResourceIdOther").is(":visible")) {
					    $('#targetResourceId').append(
					    $('<option></option>').val($("#targetResourceIdOther").val()).html(
					    $("#targetResourceIdOther").val()))
					    $("#targetResourceId").val($("#targetResourceIdOther").val());
				    }
				    $(".idError").hide();
				    console.log("save properties triggered2");
				    startNextItem();
				    console.log("get all policies");
				    var finalSaveList = {};
				    $("#policyTable td").each(function() {

					    console.log("enter policy table each loop");
					    var tableVal = $(this).text();
					    if (tableVal in allPolicies) {
						    finalSaveList[tableVal] = allPolicies[tableVal];
					    }
					    console.log("save properties; add tableVal to policies: " + tableVal);
				    });
				    var scope = angular.element(document.getElementById('formSpan')).scope();
				    scope.submitForm(finalSaveList);
				    $("#close_button").click();
			    });
			    $('#policyTable').on('click', 'tr', function(event) {

				    console.log("click on policyTable");
				    $(".idError").hide();
				    if (!(readMOnly)) {
					    startNextItem();
				    }
				    $(this).addClass('highlight').siblings().removeClass('highlight');
				    disperseConfig(allPolicies, $(this).find("td").html());
			    });
			    $('#pname').on('keypress', function(e) {

				    /*
					 * var newVal = $(this).val() +
					 * String.fromCharCode(e.which); if ((newVal>99) ||
					 * (($(this).val().length>2) && e.keyCode != 46 && e.keyCode
					 * !=8)){ e.preventDefault(); }
					 */
				    if (e.keyCode == 32) {
					    $("#spaceError").show();
					    e.preventDefault();
				    }
			    });
			    console.log("start next Item on 796");
			    startNextItem();
			    if (("#policyTable .highlight").length > 0) {
				    $('#policyTable tr.highlight').removeClass('highlight');
			    }
			    $("#policyTable").prepend("<tr class='highlight'><td>New_Policy</td></tr>");
			    $("#pid").val(0);
			    initTargetResourceId();
			    // load metrics dropdown
			    if (elementMap["global"]) {
				    for (var i = 0; i < (elementMap["global"].length); i++) {
					    if ((elementMap["global"][i]["name"]) == "actionSet") {
						    var asSel = elementMap["global"][i]["value"];
						    if (asSel == "vnfRecipe" && vf_Services !== null && vf_Services !== undefined) {
							    if (vf_Services["policy"][asSel]) {
								    $.each((vf_Services["policy"][asSel]), function(val, text) {

									    $('#recipe').append($('<option></option>').val(val).html(text));
								    });
							    }
							    break;
						    }
					    }
				    }
			    }
		    });
	    }
	    $scope.init();
	    $scope.isNumberKey = function(event) {

		    var charCode = (event.which) ? event.which : event.keyCode
		    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
			    return false;
		    }
		    return true;
	    }
//	    setTimeout(function() {
//
//		    console.log("setTimeout");
//		    setMultiSelect();
//	    }, 100);
	    $scope.close = function() {

		    console.log("close");
		    $uibModalInstance.close("closed");
	    };
	    $scope.submitForm = function(obj) {

		    var operationalPolicies = JSON.parse(JSON.stringify(getOperationalPolicies()));
		    if (obj !== null) {
			    operationalPolicies[0]["configurationsJson"] = obj;
		    }
		    operationalPolicyService.saveOpPolicyProperties(operationalPolicies).then(function(pars) {

			    updateOpPolicyProperties(operationalPolicies);
		    }, function(data) {

		    });
	    };
    } ]);