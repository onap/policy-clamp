/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
import Form from 'react-bootstrap/Form';
import styled from 'styled-components';
import LoopService from '../../api/LoopService';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

export default class LoopProperties extends React.Component {

	formProps = {dcaeDeployParameters: {
			"location_id": "",
			"service_id": "",
			"policy_id": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca"
	}};

	constructor(props, context) {
		super(props, context);

		this.handleClose = this.handleClose.bind(this);
		this.handleChange = this.handleChange.bind(this);
		this.saveProperties =  this.saveProperties.bind(this);
		this.initialValues = this.initialValues.bind(this);
		this.state = {
			show: true,
			loopName: 'LOOP_h2NMX_v1_0_ResourceInstanceName1_tca',
			form: this.formProps
		};

	}
	initialValues() {
			const updatedForm = this.state.form;
			Object.keys(updatedForm).forEach((key) => {
			if (key === 'dcaeDeployParameters') {
				updatedForm[key] = JSON.stringify(this.state.form[key]);
			} else {
				updatedForm[key] = this.state.form[key];
			}
			this.setState({ form: updatedForm });
		});
	}
	handleClose() {
			this.props.history.push('/');
	}
	handleChange(e) {
			const formUpdated =  this.state.form;
			formUpdated[e.target.name] = e.target.value;
			this.setState({ form: formUpdated });
   };
	saveProperties(event) {
		// translate the deploymentParameter into jsonFormat at submit time
		const updatedForm = this.state.form;
		Object.keys(updatedForm).forEach((key) => {
			if (key === 'dcaeDeployParameters') {
				try {
				  let value = JSON.parse(updatedForm[key]);
					updatedForm[key] = value;
					// save Properties
					this.setState( {form: updatedForm} );
					LoopService.updateGlobalProperties(this.state.loopName, this.state.form);
					this.props.history.push('/');
				} catch (error) {
				  alert("Deployment Parameters is not in good Json format. Please correct it.");
				}
			}
		});
	}
	render() {
		return (
			<ModalStyled size="lg"  show={this.state.show} onHide={this.handleClose} onEntered={this.initialValues}>
				<Modal.Header closeButton>
					<Modal.Title>Model Properties</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<Form.Group controlId="exampleForm.ControlTextarea1">
					<Form.Label>Deploy Parameters</Form.Label>
					<Form.Control as="textarea" rows="3" name="dcaeDeployParameters" onChange={this.handleChange} defaultValue={this.state.form["dcaeDeployParameters"]}/>
					</Form.Group>
				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" type="null" onClick={this.handleClose}>Cacel</Button>
					<Button variant="primary" type="submit" onClick={this.saveProperties}>Save</Button>
				</Modal.Footer>
			</ModalStyled>
		);
	}
}
