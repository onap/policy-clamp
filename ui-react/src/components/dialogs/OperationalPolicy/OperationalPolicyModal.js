/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

import React from 'react'
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import LoopCache from '../../backend_communication/LoopCache'
import './OperationalPolicy.css'
import styled from 'styled-components';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

export default class OperationalPolicyModal extends React.Component {

	constructor(props, context) {
		super(props, context);

		this.loopCache = new LoopCache();

		this.handleClose = this.handleClose.bind(this);
		this.initPolicySelect = this.initPolicySelect.bind(this);

		this.allPolicies = [];
		this.policyIds = [];

		this.initPolicySelect();

		this.state = {
			show: true,
		};

	}

	handleClose() {
		this.setState({ show: false });
	}

	initPolicySelect() {
		if (this.allPolicies['operational_policy'] === undefined || this.allPolicies['operational_policy'] === null) {
			this.allPolicies = this.loopCache.getOperationalPolicyProperty();
		}
		// Provision all policies ID first
		if (this.policyIds.length == 0 && this.allPolicies['operational_policy'] != undefined) {

			for (let i = 0; i < this.allPolicies['operational_policy']['policies'].length; i++) {
				this.policyIds.push(this.allPolicies['operational_policy']['policies'][i]['id']);
			}
		}
	}

	renderPolicyIdSelect() {
		return (
			<select type="text" id="trigger_policy" name="trigger_policy"
				className="form-control">
				<option value="">-- choose an option --</option>
				{this.policyIds.map(policyId => (<option key={policyId}>{policyId}</option>))}
			</select>
		);
	}

	serializeElement(element) {
		var o = {};
		element.serializeArray().forEach(function () {
			if (o[this.name]) {
				if (!o[this.name].push) {
					o[this.name] = [o[this.name]];
				}
				o[this.name].push(this.value || '');
			} else {
				o[this.name] = this.value || '';
			}
		});
		return o;
	}

	// When we change the name of a policy
	isDuplicatedId (event) {
		// update policy id structure
		var formNum = document.getElementById(event.target).closest('.formId').attr('id').substring(6);
		var policyId = document.getElementById(event.target).val();
		if (this.policyIds.includes(policyId)) {
			console.log("Duplicated ID, cannot proceed");
			return true;
		} else {
			this.duplicated = false;
			this.policyIds.splice(this.policyIds.indexOf(document.getElementById("#formId" + formNum + " #id").val()), 1);
			this.policyIds.push(document.getElementById(event.target).val());
			// Update the tab now
			document.getElementById("#go_properties_tab" + formNum).text(document.getElementById(event.target).val());
		}
	}

	configureComponents() {
		    console.log("Load properties to op policy");
		    // Set the header
		    document.getElementsByClassName('form-control').forEach(function() {
			    this.val(this.allPolicies['operational_policy']['controlLoop'][this.id]);
		    });
		    // Set the sub-policies
		    this.allPolicies['operational_policy']['policies'].forEach(function(opPolicyElemIndex, opPolicyElemValue) {

/*			    var formNum = add_one_more();
			    forEach(document.getElementsByClassName('policyProperties').find('.form-control'), function(opPolicyPropIndex, opPolicyPropValue) {

				    $("#formId" + formNum + " .policyProperties").find("#" + opPolicyPropValue.id).val(
				    allPolicies['operational_policy']['policies'][opPolicyElemIndex][opPolicyPropValue.id]);
			    });

			    // Initial TargetResourceId options
			    initTargetResourceIdOptions(allPolicies['operational_policy']['policies'][opPolicyElemIndex]['target']['type'], formNum);
			    $.each($('.policyTarget').find('.form-control'), function(opPolicyTargetPropIndex, opPolicyTargetPropValue) {

				    $("#formId" + formNum + " .policyTarget").find("#" + opPolicyTargetPropValue.id).val(
				    allPolicies['operational_policy']['policies'][opPolicyElemIndex]['target'][opPolicyTargetPropValue.id]);
			    });

			    // update the current tab label
			    $("#go_properties_tab" + formNum).text(
			    allPolicies['operational_policy']['policies'][opPolicyElemIndex]['id']);
			    // Check if there is a guard set for it
			    $.each(allPolicies['guard_policies'], function(guardElemId, guardElemValue) {

				    if (guardElemValue.recipe === $($("#formId" + formNum + " #recipe")[0]).val()) {
					    // Found one, set all guard prop
					    $.each($('.guardProperties').find('.form-control'), function(guardPropElemIndex,
					                                                                 guardPropElemValue) {

						    guardElemValue['id'] = guardElemId;
						    $("#formId" + formNum + " .guardProperties").find("#" + guardPropElemValue.id).val(
						    guardElemValue[guardPropElemValue.id]);
					    });
					    iniGuardPolicyType(guardElemId, formNum);
					    // And finally enable the flag
					    $("#formId" + formNum + " #enableGuardPolicy").prop("checked", true);
				    }
			    });*/
			});
	    }

	render() {
		return (
			<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose}>
				<Modal.Header closeButton>
					<Modal.Title>Operational policies</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<div attribute-test="policywindowproperties" id="configure-widgets"
						className="disabled-block-container">
						<div attribute-test="policywindowpropertiesb" className="modal-body row">
							<div className="panel panel-default col-sm-10 policyPanel">
								<form id="operationalPolicyHeaderForm" className="form-horizontal">
									<div className="form-group clearfix">
										<label className="col-sm-2">Parent policy</label>
										<div className="col-sm-3" style={{ padding: '0px' }}>
											{this.renderPolicyIdSelect()}
										</div>

										<label htmlFor="timeout" className="col-sm-3"
											style={{ paddingLeft: '5px', paddingRight: '10px' }}>Overall
	        									Time Limit</label>
										<div className="col-sm-2" style={{ paddingLeft: '0px' }}>
											<input type="text" ng-pattern="/^[0-9]*$/" ng-model="number"
												className="form-control" id="timeout" name="timeout" />
										</div>

										<label htmlFor="abatement" className="col-sm-2">Abatement</label>
										<div className="col-sm-2" style={{ paddingLeft: '0px' }}>
											<select className="form-control" id="abatement" name="abatement">
												<option value="false">False</option>
												<option value="true">True</option>
											</select>
										</div>
									</div>
									<div className="form-group clearfix row">
										<label className="col-sm-4 control-label" htmlFor="clname">ControlLoopName</label>
										<div className="col-sm-8">
											<input type="text" className="form-control" name="controlLoopName"
												readOnly="readonly" id="clname" value={this.loopCache.getLoopName()} />
										</div>
									</div>
								</form>
								<div className="panel-heading" style={{ backgroundColor: 'white' }}>
									<ul id="nav_Tabs" className="nav nav-tabs">
										<li>
											<a id="add_one_more" href="#desc_tab">
												<span
													className="glyphicon glyphicon-plus" aria-hidden="true">
												</span>
											</a>
										</li>
									</ul>
								</div>
								<div className="panel-body">
									<div className="tab-content">
										<div id="properties_tab" className="tab-pane fade in active"></div>
									</div>
								</div>
							</div>

							<span id="formSpan" style={{ display: 'none' }}>
								<form className="policyProperties" className="form-horizontal"
									style={{ border: '2px dotted gray' }}
									title="Operational Policy Properties">
									<div className="form-group clearfix">
										<label className="col-sm-4 control-label" htmlFor="id">ID</label>
										<div className="col-sm-8">
											<input type="text" className="form-control" name="id" id="id"
										onKeyUp="updateTabLabel($event)" />
											<span >ID must be unique</span>
										</div>
									</div>
									<div className="form-group clearfix">
										<label className="col-sm-4 control-label" htmlFor="recipe">Recipe</label>
										<div className="col-sm-8">
											<select className="form-control" name="recipe" id="recipe"
												ng-model="recipe" ng-click="updateGuardRecipe($event)">
												<option value="">-- choose an option --</option>
												<option value="Restart">Restart</option>
												<option value="Rebuild">Rebuild</option>
												<option value="Migrate">Migrate</option>
												<option value="Health-Check">Health-Check</option>
												<option value="ModifyConfig">ModifyConfig</option>
												<option value="VF Module Create">VF Module Create</option>
												<option value="VF Module Delete">VF Module Delete</option>
												<option value="Reroute">Reroute</option>
											</select>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="retry" className="col-sm-4 control-label"> Retry</label>
										<div className="col-sm-8">
											<input type="text" maxLength="5" className="form-control" id="retry"
												ng-pattern="/^[0-9]*$/" ng-model="number" name="retry">
											</input>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="timeout" className="col-sm-4 control-label">
											Timeout</label>
										<div className="col-sm-8">
											<input type="text" maxLength="5" className="form-control"
												id="timeout" ng-pattern="/^[0-9]*$/" ng-model="number"
												name="timeout"></input>
										</div>
									</div>

									<div className="form-group clearfix">
										<label htmlFor="actor" className="col-sm-4 control-label"> Actor</label>
										<div className="col-sm-8">
											<select className="form-control" id="actor" name="actor" ng-click="updateGuardActor($event)" ng-model="actor">
												<option value="">-- choose an option --</option>
												<option value="APPC">APPC</option>
												<option value="SO">SO</option>
												<option value="VFC">VFC</option>
												<option value="SDNC">SDNC</option>°
	        									<option value="SDNR">SDNR</option>°
	        								</select>
										</div>

										<label htmlFor="payload" className="col-sm-4 control-label">
											Payload (YAML)</label>
										<div className="col-sm-8">
											<textarea className="form-control" id="payload" name="payload"></textarea>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="success" className="col-sm-4 control-label">When
	        								Success</label>
										<div className="col-sm-8">
											<select className="form-control" id="success" name="success"
												ng-model="null_dump"
												ng-options="policy for policy in policy_ids track by policy">
												<option value="">-- choose an option --</option>
											</select>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="failure" className="col-sm-4 control-label">When
	        								Failure</label>
										<div className="col-sm-8">
											<select className="form-control" id="failure" name="failure"
												ng-model="null_dump"
												ng-options="policy for policy in policy_ids track by policy">
												<option value="">-- choose an option --</option>
											</select>

										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="failure_timeout" className="col-sm-4 control-label">When
	        								Failure Timeout</label>
										<div className="col-sm-8">
											<select className="form-control" id="failure_timeout"
												name="failure_timeout" ng-model="null_dump"
												ng-options="policy for policy in policy_ids track by policy">
												<option value="">-- choose an option --</option>
											</select>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="failure_retries" className="col-sm-4 control-label">When
	        								Failure Retries</label>
										<div className="col-sm-8">
											<select className="form-control" id="failure_retries"
												name="failure_retries" ng-model="null_dump"
												ng-options="policy for policy in policy_ids track by policy">
												<option value="">-- choose an option --</option>
											</select>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="failure_exception" className="col-sm-4 control-label">When
	        								Failure Exception</label>
										<div className="col-sm-8">
											<select className="form-control" id="failure_exception"
												name="failure_exception" ng-model="null_dump"
												ng-options="policy for policy in policy_ids track by policy">
												<option value="">-- choose an option --</option>
											</select>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="failure_guard" className="col-sm-4 control-label">When
	        								Failure Guard</label>
										<div className="col-sm-8">
											<select className="form-control" id="failure_guard"
												name="failure_guard" ng-model="null_dump"
												ng-options="policy for policy in policy_ids track by policy">
												<option value="">-- choose an option --</option>
											</select>
										</div>
									</div>
								</form>
								<form className="policyTarget" className="form-horizontal"
									title="Operational Policy Target" style={{ border: '2px dotted gray' }}>
									<div className="form-group clearfix">
										<label htmlFor="type" className="col-sm-4 control-label"> Target
	        								Type</label>
										<div className="col-sm-8">
											<select className="form-control" name="type" id="type"
												ng-click="initTargetResourceId($event)" ng-model="type">
												<option value="">-- choose an option --</option>
												<option value="VFMODULE">VFMODULE</option>
												<option value="VM">VM</option>
												<option value="VNF">VNF</option>
											</select>
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="resourceID" className="col-sm-4 control-label">
											Target ResourceId</label>
										<div className="col-sm-8">
											<select className="form-control" name="resourceID" id="resourceID"
												ng-click="changeTargetResourceId($event)"
												ng-model="resourceId">
												<option value="">-- choose an option --</option>
											</select>
										</div>
									</div>
									<div id="metadata">
										<div className="form-group clearfix">
											<label htmlFor="modelInvariantId" className="col-sm-4 control-label">
												Model Invariant Id</label>
											<div className="col-sm-8">
												<input className="form-control" name="modelInvariantId"
													id="modelInvariantId" readOnly />
											</div>
										</div>
										<div className="form-group clearfix">
											<label htmlFor="modelVersionId" className="col-sm-4 control-label">
												Model Version Id</label>
											<div className="col-sm-8">
												<input className="form-control" name="modelVersionId"
													id="modelVersionId" readOnly />
											</div>
										</div>
										<div className="form-group clearfix">
											<label htmlFor="modelName" className="col-sm-4 control-label">
												Model Name</label>
											<div className="col-sm-8">
												<input className="form-control" name="modelName" id="modelName"
													readOnly />
											</div>
										</div>
										<div className="form-group clearfix">
											<label htmlFor="modelVersion" className="col-sm-4 control-label">
												Model Version</label>
											<div className="col-sm-8">
												<input className="form-control" name="modelVersion"
													id="modelVersion" readOnly />
											</div>
										</div>
										<div className="form-group clearfix">
											<label htmlFor="modelCustomizationId" className="col-sm-4 control-label">
												Model Customization Id</label>
											<div className="col-sm-8">
												<input className="form-control" name="modelCustomizationId"
													id="modelCustomizationId" readOnly />
											</div>
										</div>
									</div>
								</form>
								<div className="form-group clearfix">
									<label htmlFor="enableGuardPolicy" className="col-sm-4 control-label">
										Enable Guard Policy</label>
									<div className="col-sm-8">
										<input type="checkbox" className="form-control"
											name="enableGuardPolicy" id="enableGuardPolicy" />
									</div>

									<div className="col-sm-8">
										<label htmlFor="guardPolicyType" className="col-sm-4 control-label">
											Guard Policy Type</label> <select className="form-control"
												name="guardPolicyType" id="guardPolicyType"
												ng-click="changeGuardPolicyType()" ng-model="guardType">
											<option value="GUARD_MIN_MAX">MinMax</option>
											<option value="GUARD_YAML">FrequencyLimiter</option>
										</select>
									</div>
								</div>
								<form className="guardProperties" className="form-horizontal"
									title="Guard policy associated" style={{ border: '2px dotted gray' }}>

									<div className="form-group clearfix withnote">
										<label className="col-sm-4 control-label" htmlFor="id">Guard Policy ID</label>
										<div className="col-sm-8">
											<input type="text" className="form-control" name="id" id="id" ng-blur="changeGuardId()" ng-model="id" />
										</div>
									</div>
									<div>
										<label className="form-group note">Note: Prefix will be added to Guard Policy ID automatically based on Guard Policy Type</label>
									</div>
									<div className="form-group clearfix">
										<label className="col-sm-4 control-label" htmlFor="recipe">Recipe</label>
										<div className="col-sm-8">
											<input type="text" className="form-control" name="recipe"
												readOnly="readonly" id="recipe" />
										</div>
									</div>
									<div className="form-group clearfix">
										<label className="col-sm-4 control-label" htmlFor="clname">ControlLoopName</label>
										<div className="col-sm-8">
											<input type="text" className="form-control" name="clname"
												readOnly="readonly" id="clname" ng-model="clname" />
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="actor" className="col-sm-4 control-label">Actor</label>
										<div className="col-sm-8">
											<input type="text" className="form-control" name="actor"
												readOnly="readonly" id="actor" />
										</div>
									</div>
									<div className="form-group clearfix">

										<label htmlFor="targets" className="col-sm-4 control-label">Guard
	        								targets</label>
										<div className="col-sm-8">
											<input className="form-control" name="targets" id="targets" />
										</div>
									</div>

									<div className="form-group clearfix" id="minMaxGuardPolicyDiv">
										<label htmlFor="min" className="col-sm-4 control-label"> Min
	        								Guard</label>
										<div className="col-sm-8">
											<input className="form-control" name="min" id="min" />
										</div>
										<label htmlFor="max" className="col-sm-4 control-label"> Max
	        								Guard</label>
										<div className="col-sm-8">
											<input className="form-control" name="max" id="max" />
										</div>
									</div>
									<div className="form-group clearfix"
										id="frequencyLimiterGuardPolicyDiv" style={{ display: 'none' }}>
										<label htmlFor="limit" className="col-sm-4 control-label">Limit</label>
										<div className="col-sm-8">
											<input className="form-control" name="limit" id="limit" />
										</div>
										<label htmlFor="timeUnits" className="col-sm-4 control-label">Time Units</label>
										<div className="col-sm-8">
											<select className="form-control" name="timeUnits"
												id="timeUnits">
												<option value=""></option>
												<option value="minute">minute</option>
												<option value="hour">hour</option>
												<option value="day">day</option>
												<option value="week">week</option>
												<option value="month">month</option>
												<option value="year">year</option>
											</select>
										</div>
										<label htmlFor="timeWindow" className="col-sm-4 control-label">Time	Window</label>
										<div className="col-sm-8">
											<input className="form-control" name="timeWindow" id="timeWindow" />
										</div>
									</div>
									<div className="form-group clearfix">
										<label htmlFor="guardActiveStart" className="col-sm-4 control-label">
											Guard Active Start</label>
										<div className="col-sm-8">
											<input className="form-control" name="guardActiveStart"
												id="guardActiveStart" value="00:00:00Z" />
										</div>
										<label htmlFor="guardActiveEnd" className="col-sm-4 control-label">
											Guard Active End</label>
										<div className="col-sm-8">
											<input className="form-control" name="guardActiveEnd"
												id="guardActiveEnd" value="00:00:01Z" />
										</div>
									</div>

								</form>

							</span>
						</div>
					</div>
		
				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" onClick={this.handleClose}>
						Close
	            </Button>
					<Button variant="primary" onClick={this.handleClose}>
						Save Changes
	            </Button>
				</Modal.Footer>
			</ModalStyled>

		);
	}
}
