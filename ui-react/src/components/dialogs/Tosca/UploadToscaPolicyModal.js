/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import styled from 'styled-components';
import Alert from 'react-bootstrap/Alert';
import PolicyToscaService from '../../../api/PolicyToscaService';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`
export default class UploadToscaPolicyModal extends React.Component {
	constructor(props, context) {
		super(props, context);

		this.handleCreateFromToscaPolicyModel = this.handleCreateFromToscaPolicyModel.bind(this);
		this.handleClose = this.handleClose.bind(this);
		this.handlePolicyModelType = this.handlePolicyModelType.bind(this);
		this.fileSelectedHandler = this.fileSelectedHandler.bind(this);
		this.state = {
				show: true,
				selectedFile: '',
				policyModelType: '',
				policyModelTosca: [],
				apiResponseStatus: '',
				apiResponseMessage: '',
				upldBtnClicked: false
			};
		}

		fileSelectedHandler = (event) => {
				if (event.target.files && event.target.files[0]) {
					const scope = this;
  				    let reader = new FileReader();
					this.setState({policyModelType: '', policyModelTosca: '' });
					reader.onload = function(e) {
					    scope.setState({ policyModelTosca:  reader.result});
					};
					console.log("Filename is", event.target.files[0]);
					reader.readAsText(event.target.files[0]);
				}
				this.setState({selectedFile: event.target.files[0]});
		};

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/');
	}

	handleCreateFromToscaPolicyModel(e) {
        e.preventDefault();
		console.log("Policy Model Type is", this.state.policyModelType);
		if(this.state.policyModelType && this.state.policyModelTosca) {
 		PolicyToscaService.createPolicyModelFromToscaModel(this.state.policyModelType, this.state.policyModelTosca).then(resp => {
			if(resp.status === 200) {
			this.setState({apiResponseStatus: resp.status, apiResponseMessage: resp.message, upldBtnClicked: true});
		} else {
			this.setState({apiResponseStatus: 500, apiResponseMessage: resp, upldBtnClicked: true});
		}
	});
	} else {
		this.setState({apiResponse: 500, apiResponseMessage: 'Parameters are missing', upldBtnClicked: true});
	}
}

	handlePolicyModelType = event => {
    this.setState({
      policyModelType: event.target.value
    })
  }

	render() {
		return (
			<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose}>
				<Modal.Header closeButton>
					<Modal.Title>Upload Tosca Model</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<Form.Group as={Row} controlId="formPlaintextEmail">
						<Col sm="10">
						<input style={{display: 'none'}} type="file" name="file" accept=".yaml" onChange={this.fileSelectedHandler}
							ref={fileInput => this.fileInput = fileInput}/>
						<button onClick={() => this.fileInput.click()}>Pick Tosca File</button>
							<Alert variant="secondary">
								<p>{this.state.selectedFile.name}</p>
							</Alert>
							<Form.Label column sm="2">Policy Model Type:</Form.Label>
							<input type="text" style={{width: '50%'}}
								value={this.state.policyModelType}
								onChange={this.handlePolicyModelType}
							/>
						</Col>
					</Form.Group>
				</Modal.Body>
				<Modal.Footer>
					{!this.state.apiResponseStatus?<Button variant="secondary" type="null" onClick={this.handleClose}>Cancel</Button>:""}
				  {!this.state.apiResponseStatus?<Button disabled={!this.state.selectedFile.name || this.state.upldBtnClicked} variant="primary" type="submit" onClick={this.handleCreateFromToscaPolicyModel.bind(this)}>Create</Button>:""}
					{this.state.apiResponseStatus?<Alert variant={this.state.apiResponseStatus === 200?"success":"danger"}>
							<p>{this.state.apiResponseMessage}</p>
								<Button onClick={this.handleClose} variant={this.state.apiResponseStatus === 200?"outline-success":"danger"}>
									Exit
								</Button>
						</Alert>:""}
				</Modal.Footer>
			</ModalStyled>
		);
	}
}
