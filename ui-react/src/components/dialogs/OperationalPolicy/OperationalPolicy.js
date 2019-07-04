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

import './OperationalPolicy.css'

class OperationalPolicy extends React.Component {
	
	constructor(props, context) {
	    super(props, context);

	    this.handleShow = this.handleShow.bind(this);
	    this.handleClose = this.handleClose.bind(this);
		this.initPolicySelect = this.initPolicySelect.bind(this);

		this.allPolicies=[];
		this.policy_ids=[];

	    this.state = {
	      show: false,
	    };
	  }

	  handleClose() {
	    this.setState({ show: false });
	  }

	  handleShow() {
	    this.setState({ show: true });
	  }

	  initPolicySelect() {
		    if (this.allPolicies['operational_policy'] === undefined || this.allPolicies['operational_policy'] === null) {
			    this.allPolicies = getOperationalPolicyProperty();
		    }
		    // Provision all policies ID first
		    if (policy_ids.length == 0 && this.allPolicies['operational_policy'] != undefined) {
			    $.each(this.allPolicies['operational_policy']['policies'], function() {
				    policy_ids.push(this['id']);
			    });
		    }
	    }

	  render() {
	    return (
	      <>
	        <Button variant="primary" onClick={this.handleShow}>
	          Launch demo modal
	        </Button>

	        <Modal size="lg" show={this.state.show} onHide={this.handleClose}>
	          <Modal.Header closeButton>
	            <Modal.Title>Modal heading</Modal.Title>
	          </Modal.Header>
	          <Modal.Body>
	          <div attribute-test="policywindowproperties" id="configure-widgets"
	        		className="disabled-block-container">
	        		<div attribute-test="policywindowpropertiesh" className="modal-header">
	        			<button type="button" className="close" onClick="close(false)"
	        				aria-hidden="true" style={{marginTop: '-3px'}}>&times;</button>
	        			<h4>Operational Policy</h4>
	        		</div>

	        		<div className="modal-body">
	        			<div attribute-test="policywindowpropertiesb" className="modal-body row">

	        				<div className="panel panel-default col-sm-10 policyPanel">
	        					<form id="operationalPolicyHeaderForm" className="form-horizontal">
	        							<div className="form-group clearfix">
	        								<label className="col-sm-2">Parent policy</label>
	        								<div className="col-sm-3" style={{padding: '0px'}}>
	        									<select type="text" id="trigger_policy" name="trigger_policy"
	        										className="form-control" ng-init="initPolicySelect()"
	        										ng-options="policy for policy in policy_ids track by policy">
	        										<option value="">-- choose an option --</option>
	        										</select>
	        								</div>

	        								<label htmlFor="timeout" className="col-sm-3"
	        									style={{paddingLeft: '5px', paddingRight: '10px'}}>Overall
	        									Time Limit</label>
	        								<div className="col-sm-2" style={{paddingLeft: '0px'}}>
	        									<input type="text" ng-pattern="/^[0-9]*$/" ng-model="number"
	        										className="form-control" id="timeout" name="timeout"/>
	        								</div>

	        								<label htmlFor="abatement" className="col-sm-2">Abatement</label>
	        								<div className="col-sm-2" style={{paddingLeft: '0px'}}>
	        									<select className="form-control" id="abatement" name="abatement" onChange={this.handleChange}>
	        	   									<option value="false">False</option>
	        	   									<option value="true">True</option>
	        									</select>
	        								</div>
	        							</div>
	        							<div className="form-group clearfix row">
	        	   						    <label className="col-sm-4 control-label" htmlFor="clname">ControlLoopName</label>
	        							    <div className="col-sm-8">
	        								    <input type="text" className="form-control" name="controlLoopName"
	        									    readOnly="readonly" id="clname" ng-model="clname"/>
	        							    </div>
	        							</div>
	        					</form>
	        					<div className="panel-heading" style={{backgroundColor: 'white'}}>
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

	        				<span id="formSpan" style={{display: 'none'}}>
	        					<form className="policyProperties" className="form-horizontal"
	        						style={{border:'2px dotted gray'}}
	        						title="Operational Policy Properties">
	        						<div className="form-group clearfix">
	        							<label className="col-sm-4 control-label" htmlFor="id">ID</label>
	        							<div className="col-sm-8">
	        								<input type="text" className="form-control"  name="id" id="id" ng-model="duplicated" ng-init="duplicated = false"
	        									ng-keyup="updateTabLabel($event)" />
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
	        						title="Operational Policy Target" style={{border: '2px dotted gray'}}>
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
	        										id="modelInvariantId" readOnly/>
	        								</div>
	        							</div>
	        							<div className="form-group clearfix">
	        								<label htmlFor="modelVersionId" className="col-sm-4 control-label">
	        									Model Version Id</label>
	        								<div className="col-sm-8">
	        									<input className="form-control" name="modelVersionId"
	        										id="modelVersionId" readOnly/>
	        								</div>
	        							</div>
	        							<div className="form-group clearfix">
	        								<label htmlFor="modelName" className="col-sm-4 control-label">
	        									Model Name</label>
	        								<div className="col-sm-8">
	        									<input className="form-control" name="modelName" id="modelName"
	        										readOnly/>
	        								</div>
	        							</div>
	        							<div className="form-group clearfix">
	        								<label htmlFor="modelVersion" className="col-sm-4 control-label">
	        									Model Version</label>
	        								<div className="col-sm-8">
	        									<input className="form-control" name="modelVersion"
	        										id="modelVersion" readOnly/>
	        								</div>
	        							</div>
	        							<div className="form-group clearfix">
	        								<label htmlFor="modelCustomizationId" className="col-sm-4 control-label">
	        									Model Customization Id</label>
	        								<div className="col-sm-8">
	        									<input className="form-control" name="modelCustomizationId"
	        										id="modelCustomizationId" readOnly/>
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
	        						title="Guard policy associated" style={{border: '2px dotted gray'}}>

	        						<div className="form-group clearfix withnote">
	        							<label className="col-sm-4 control-label" htmlFor="id">Guard Policy ID</label>
	        							<div className="col-sm-8">
	        								<input type="text" className="form-control" name="id" id="id" ng-blur="changeGuardId()" ng-model="id"/>
	        							</div>
	        						</div>
	        						<div>
	        							<label className="form-group note">Note: Prefix will be added to Guard Policy ID automatically based on Guard Policy Type</label>
	        						</div>
	        						<div className="form-group clearfix">
	        							<label className="col-sm-4 control-label" htmlFor="recipe">Recipe</label>
	        							<div className="col-sm-8">
	        								<input type="text" className="form-control" name="recipe"
	        									readOnly="readonly" id="recipe"/>
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
	        							id="frequencyLimiterGuardPolicyDiv" style={{display: 'none'}}>
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
	        									id="guardActiveStart"  value="00:00:00Z"/>
	        							</div>
	        							<label htmlFor="guardActiveEnd" className="col-sm-4 control-label">
	        								Guard Active End</label>
	        							<div className="col-sm-8">
	        								<input className="form-control" name="guardActiveEnd"
	        									id="guardActiveEnd" value="00:00:01Z"/>
	        							</div>
	        						</div>

	        					</form>

	        				</span>
	        			</div>
	        		</div>

	        		<div attribute-test="policywindowpropertiesf" className="modal-footer">
	        			<button id="savePropsBtn" className="btn btn-primary" ng-disabled="duplicated">Close</button>
	        			<button ng-click="close(true)" id="close_button"
	        				className="btn btn-primary">Cancel</button>
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
	        </Modal>
	      </>
	    );
	  }
}

export default OperationalPolicy;