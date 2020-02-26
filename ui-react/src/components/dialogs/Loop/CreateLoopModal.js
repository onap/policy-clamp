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
import Select from 'react-select';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import styled from 'styled-components';
import LoopService from '../../../api/LoopService';
import TemplateService from '../../../api/TemplateService';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

export default class CreateLoopModal extends React.Component {
	constructor(props, context) {
		super(props, context);

		this.getTemplateNames = this.getTemplateNames.bind(this);
		this.handleCreate = this.handleCreate.bind(this);
		this.handleModelName = this.handleModelName.bind(this);
		this.handleClose = this.handleClose.bind(this);
		this.handleDropdownListChange = this.handleDropdownListChange.bind(this);
		this.state = {
			show: true,
			chosenTemplateName: '',
			modelName: '',
			templateNames: []
		};
	}

	componentWillMount() {
		this.getTemplateNames();
	}

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/');
	}

	handleDropdownListChange(e) {
		this.setState({ chosenTemplateName: e.value });
	}

	getTemplateNames() {
		TemplateService.getTemplateNames().then(templateNames => {
			const templateOptions = templateNames.map((templateName) => { return { label: templateName, value: templateName } });
			this.setState({ templateNames: templateOptions })
		});
	}

	handleCreate() {
		if (!this.state.modelName) {
			alert("A model name is required");
			return;
		}
		console.info("Create Model " + this.state.modelName + ", Template " + this.state.chosenTemplateName + " is chosen");
		this.setState({ show: false });
		LoopService.createLoop("LOOP_" + this.state.modelName, this.state.chosenTemplateName).then(text => {
			console.debug("CreateLoop response received: ", text);
			try {
				this.props.history.push('/');
				this.props.loadLoopFunction("LOOP_" + this.state.modelName);
			} catch(err) {
				alert(text);
				this.props.history.push('/');
			}
		})
		.catch(error => {
			console.debug("Create Loop failed");
		});

	}

	handleModelName = event => {
    	this.setState({
      		modelName: event.target.value
    	})
	}

	render() {
		return (
			<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose}>
				<Modal.Header closeButton>
					<Modal.Title>Create Model</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<Form.Group as={Row} controlId="formPlaintextEmail">
						<Form.Label column sm="2">Template Name</Form.Label>
						<Col sm="10">
							<Select onChange={this.handleDropdownListChange} options={this.state.templateNames} />
						</Col>
					</Form.Group>
					<Form.Group controlId="formPlaintextEmail">
						<Form.Label column sm="2">Model Name:</Form.Label>
						<input type="text" style={{width: '50%'}}
							value={this.state.modelName}
							onChange={this.handleModelName}
						/>
					</Form.Group>
				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" type="null" onClick={this.handleClose}>Cancel</Button>
					<Button variant="primary" type="submit" onClick={this.handleCreate}>Create</Button>
				</Modal.Footer>
			</ModalStyled>

		);
	}
}