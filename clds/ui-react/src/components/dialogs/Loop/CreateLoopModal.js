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
import LoopCache from '../../../api/LoopCache';
import SvgGenerator from '../../loop_viewer/svg/SvgGenerator';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

const ErrMsgStyled = styled.div`
	color: red;
`

export default class CreateLoopModal extends React.Component {
	constructor(props, context) {
		super(props, context);

		this.getAllLoopTemplates = this.getAllLoopTemplates.bind(this);
		this.handleCreate = this.handleCreate.bind(this);
		this.handleModelName = this.handleModelName.bind(this);
		this.handleClose = this.handleClose.bind(this);
		this.handleDropDownListChange = this.handleDropDownListChange.bind(this);
		this.renderSvg = this.renderSvg.bind(this);
		this.state = {
			show: true,
			chosenTemplateName: '',
			modelInputErrMsg: '',
			modelName: '',
			templateNames: [],
			fakeLoopCacheWithTemplate: new LoopCache({})
		};
	}

	async componentDidMount() {
		await this.getAllLoopTemplates();
		await this.getModelNames();
	}

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/');
	}

	handleDropDownListChange(e) {
	    if (typeof e.value !== "undefined") {
	        this.setState({
	        fakeLoopCacheWithTemplate:
                new LoopCache({
                    "loopTemplate":e.templateObject,
                    "name": "fakeLoop"
                }),
                chosenTemplateName: e.value
	        })
		} else {
          	this.setState({ fakeLoopCacheWithTemplate: new LoopCache({}) })
        }
	}

	getAllLoopTemplates() {
		TemplateService.getAllLoopTemplates().then(templatesData => {
		    const templateOptions = templatesData.map((templateData) => { return { label: templateData.name, value: templateData.name, templateObject: templateData } });
            this.setState({
                templateNames: templateOptions })
		});
	}

	getModelNames() {
		TemplateService.getLoopNames().then(loopNames => {
			if (!loopNames) {
				loopNames = [];
			}
			// Remove LOOP_ prefix
			let trimmedLoopNames = loopNames.map(str => str.replace('LOOP_', ''));
			this.setState({ modelNames: trimmedLoopNames });
		});
	}

	handleCreate() {
		if (!this.state.modelName) {
			alert("A model name is required");
			return;
		}
		console.debug("Create Model " + this.state.modelName + ", Template " + this.state.chosenTemplateName + " is chosen");
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

	handleModelName(event) {
		if (this.state.modelNames.includes(event.target.value)) {
			this.setState({
				modelInputErrMsg: 'A model named "' + event.target.value + '" already exists. Please pick another name.',
				modelName: event.target.value
			});
			return;
		} else {
			this.setState({
				modelInputErrMsg: '',
				modelName: event.target.value
			});
		}
	}

	renderSvg() {
		return (
			<SvgGenerator loopCache={this.state.fakeLoopCacheWithTemplate} clickable={false} generatedFrom={SvgGenerator.GENERATED_FROM_TEMPLATE}/>
		);
	}

	render() {
		return (
			<ModalStyled size="xl" show={this.state.show} onHide={this.handleClose} backdrop="static" keyboard={false} >
				<Modal.Header closeButton>
					<Modal.Title>Create Model</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<Form.Group as={Row} controlId="formPlaintextEmail">
						<Form.Label column sm="2">Template Name:</Form.Label>
						<Col sm="10">
							<Select onChange={this.handleDropDownListChange} options={this.state.templateNames} />
						</Col>
					</Form.Group>
                    <Form.Group as={Row} style={{alignItems: 'center'}} controlId="formSvgPreview">
                    <Form.Label column sm="2">Model Preview:</Form.Label>
                        <Col sm="10">
                            {this.renderSvg()}
                        </Col>
                    </Form.Group>
					<Form.Group as={Row} controlId="formPlaintextEmail">
						<Form.Label column sm="2">Model Name:</Form.Label>
						<input sm="5" type="text" style={{width: '50%', marginLeft: '1em' }}
							value={this.state.modelName}
							onChange={this.handleModelName}
						/>
						<span sm="5"/>
					</Form.Group>
					<Form.Group as={Row} controlId="formPlaintextEmail">
						<Form.Label column sm="2"> </Form.Label>
						<ErrMsgStyled>{this.state.modelInputErrMsg}</ErrMsgStyled>
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
