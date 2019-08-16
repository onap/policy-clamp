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
import React from 'react';
import LoopActionService from '../../api/LoopActionService';
import LoopService from '../../api/LoopService';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import styled from 'styled-components';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`
const FormStyled = styled(Form.Group)`
	padding: .25rem 1.5rem;
`
export default class DeployLoop extends React.Component {
	state = {
		loopCache: this.props.loopCache,
		temporaryPropertiesJson: JSON.parse(JSON.stringify(this.props.loopCache.getGlobalProperties())),
		show: true
	};
	constructor(props, context) {
		super(props, context);

		this.handleSave = this.handleSave.bind(this);
		this.handleClose = this.handleClose.bind(this);
		this.handleChange = this.handleChange.bind(this);
		this.refreshStatus = this.refreshStatus.bind(this);
		this.renderDeployParam = this.renderDeployParam.bind(this);
	}
	componentWillReceiveProps(newProps) {
		this.setState({
			loopName: newProps.loopCache.getLoopName(),
			show: true
		});
	}
	handleClose(){
		this.props.history.push('/');
	}
	handleSave(e) {
		const loopName = this.props.loopCache.getLoopName();
		// save the global propserties
		LoopService.updateGlobalProperties(loopName, this.state.temporaryPropertiesJson).then(resp => {
			this.setState({ show: false });

			console.log("Perform action: deploy");
			LoopActionService.performAction(loopName, "deploy").then(pars => {
				alert("Action deploy successfully performed");
				// refresh status and update loop logs
				this.refreshStatus(loopName);
			})
			.catch(error => {
				alert("Action deploy failed");
				// refresh status and update loop logs
				this.refreshStatus(loopName);
			});
		});
	}

	refreshStatus(loopName) {
		LoopActionService.refreshStatus(loopName).then(data => {
			this.props.updateLoopFunction(data);
			this.props.history.push('/');
		})
		.catch(error => {
			alert("Refresh status failed");
			this.props.history.push('/');
		});
	}
	handleChange(event) {
		let deploymentParam = this.state.temporaryPropertiesJson["dcaeDeployParameters"];
		deploymentParam[event.target.name] = event.target.value;

		this.setState({temporaryPropertiesJson:{dcaeDeployParameters: deploymentParam}});
	}
	renderDeployParam() {
		if (typeof (this.state.temporaryPropertiesJson) === "undefined") {
			 return "";
		}

		const deployJson = this.state.temporaryPropertiesJson["dcaeDeployParameters"];
		var indents = [];
		Object.keys(deployJson).map((item,key) =>
		indents.push(<FormStyled>
				<Form.Label>{item}</Form.Label>
				<Form.Control type="text" name={item} onChange={this.handleChange} defaultValue={deployJson[item]}></Form.Control>
			</FormStyled>));

			return indents;
	}


	render() {
		return (
					<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose} >
						<Modal.Header closeButton>
							<Modal.Title>Deployment parameters</Modal.Title>
						</Modal.Header>
						{this.renderDeployParam()}
						<Modal.Footer>
							<Button variant="secondary" type="null" onClick={this.handleClose}>Cancel</Button>
							<Button variant="primary" type="submit" onClick={this.handleSave}>Deploy</Button>
						</Modal.Footer>
					</ModalStyled>
		);
	}
}
