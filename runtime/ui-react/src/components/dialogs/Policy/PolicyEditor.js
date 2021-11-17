/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights
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
import PolicyToscaService from '../../../api/PolicyToscaService';
import { JSONEditor } from '@json-editor/json-editor/dist/nonmin/jsoneditor.js';
import "@fortawesome/fontawesome-free/css/all.css"
import styled from 'styled-components';
import Button from 'react-bootstrap/Button';
import TextField from '@material-ui/core/TextField';
import Alert from 'react-bootstrap/Alert';
import PolicyService from '../../../api/PolicyService';
import OnapUtils from '../../../utils/OnapUtils';

const DivWhiteSpaceStyled = styled.div`
  white-space: pre;
`

const JsonEditorDiv = styled.div`
  margin-top: 20px;
  background-color: ${ props => props.theme.loopViewerBackgroundColor };
  text-align: justify;
  font-size: ${ props => props.theme.policyEditorFontSize };
  border: 1px solid #C0C0C0;
`
const PanelDiv = styled.div`
  text-align: justify;
  font-size: ${ props => props.theme.policyEditorFontSize };
  background-color: ${ props => props.theme.loopViewerBackgroundColor };
`

export default class PolicyEditor extends React.Component {

  state = {
    policyModelType: this.props.policyModelType,
    policyModelTypeVersion: this.props.policyModelTypeVersion,
    policyName: (typeof this.props.policyName !== "undefined") ? this.props.policyName : "org.onap.policy.new",
    policyVersion: (typeof this.props.policyVersion !== "undefined") ? this.props.policyVersion : "0.0.1",
    policyProperties: this.props.policyProperties,
    showSuccessAlert: false,
    showFailAlert: false,
    jsonEditor: null,
    jsonEditorDivId: this.props.policyModelType + "_" + this.props.policyModelTypeVersion + "_" + this.props.policyName + "_" + this.props.policyVersion,
  }

  constructor(props, context) {
    super(props, context);
    this.createJsonEditor = this.createJsonEditor.bind(this);
    this.getToscaModelForPolicy = this.getToscaModelForPolicy.bind(this);
    this.disableAlert = this.disableAlert.bind(this);
    this.handleCreateNewVersion = this.handleCreateNewVersion.bind(this);
    this.handleChangePolicyName = this.handleChangePolicyName.bind(this);
    this.handleChangePolicyVersion = this.handleChangePolicyVersion.bind(this);
  }

  disableAlert() {
    this.setState({ showSuccessAlert: false, showFailAlert: false });
  }

  customValidation(editorData) {
    // method for sub-classes to override with customized validation
    return [];
  }

  handleCreateNewVersion() {
    var editorData = this.state.jsonEditor.getValue();
    var errors = this.state.jsonEditor.validate();
    errors = errors.concat(this.customValidation(editorData));

    if (errors.length !== 0) {
      console.error("Errors detected during policy data validation ", errors);
      this.setState({
        showFailAlert: true,
        showMessage: 'Errors detected during policy data validation:\n' + OnapUtils.jsonEditorErrorFormatter(errors)
      });
      return;
    } else {
      console.info("NO validation errors found in policy data");
      PolicyService.createNewPolicy(this.state.policyModelType, this.state.policyModelTypeVersion,
        this.state.policyName, this.state.policyVersion, editorData).then(respPolicyCreation => {
        if (typeof (respPolicyCreation) === "undefined") {
          //it indicates a failure
          this.setState({
            showFailAlert: true,
            showMessage: 'Policy Creation Failure'
          });
        } else {
          this.setState({
            showSuccessAlert: true,
            showMessage: 'Policy ' + this.state.policyName + '/' + this.state.policyVersion + ' created successfully'
          });
          this.props.policiesTableUpdateFunction();
        }
      })
    }
  }

  getToscaModelForPolicy() {
    PolicyToscaService.getToscaPolicyModel(this.state.policyModelType, this.state.policyModelTypeVersion).then(respJsonPolicyTosca => {
      if (respJsonPolicyTosca !== {}) {
        this.setState({
          jsonSchemaPolicyTosca: respJsonPolicyTosca,
          jsonEditor: this.createJsonEditor(respJsonPolicyTosca, this.state.policyProperties),
        })
      }
    });
  }

  componentDidMount() {
    this.getToscaModelForPolicy();
  }

  createJsonEditor(toscaModel, editorData) {
    return new JSONEditor(document.getElementById(this.state.jsonEditorDivId),
      {
        schema: toscaModel,
        startval: editorData,
        theme: 'bootstrap4',
        iconlib: 'fontawesome5',
        object_layout: 'grid',
        disable_properties: false,
        disable_edit_json: false,
        disable_array_reorder: true,
        disable_array_delete_last_row: true,
        disable_array_delete_all_rows: false,
        array_controls_top: true,
        keep_oneof_values: false,
        collapsed: true,
        show_errors: 'always',
        display_required_only: false,
        show_opt_in: false,
        prompt_before_delete: true,
        required_by_default: false
      })
  }

  handleChangePolicyName(event) {
    this.setState({
      policyName: event.target.value,
    });
  }

  handleChangePolicyVersion(event) {
    this.setState({
      policyVersion: event.target.value,
    });
  }

  render() {
    return (
      <PanelDiv>
        <Alert variant="success" show={ this.state.showSuccessAlert } onClose={ this.disableAlert } dismissible>
          <DivWhiteSpaceStyled>
            { this.state.showMessage }
          </DivWhiteSpaceStyled>
        </Alert>
        <Alert variant="danger" show={ this.state.showFailAlert } onClose={ this.disableAlert } dismissible>
          <DivWhiteSpaceStyled>
            { this.state.showMessage }
          </DivWhiteSpaceStyled>
        </Alert>
        <TextField required id="policyName" label="Required" defaultValue={ this.state.policyName }
                   onChange={ this.handleChangePolicyName } variant="outlined" size="small"/>
        <TextField required id="policyVersion" label="Required" defaultValue={ this.state.policyVersion }
                   onChange={ this.handleChangePolicyVersion } size="small" variant="outlined"/>
        <Button variant="secondary" title="Create a new policy version from the defined parameters"
                onClick={ this.handleCreateNewVersion }>Create New Version</Button>
        <JsonEditorDiv id={ this.state.jsonEditorDivId } title="Policy Properties"/>
      </PanelDiv>
    );
  }
}
