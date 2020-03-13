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

import React from 'react'
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Select from 'react-select';
import Modal from 'react-bootstrap/Modal';
import styled from 'styled-components';
import LoopService from '../../../api/LoopService';
import JSONEditor from '@json-editor/json-editor';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

export default class PolicyModal extends React.Component {

	state = {
		show: true,
		loopCache: this.props.loopCache,
		jsonEditor: null,
		policyName: this.props.match.params.policyName,
		// This is to indicate whether it's an operational or config policy (in terms of loop instance)
		policyInstanceType: this.props.match.params.policyInstanceType,
		pdpGroup: null,
		pdpGroupList: [],
		pdpSubgroupList: [],
		chosenPdpGroup: '',
		chosenPdpSubgroup: ''
	};

	constructor(props, context) {
		super(props, context);
		this.handleClose = this.handleClose.bind(this);
		this.handleSave = this.handleSave.bind(this);
		this.renderJsonEditor = this.renderJsonEditor.bind(this);
		this.handlePdpGroupChange = this.handlePdpGroupChange.bind(this);
		this.handlePdpSubgroupChange = this.handlePdpSubgroupChange.bind(this);
		this.createJsonEditor = this.createJsonEditor.bind(this);
	}

	handleSave() {
		var errors = this.state.jsonEditor.validate();
		var editorData = this.state.jsonEditor.getValue();

		if (errors.length !== 0) {
			console.error("Errors detected during policy data validation ", errors);
			return;
		}
		else {
			console.info("NO validation errors found in policy data");
			if (this.state.policyInstanceType === 'MICRO-SERVICE-POLICY') {
                this.state.loopCache.updateMicroServiceProperties(this.state.policyName, editorData);
                this.state.loopCache.updateMicroServicePdpGroup(this.state.policyName, this.state.chosenPdpGroup, this.state.chosenPdpSubgroup);
                LoopService.setMicroServiceProperties(this.state.loopCache.getLoopName(), this.state.loopCache.getMicroServiceForName(this.state.policyName)).then(resp => {
                    this.setState({ show: false });
                    this.props.history.push('/');
                    this.props.loadLoopFunction(this.state.loopCache.getLoopName());
                });
			} else if (this.state.policyInstanceType === 'OPERATIONAL-POLICY') {
				this.state.loopCache.updateOperationalPolicyProperties(this.state.policyName, editorData);
				this.state.loopCache.updateOperationalPolicyPdpGroup(this.state.policyName, this.state.chosenPdpGroup, this.state.chosenPdpSubgroup);
				LoopService.setOperationalPolicyProperties(this.state.loopCache.getLoopName(), this.state.loopCache.getOperationalPolicies()).then(resp => {
					this.setState({ show: false });
				this.props.history.push('/');
					this.props.loadLoopFunction(this.state.loopCache.getLoopName());
				});
			}
		}
	}

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/');
	}

	componentDidMount() {
		this.renderJsonEditor();
	}

    createJsonEditor(toscaModel, editorData) {
        return new JSONEditor(document.getElementById("editor"),
        {   schema: toscaModel,
              startval: editorData,
              theme: 'bootstrap4',
              object_layout: 'grid',
              disable_properties: true,
              disable_edit_json: false,
              disable_array_reorder: true,
              disable_array_delete_last_row: true,
              disable_array_delete_all_rows: false,
              array_controls_top: true,
              keep_oneof_values: false,
              collapsed:true,
              show_errors: 'always',
              display_required_only: false,
              show_opt_in: true,
              prompt_before_delete: true,
              required_by_default: false,
              array_controls_top: true
        })
    }

	renderJsonEditor() {
		console.debug("Rendering PolicyModal ", this.state.policyName);
		var toscaModel = {};
		var editorData = {};
		var pdpGroupValues = {};
		var chosenPdpGroupValue, chosenPdpSubgroupValue;
		if (this.state.policyInstanceType === 'MICRO-SERVICE-POLICY') {
			toscaModel = this.state.loopCache.getMicroServiceJsonRepresentationForName(this.state.policyName);
			editorData = this.state.loopCache.getMicroServicePropertiesForName(this.state.policyName);
			pdpGroupValues = this.state.loopCache.getMicroServiceSupportedPdpGroup(this.state.policyName);
			chosenPdpGroupValue = this.state.loopCache.getMicroServicePdpGroup(this.state.policyName);
			chosenPdpSubgroupValue = this.state.loopCache.getMicroServicePdpSubgroup(this.state.policyName);
		} else if (this.state.policyInstanceType === 'OPERATIONAL-POLICY') {
			toscaModel = this.state.loopCache.getOperationalPolicyJsonRepresentationForName(this.state.policyName);
			editorData = this.state.loopCache.getOperationalPolicyPropertiesForName(this.state.policyName);
			pdpGroupValues = this.state.loopCache.getOperationalPolicySupportedPdpGroup(this.state.policyName);
			chosenPdpGroupValue = this.state.loopCache.getOperationalPolicyPdpGroup(this.state.policyName);
			chosenPdpSubgroupValue = this.state.loopCache.getOperationalPolicyPdpSubgroup(this.state.policyName);
		}

		if (toscaModel == null) {
			return;
		}

        var pdpSubgroupValues = [];
		if (typeof(chosenPdpGroupValue) !== "undefined") {
			var selectedPdpGroup =	pdpGroupValues.filter(entry => (Object.keys(entry)[0] === chosenPdpGroupValue));
			pdpSubgroupValues = selectedPdpGroup[0][chosenPdpGroupValue].map((pdpSubgroup) => { return { label: pdpSubgroup, value: pdpSubgroup } });
		}
		this.setState({
        				jsonEditor: this.createJsonEditor(toscaModel,editorData),
        				pdpGroup: pdpGroupValues,
        				pdpGroupList: pdpGroupValues.map(entry => {
                                      				return { label: Object.keys(entry)[0], value: Object.keys(entry)[0] };
                                      		}),
        				pdpSubgroupList: pdpSubgroupValues,
        				chosenPdpGroup: chosenPdpGroupValue,
        				chosenPdpSubgroup: chosenPdpSubgroupValue
        			})
	}

	handlePdpGroupChange(e) {
		var selectedPdpGroup =	this.state.pdpGroup.filter(entry => (Object.keys(entry)[0] === e.value));
		const pdpSubgroupValues = selectedPdpGroup[0][e.value].map((pdpSubgroup) => { return { label: pdpSubgroup, value: pdpSubgroup } });
		if (this.state.chosenPdpGroup !== e.value) {
			this.setState({
				chosenPdpGroup: e.value,
				chosenPdpSubgroup: '',
				pdpSubgroupList: pdpSubgroupValues
			});
		}
	}

	handlePdpSubgroupChange(e) {
		this.setState({ chosenPdpSubgroup: e.value });
	}

	render() {
		return (
			<ModalStyled size="xl" show={this.state.show} onHide={this.handleClose}>
				<Modal.Header closeButton>
					<Modal.Title>Edit the policy</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<div id="editor" />
					<Form.Group as={Row} controlId="formPlaintextEmail">
						<Form.Label column sm="2">Pdp Group Info</Form.Label>
						<Col sm="3">
							<Select value={{ label: this.state.chosenPdpGroup, value: this.state.chosenPdpGroup }} onChange={this.handlePdpGroupChange} options={this.state.pdpGroupList} />
						</Col>
						<Col sm="3">
							<Select value={{ label: this.state.chosenPdpSubgroup, value: this.state.chosenPdpSubgroup }} onChange={this.handlePdpSubgroupChange} options={this.state.pdpSubgroupList} />
						</Col>
					</Form.Group>
				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" onClick={this.handleClose}>
						Close
					</Button>
					<Button variant="primary" onClick={this.handleSave}>
						Save Changes
					</Button>
				</Modal.Footer>
			</ModalStyled>

		);
	}
}