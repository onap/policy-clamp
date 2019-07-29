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
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import styled from 'styled-components';
import UserService from '../../api/UserService';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

export default class UserInfo extends React.Component {

	constructor(props, context) {
		super(props, context);

		this.handleClose = this.handleClose.bind(this);
		this.initialValues = this.initialValues.bind(this);
		this.renderReadTemplatePermission = this.renderReadTemplatePermission.bind(this);
		this.renderReadModelPermission = this.renderReadModelPermission.bind(this);
		this.renderReadToscaPermission = this.renderReadToscaPermission.bind(this);
		this.renderUpdateTemplatePermission = this.renderUpdateTemplatePermission.bind(this);
		this.renderUpdateModelPermission = this.renderUpdateModelPermission.bind(this);
		this.renderUpdateToscaPermission = this.renderUpdateToscaPermission.bind(this);
		this.renderUserName = this.renderUserName.bind(this);
		this.state = {
			show: true,
			userInfo: {permissionReadTemplate: true,
						permissionReadCl: true,
						permissionReadTosca: true,
						permissionUpdateCl: true,
						permissionUpdateTemplate: true,
						permissionUpdateTosca: true,
						userName: 'admin',
						cldsVersion: '1.0.0'
						}
		};

	}
	initialValues() {
		UserService.getUserInfo().then(userInfo => {
			this.setState({ userInfo: userInfo })
		});
	}
	handleClose() {
			this.props.history.push('/');
	}
	renderReadTemplatePermission() {
		if (this.state.userInfo["permissionReadTemplate"]) {
			return <Form.Control plaintext readOnly defaultValue="Read Template" />
		} else  {
			return;
		}
	}
	renderReadModelPermission() {
		if (this.state.userInfo["permissionReadCl"]) {
			return <Form.Control plaintext readOnly defaultValue="Read Model" />
		} else  {
			return;
		}
	}
	renderReadToscaPermission() {
		if (this.state.userInfo["permissionReadTosca"]) {
			return <Form.Control plaintext readOnly defaultValue="Read Tosca" />
		} else  {
			return;
		}
	}
	renderUpdateTemplatePermission() {
		if (this.state.userInfo["permissionUpdateTemplate"]) {
			return <Form.Control plaintext readOnly defaultValue="Edit Template" />
		} else  {
			return;
		}
	}
	renderUpdateModelPermission() {
		if (this.state.userInfo["permissionUpdateCl"]) {
			return <Form.Control plaintext readOnly defaultValue="Edit Model" />
		} else  {
			return;
		}
	}
	renderUpdateToscaPermission() {
		if (this.state.userInfo["permissionUpdateTosca"]) {
			return <Form.Control plaintext readOnly defaultValue="Edit Tosca" />
		} else  {
			return;
		}
	}
	renderUserName() {
		if (this.state.userInfo["userName"]) {
			return <Form.Control plaintext readOnly defaultValue={this.state.userInfo["userName"]} />
		} else  {
			return;
		}
	}
	renderVersion() {
		if (this.state.userInfo["cldsVersion"]) {
			return <Form.Control plaintext readOnly defaultValue={this.state.userInfo["cldsVersion"]} />
		} else  {
			return;
		}
	}
	render() {
		return (
			<ModalStyled size="lg"  show={this.state.show} onHide={this.handleClose} onEntered={this.initialValues}>
				<Modal.Header closeButton>
					<Modal.Title>User Info</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<Form.Group as={Row} controlId="userName">
						<Form.Label column sm="3">Current User:</Form.Label>
						<Col>{this.renderUserName()}</Col>
					</Form.Group>
					<Form.Group as={Row} controlId="cldsVersion">
						<Form.Label column sm="3">CLDS Version:</Form.Label>
						<Col>{this.renderVersion()}</Col>
					</Form.Group>
					<Form.Group as={Row} controlId="userPermissions">
						<Form.Label column sm="3">User Permissions:</Form.Label>
						<Col>
							{this.renderReadTemplatePermission()}
							{this.renderReadModelPermission()}
							{this.renderReadToscaPermission()}
							{this.renderUpdateTemplatePermission()}
							{this.renderUpdateModelPermission()}
							{this.renderUpdateToscaPermission()}
						</Col>
					</Form.Group>
				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" type="null" onClick={this.handleClose}>Cacel</Button>
				</Modal.Footer>
			</ModalStyled>
		);
	}
}
