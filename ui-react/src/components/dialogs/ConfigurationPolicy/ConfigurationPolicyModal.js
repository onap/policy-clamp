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
import styled from 'styled-components';

import JSONEditor from '@json-editor/json-editor';

const ModalStyled = styled(Modal)`
	background-color: transparent;
`

export default class ConfigurationPolicyModal extends React.Component {

	state = {
		show: true,
		loopCache: this.props.loopCache,
		jsonEditor: null,
	};

	constructor(props, context) {
		super(props, context);
		this.handleClose = this.handleClose.bind(this);
		this.renderJsonEditor = this.renderJsonEditor.bind(this);
	}

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/')
	}

	componentDidMount() {
		this.renderJsonEditor();
	}

	renderJsonEditor() {
		var toscaModel = this.state.loopCache.getMicroServiceJsonRepresentationForType("TCA_Jbv1z_v1_0_ResourceInstanceName1_tca");
		if (toscaModel == null) {
			return;
		}
		var editorData = this.state.loopCache.getMicroServiceProperties("TCA_Jbv1z_v1_0_ResourceInstanceName1_tca");

		JSONEditor.defaults.options.theme = 'bootstrap4';
		//JSONEditor.defaults.options.iconlib = 'bootstrap2';
		JSONEditor.defaults.options.object_layout = 'grid';
		JSONEditor.defaults.options.disable_properties = true;
		JSONEditor.defaults.options.disable_edit_json = false;
		JSONEditor.defaults.options.disable_array_reorder = true;
		JSONEditor.defaults.options.disable_array_delete_last_row = true;
		JSONEditor.defaults.options.disable_array_delete_all_rows = false;
		JSONEditor.defaults.options.show_errors = 'always';

		this.state.jsonEditor = new JSONEditor(document.getElementById("editor"),
			{ schema: toscaModel.schema, startval: editorData });

	}

	render() {
		return (
			<ModalStyled size="lg" show={this.state.show} onHide={this.handleClose}>
				<Modal.Header closeButton>
					<Modal.Title>Configuration policies</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					<div id="editor" />

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