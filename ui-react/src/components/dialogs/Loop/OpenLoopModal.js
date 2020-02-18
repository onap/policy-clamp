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
import Select from 'react-select';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import FormCheck from 'react-bootstrap/FormCheck'
import styled from 'styled-components';
import LoopService from '../../../api/LoopService';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`
const CheckBoxStyled = styled(FormCheck.Input)`
	margin-left:3rem;
`

export default class OpenLoopModal extends React.Component {
	constructor(props, context) {
		super(props, context);

		this.getLoopNames = this.getLoopNames.bind(this);
		this.handleOpen = this.handleOpen.bind(this);
		this.handleClose = this.handleClose.bind(this);
		this.handleDropdownListChange = this.handleDropdownListChange.bind(this);
		this.state = {
			show: true,
			chosenLoopName: '',
			loopNames: []
		};
	}

	componentWillMount() {
		this.getLoopNames();
	}

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/');
	}

	handleDropdownListChange(e) {
		this.setState({ chosenLoopName: e.value });
	}

	getLoopNames() {
		LoopService.getLoopNames().then(loopNames => {
		    if (Object.entries(loopNames).length !== 0) {
		        const loopOptions = loopNames.filter(loopName => loopName!=='undefined').map((loopName) => { return { label: loopName, value: loopName } });
            	this.setState({ loopNames: loopOptions })
		    }
		});
	}

	handleOpen() {
		console.info("Loop " + this.state.chosenLoopName + " is chosen");
        this.handleClose();
		this.props.loadLoopFunction(this.state.chosenLoopName);
	}

	render() {
		return (
			<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose}>
				<Modal.Header closeButton>
					<Modal.Title>Open Model</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<Form.Group as={Row} controlId="formPlaintextEmail">
						<Form.Label column sm="2">Model Name</Form.Label>
						<Col sm="10">
							<Select onChange={this.handleDropdownListChange} options={this.state.loopNames} />
						</Col>
					</Form.Group>
					<Form.Group controlId="formBasicChecbox">
						<Form.Check>
							<FormCheck.Label>Read Only</FormCheck.Label>
							<CheckBoxStyled type="checkbox" />
						</Form.Check>
					</Form.Group>
				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" type="null" onClick={this.handleClose}>Cancel</Button>
					<Button variant="primary" type="submit" onClick={this.handleOpen}>Open</Button>
				</Modal.Footer>
			</ModalStyled>

		);
	}
}
