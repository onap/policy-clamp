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

import React, { forwardRef } from 'react';
import Modal from 'react-bootstrap/Modal';
import styled from 'styled-components';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import PolicyService from '../../../api/PolicyService';
import FormGroup from '@material-ui/core/FormGroup';
import Checkbox from '@material-ui/core/Checkbox';
import FormControlLabel from '@material-ui/core/FormControlLabel';

const DivWhiteSpaceStyled = styled.div`
  white-space: pre;
`

const PanelDiv = styled.div`
  text-align: justify;
  font-size: ${ props => props.theme.policyEditorFontSize };
  background-color: ${ props => props.theme.loopViewerBackgroundColor };
`

export default class PolicyDeploymentEditor extends React.Component {

  state = {
    policyData: this.props.policyData,
    showSuccessAlert: false,
    showFailAlert: false,
    checkboxesState: this.createPdpStructure(this.props.policyData),
    checkboxesInitialState: this.createPdpStructure(this.props.policyData),
  };

  constructor(props, context) {
    super(props, context);
    this.handleClose = this.handleClose.bind(this);
    this.handleUpdatePdpDeployment = this.handleUpdatePdpDeployment.bind(this);
    this.disableAlert = this.disableAlert.bind(this);
    this.renderPdpDeploymentCheckboxes = this.renderPdpDeploymentCheckboxes.bind(this);
    this.createPdpStructure = this.createPdpStructure.bind(this);
    this.handleCheckboxChange = this.handleCheckboxChange.bind(this);
    this.createPdpGroupOperations = this.createPdpGroupOperations.bind(this);
  }

  handleClose() {
    this.setState({ show: false });

  }

  disableAlert() {
    this.setState({ showSuccessAlert: false, showFailAlert: false });
  }

  createPdpGroupOperations(initialStates, newStates) {
    let commandsArray = [];
    initialStates.forEach(initElem => {
      let newStateFound = newStates.find(newElement => newElement.name === initElem.name);
      if (initElem.value !== newStateFound.value) {
        let newPdpGroupsArray = newStateFound.name.split("/");
        let operation = "POST/";
        if (initElem.value) {
          operation = "DELETE/";
        }
        commandsArray.push(operation + newPdpGroupsArray[0] + "/" + newPdpGroupsArray[1] + "/"
          + this.state.policyData.name + "/" + this.state.policyData.version);
      }
    });
    return commandsArray.length > 0 ? { "PdpActions": commandsArray } : undefined;
  }

  handleUpdatePdpDeployment() {
    let operationsList = this.createPdpGroupOperations(this.state.checkboxesInitialState,
      this.state.checkboxesState);
    if (typeof (operationsList) !== "undefined") {
      PolicyService.updatePdpDeployment(operationsList).then(respPdpDeploymentUpdate => {
        if (typeof (respPdpDeploymentUpdate) === "undefined") {
          //it indicates a failure
          this.setState({
            showFailAlert: true,
            showMessage: 'Pdp Deployment update Failure'
          });
        } else {
          this.setState({
            showSuccessAlert: true,
            showMessage: 'Pdp Deployment Update successful'
          });
          this.props.policiesTableUpdateFunction();
        }
      })
    } else {
      this.setState({
        showSuccessAlert: true,
        showMessage: 'Pdp Deployment: Nothing to change'
      });
    }
  }

  createPdpStructure(policyData) {
    // Create map with data for all group/subgroup where the policy is deployed
    let infoPdpMap = new Map();
    if (typeof policyData.pdpGroupInfo !== "undefined") {
      policyData["pdpGroupInfo"].forEach(pdpGroupElem => {
        let pdpGroupName = Object.keys(pdpGroupElem)[0];
        pdpGroupElem[pdpGroupName]["pdpSubgroups"].forEach(pdpSubGroupElem => {
          infoPdpMap.set(pdpGroupName + "/" + pdpSubGroupElem["pdpType"], true);
        });
      });
    }
    // Create the possible values for pdpgroup/subgroup and tick the ones where policy is deployed
    let pdpStates = [];
    if (typeof policyData.supportedPdpGroups !== "undefined") {
      for (const pdpGroup of policyData["supportedPdpGroups"]) {
        let pdpGroupName = Object.keys(pdpGroup)[0];
        for (const pdpSubGroup of Object.values(pdpGroup)[0]) {
          let fullName = pdpGroupName + "/" + pdpSubGroup;
          pdpStates.push({
            name: fullName,
            value: infoPdpMap.get(fullName) !== undefined
          });
        }
      }
    }
    return pdpStates;
  }

  handleCheckboxChange(event) {
    const checkboxesArray = this.state.checkboxesState;
    checkboxesArray.find(element => element.name === event.target.name).value = event.target.checked;
    this.setState({ checkboxesState: checkboxesArray });
  }

  renderPdpDeploymentCheckboxes() {
    return this.state.checkboxesState.map(item => {
      return <FormControlLabel control={ <Checkbox checked={ item.value } onChange={ this.handleCheckboxChange }
                                                   name={ item.name }/> } label={ item.name }/>;
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
        <Button variant="secondary" title="Update the policy to the specified PDP Groups/Subgroups"
                onClick={ this.handleUpdatePdpDeployment }>Update PDP</Button>
        <FormGroup>{ this.renderPdpDeploymentCheckboxes() }</FormGroup>
      </PanelDiv>
    );
  }
}
