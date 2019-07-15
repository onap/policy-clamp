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

import React from 'react'
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import { LOOP_CACHE } from '../../../api/LoopCache'
import styled from 'styled-components';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

export default class ConfigurationPolicyModal extends React.Component {

	constructor(props, context) {
		super(props, context);

		this.handleClose = this.handleClose.bind(this);

		this.state = {
			show: true,
		};

	}

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/')
	}



	render() {
		return (
			<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose}>
				<Modal.Header closeButton>
					<Modal.Title>Configuration policies</Modal.Title>
				</Modal.Header>
				<Modal.Body>



				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" onClick={this.handleClose}>
						Close
	            </Button>
					<Button variant="primary" onClick={this.handleClose}>
						Save Changes
	            </Button>
				</Modal.Footer>
			</ModalStyled>

		);
	}
}