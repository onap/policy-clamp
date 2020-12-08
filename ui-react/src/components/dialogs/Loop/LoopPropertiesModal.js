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
import LoopService from '../../../api/LoopService';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`
export default class LoopPropertiesModal extends React.Component {

	state = {
		show: true,
		loopCache: this.props.loopCache,
		temporaryPropertiesJson: JSON.parse(JSON.stringify(this.props.loopCache.getGlobalProperties()))
	};

	constructor(props, context) {
		super(props, context);

		this.handleClose = this.handleClose.bind(this);
		this.handleSave = this.handleSave.bind(this);
		this.handleChange = this.handleChange.bind(this);

		this.renderDcaeParameters = this.renderDcaeParameters.bind(this);
		this.renderAllParameters = this.renderAllParameters.bind(this);
		this.getDcaeParameters = this.getDcaeParameters.bind(this);
		this.readOnly = props.readOnly !== undefined ? props.readOnly : false;
	}

	componentWillReceiveProps(newProps) {
		this.setState({
			loopCache: newProps.loopCache,
			temporaryPropertiesJson: JSON.parse(JSON.stringify(newProps.loopCache.getGlobalProperties()))
		});
	}

	handleClose() {
		this.props.history.push('/');
	}

	handleSave(event) {
		LoopService.updateGlobalProperties(this.state.loopCache.getLoopName(), this.state.temporaryPropertiesJson).then(resp => {
			this.setState({ show: false });
			this.props.history.push('/');
			this.props.loadLoopFunction(this.state.loopCache.getLoopName());
		});
	}

	handleChange(event) {
		this.setState({temporaryPropertiesJson:{[event.target.name]: JSON.parse(event.target.value)}});
	}

	renderAllParameters() {
		return (<Modal.Body>
			<Form>
				{this.renderDcaeParameters()}
			</Form>
		</Modal.Body>
		);
	}

	getDcaeParameters() {
		if (typeof (this.state.temporaryPropertiesJson) !== "undefined") {
			return JSON.stringify(this.state.temporaryPropertiesJson["dcaeDeployParameters"]);
		} else {
			return "";
		}

	}

	renderDcaeParameters() {
		return (
			<Form.Group >
				<Form.Label>Deploy Parameters</Form.Label>
				<Form.Control as="textarea" rows="3" name="dcaeDeployParameters" onChange={this.handleChange} defaultValue={this.getDcaeParameters()}></Form.Control>
			</Form.Group>
		);
	}

	render() {
		return (
			<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose} backdrop="static" keyboard={false} >
				<Modal.Header closeButton>
					<Modal.Title>Model Properties</Modal.Title>
				</Modal.Header>
					{this.renderAllParameters()}
				<Modal.Footer>
					<Button variant="secondary" type="null" onClick={this.handleClose}>Cancel</Button>
					<Button variant="primary" type="submit" disabled={this.readOnly}  onClick={this.handleSave}>Save Changes</Button>
				</Modal.Footer>
			</ModalStyled>
		);
	}
}
