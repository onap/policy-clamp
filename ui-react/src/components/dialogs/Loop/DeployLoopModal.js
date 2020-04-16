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
import LoopActionService from '../../../api/LoopActionService';
import LoopService from '../../../api/LoopService';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import styled from 'styled-components';
import Spinner from 'react-bootstrap/Spinner'

const StyledSpinnerDiv = styled.div`
	justify-content: center !important;
	display: flex !important;
`;

const ModalStyled = styled(Modal)`
	background-color: transparent;
`
const FormStyled = styled(Form.Group)`
	padding: .25rem 1.5rem;
`
export default class DeployLoopModal extends React.Component {


		
	constructor(props, context) {
		super(props, context);

		this.handleSave = this.handleSave.bind(this);
		this.handleClose = this.handleClose.bind(this);
		this.handleChange = this.handleChange.bind(this);
		this.refreshStatus = this.refreshStatus.bind(this);
		this.renderDeployParam = this.renderDeployParam.bind(this);
		this.renderSpinner = this.renderSpinner.bind(this);

		const propertiesJson = JSON.parse(JSON.stringify(this.props.loopCache.getGlobalProperties()));
		this.state = {
			loopCache: this.props.loopCache,
			temporaryPropertiesJson: propertiesJson,
			show: true,
			key: this.getInitialKeyValue(propertiesJson)
		};
	}
	getInitialKeyValue(temporaryPropertiesJson) {
		const deployJsonList = temporaryPropertiesJson["dcaeDeployParameters"];
		let initialKey;
		Object.keys(deployJsonList)
			.filter((obj) => Object.keys(deployJsonList).indexOf(obj) === 0)
			.map(obj =>
				initialKey = obj
		);
		return initialKey;
	}
	componentWillReceiveProps(newProps) {
		this.setState({
			loopName: newProps.loopCache.getLoopName(),
			show: true
		});
	}

	handleClose(){
		this.setState({ show: false });
		this.props.history.push('/');
	}

	renderSpinner() {
		if (this.state.deploying) {
			return (
				<StyledSpinnerDiv>
					<Spinner animation="border" role="status">
						<span className="sr-only">Loading...</span>
					</Spinner>
				</StyledSpinnerDiv>
			);
		} else {
			return (<div></div>);
		}
	}

	handleSave() {
		const loopName = this.props.loopCache.getLoopName();
		// save the global propserties
		this.setState({ deploying: true });
		LoopService.updateGlobalProperties(loopName, this.state.temporaryPropertiesJson).then(resp => {
			LoopActionService.performAction(loopName, "deploy").then(pars => {
				this.props.showSucAlert("Action deploy successfully performed");
				// refresh status and update loop logs
				this.refreshStatus(loopName);
			})
			.catch(error => {
				this.props.showFailAlert("Action deploy failed");
				// refresh status and update loop logs
				this.refreshStatus(loopName);
			});
		});
	}

	refreshStatus(loopName) {
		LoopActionService.refreshStatus(loopName).then(data => {
			this.props.updateLoopFunction(data);
			this.setState({ show: false, deploying: false });
			this.props.history.push('/');
		})
		.catch(error => {
			this.props.showFailAlert("Refresh status failed");
			this.setState({ show: false, deploying: false  });
			this.props.history.push('/');
		});
	}
	handleChange(event) {
		let deploymentParam = this.state.temporaryPropertiesJson["dcaeDeployParameters"];
		deploymentParam[this.state.key][event.target.name] = event.target.value;

		this.setState({temporaryPropertiesJson:{dcaeDeployParameters: deploymentParam}});
	}
	renderDeployParamTabs() {
		if (typeof (this.state.temporaryPropertiesJson) === "undefined") {
			 return "";
		}

		const deployJsonList = this.state.temporaryPropertiesJson["dcaeDeployParameters"];
		var indents = [];
		Object.keys(deployJsonList).map((item,key) =>
			indents.push(<Tab eventKey={item} title={item}>
				{this.renderDeployParam(deployJsonList[item])}
				</Tab>)
		);
		return indents;
	}
	renderDeployParam(deployJson) {
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
					<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose} backdrop="static" keyboard={false} >
						<Modal.Header closeButton>
							<Modal.Title>Deployment parameters</Modal.Title>
						</Modal.Header>
						<Tabs id="controlled-tab-example" activeKey={this.state.key} onSelect={key => this.setState({ key })}>
						{this.renderDeployParamTabs()}
						</Tabs>
						{this.renderSpinner()}
						<Modal.Footer>
							<Button variant="secondary" type="null" onClick={this.handleClose}>Cancel</Button>
							<Button variant="primary" type="submit" onClick={this.handleSave}>Deploy</Button>
						</Modal.Footer>
					</ModalStyled>
		);
	}
}
