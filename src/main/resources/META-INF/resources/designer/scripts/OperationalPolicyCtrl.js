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
	    $scope.policy_ids = []
	    var allPolicies = {};
	    $scope.guardType = "GUARD_MIN_MAX";
	    $scope.number = 0;
	    $scope.clname="";
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
	    function add_one_more() {

		    console.log("add one more");
		    $("#nav_Tabs li").removeClass("active");
		    // FormSpan contains a block of the form that is not being
		    // displayed. We will create clones of that and add them to tabs
		    var form = $("#formSpan").clone(true, true)
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
		    $("#add_one_more")
		    .parent()
		    .before(
		    ' <li class="active"><a id="go_properties_tab'
		    + count
		    + '">new</a><button id="tab_close'
		    + count
		    + '" type="button" class="close tab-close-popup" aria-hidden="true" style="margin-top: -30px;margin-right: 5px">&times;</button></li>');
		    $("#formId" + count).append(form.children());
		    $(".formId").not($("#formId" + count)).css("display", "none");
		    addCustListen(count);
		    $("#formId" + count + " #id").val("new");
		    return count;
	    }
	    function addCustListen(count) {

		    $('#go_properties_tab' + count).click(function(event) {

			    $("#nav_Tabs li").removeClass("active");
			    $(this).parent().addClass("active");
			    $("#formId" + count).css("display", "")
			    $(".formId").not($("#formId" + count)).css("display", "none")
		    })
		    $('#tab_close' + count).click(function(event) {

			    $(this).parent().remove();
			    $scope.policy_ids.splice($scope.policy_ids.indexOf($("#formId" + count + " #id").val()), 1);
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
	    function serializeElement(element) {

		    var o = {};
		    var a = element.serializeArray();
		    $.each(a, function() {

			    if (o[this.name]) {
				    if (!o[this.name].push) {
					    o[this.name] = [ o[this.name] ];
				    }
				    o[this.name].push(this.value || '');
			    } else {
				    o[this.name] = this.value || '';
			    }
		    });
		    return o;
	    }
	    function savePolicyLocally() {

		    var polForm = {}
		    polForm = serializeElement($("#operationalPolicyHeaderForm"));
		    var policiesArray = []
		    allPolicies['guard_policies'] = [];
		    $.each($(".formId"), function() {

			    var policyProperties = serializeElement($("#" + this.id + " .policyProperties"));
			    policyProperties["target"] = serializeElement($("#" + this.id + " .policyTarget"))
			    policiesArray.push(policyProperties);
			    // Now get the Guard
			    if ($("#" + this.id + " #enableGuardPolicy").is(':checked')) {
				    allPolicies['guard_policies'].push(serializeElement($("#" + this.id + " .guardProperties")));
			    }
		    });
		    polForm['policies'] = policiesArray;
		    allPolicies['operational_policy'] = polForm;
	    }
	    function add_new_policy() {

		    console.log("add new policy");
		    // remove old gui forms
		    for (var i = 1; i < ($(".formId").length + 1); i++) {
			    $("#go_properties_tab" + i).parent().remove();
		    }
		    $(".formId").remove();
		    $("#add_one_more").click();
	    }
	    function configureComponents(allPolicies) {

		    console.log("load properties to op policy");
		    // Set the header
		    $.each($('#operationalPolicyHeaderForm').find('.form-control'), function() {

			    $(this).val(allPolicies['operational_policy'][this.id]);
		    });
		    // Set the sub-policies
		    $.each(allPolicies['operational_policy']['policies'], function(opPolicyElemIndex, opPolicyElemValue) {

			    var formNum = add_one_more();
			    $.each($('.policyProperties').find('.form-control'), function(opPolicyPropIndex, opPolicyPropValue) {

				    $("#formId"+formNum+" .policyProperties").find("#"+opPolicyPropValue.id).val(
				    allPolicies['operational_policy']['policies'][opPolicyElemIndex][opPolicyPropValue.id]);
			    });
			    // update the current tab label
			    $("#go_properties_tab" + formNum).text(
			    allPolicies['operational_policy']['policies'][opPolicyElemIndex]['id']);
			    // Check if there is a guard set for it
			    $.each(allPolicies['guard_policies'], function(guardElemIndex, guardElemValue) {

				    if (guardElemValue.recipe === $($("#formId" + formNum + " #recipe")[0]).val()) {
					    // Found one, set all guard prop
					    $.each($('.guardProperties').find('.form-control'), function(guardPropElemIndex,guardPropElemValue) {
					    	$("#formId"+formNum+" .guardProperties").find("#"+guardPropElemValue.id).val(
						    allPolicies['guard_policies'][guardElemIndex][guardPropElemValue.id]);
					    });
					    // And finally enable the flag
					    $("#formId" + formNum + " #enableGuardPolicy").prop("checked", true);
				    }
			    });
		    });
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
	    $scope.initPolicySelect = function() {

		    if (allPolicies['operational_policy'] === undefined || allPolicies['operational_policy'] === null) {
			    allPolicies = getOperationalPolicyProperty();
		    }
		    // Provision all policies ID first
		    if ($scope.policy_ids.length == 0 && allPolicies['operational_policy'] != undefined) {
			    $.each(allPolicies['operational_policy']['policies'], function() {

				    $scope.policy_ids.push(this['id']);
			    });
		    }
	    }
	    $scope.init = function() {

		    $(function() {
		    	$scope.clname=getLoopName();
			    $("#add_one_more").click(function(event) {

				    console.log("add one more");
				    event.preventDefault();
				    $scope.policy_ids.push($("#formId" + add_one_more() + " #id").val());
			    });
			    if (allPolicies['operational_policy'] !== undefined && allPolicies['operational_policy'] !== null) {
				    // load properties
				    console.log("load properties");
				    configureComponents(allPolicies);
			    } else {
				    console.log("create new policy");
				    add_new_policy();
			    }
			    $("#savePropsBtn").click(function(event) {

				    console.log("save properties triggered");
				    savePolicyLocally();
				    angular.element(document.getElementById('formSpan')).scope().submitForm(allPolicies);
				    $("#close_button").click();
			    });
			    initTargetResourceId();
		    });
	    }
	    $scope.init();
	    $scope.updateGuardRecipe = function(event) {

		    var formNum = $(event.target).closest('.formId').attr('id').substring(6);
		    // Get the second recipe (guard one) and update it
		    $($("#formId" + formNum + " #recipe")[1]).val($(event.target).val());
	    }
	    // When we change the name of a policy
	    $scope.updateTabLabel = function(event) {

		    // update policy id structure
		    var formNum = $(event.target).closest('.formId').attr('id').substring(6);
		    $scope.policy_ids.splice($scope.policy_ids.indexOf($("#formId" + formNum + " #id").val()), 1);
		    $scope.policy_ids.push($(event.target).val());
		    // Update the tab now
		    $("#go_properties_tab" + formNum).text($(event.target).val());
	    }
	    $scope.close = function() {

		    console.log("close");
		    $uibModalInstance.close("closed");
	    };
	    $scope.submitForm = function(obj) {

		    var operationalPolicies = getOperationalPolicies();
		    if (obj !== null) {
			    operationalPolicies[0]["configurationsJson"] = obj;
		    }
		    operationalPolicyService.saveOpPolicyProperties(operationalPolicies).then(function(pars) {

			    updateOpPolicyProperties(operationalPolicies);
		    }, function(data) {

		    });
	    };
    } ]);