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
    margin-top: 20px;
    text-align: justify;
    font-size: ${props => props.theme.policyEditorFontSize};
    background-color: ${props => props.theme.loopViewerBackgroundColor};
`

export default class PolicyDeploymentEditor extends React.Component {

    state = {
            policyData: this.props.policyData,
            showSuccessAlert: false,
            showFailAlert: false,
            checkboxesState: [],
    };

    constructor(props, context) {
        super(props, context);
        this.handleClose = this.handleClose.bind(this);
        this.handleUpdatePdpDeployment = this.handleUpdatePdpDeployment.bind(this);
        this.disableAlert = this.disableAlert.bind(this);
        this.renderPdpDeploymentCheckboxes = this.renderPdpDeploymentCheckboxes.bind(this);
        this.createPdpStructures = this.createPdpStructures.bind(this);
        this.createPdpStructures();
    }

    handleClose() {
        this.setState({ show: false });

    }

    disableAlert() {
        this.setState ({ showSuccessAlert: false, showFailAlert: false });
    }

    handleUpdatePdpDeployment() {
            PolicyService.updatePdpDeployment("").then(respPdpDeploymentUpdate => {
                if (typeof(respPdpDeploymentUpdate) === "undefined") {
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
    }

    createPdpStructures() {
            let supportedPdpMap = new Map();
            if (typeof this.state.policyData.supportedPdpGroups !== "undefined") {
                        for (const pdpGroup of this.state.policyData["supportedPdpGroups"]) {
                            for (const pdpSubGroup of Object.values(pdpGroup)[0]) {
                                supportedPdpMap.set(Object.keys(pdpGroup)[0] + "/" + pdpSubGroup,
                                    Object.keys(pdpGroup)[0] + "/" + pdpSubGroup);
                            }
                        }
            }

            let infoPdpMap = new Map();
            if (typeof this.state.policyData.pdpGroupInfo !== "undefined") {
                    this.state.policyData["pdpGroupInfo"].forEach(pdpGroupElem => {
                        pdpGroupElem[Object.keys(pdpGroupElem)[0]]["pdpSubgroups"].forEach(pdpSubGroupElem => {
                            infoPdpMap.set(Object.keys(pdpGroupElem)[0] + "/" + pdpSubGroupElem["pdpType"],
                                Object.keys(pdpGroupElem)[0] + "/" + pdpSubGroupElem["pdpType"]);
                        });
                    });
            }
            this.setState({ supportedPdpGroupsMap:supportedPdpMap, currentPdpGroupsMap:infoPdpMap});
    }

    renderPdpDeploymentCheckboxes() {
        let checkBoxTable=[];

        this.state.supportedPdpGroupsMap.forEach((value, key, map) => {
            checkBoxTable.push(<p>key</p>)
        });
        return (
                    <FormGroup>
                        {checkBoxTable}
                    </FormGroup>
        );
    }

    render() {
     return (
                    <PanelDiv>
                      <Alert variant="success" show={this.state.showSuccessAlert} onClose={this.disableAlert} dismissible>
                          <DivWhiteSpaceStyled>
                              {this.state.showMessage}
                          </DivWhiteSpaceStyled>
                      </Alert>
                      <Alert variant="danger" show={this.state.showFailAlert} onClose={this.disableAlert} dismissible>
                          <DivWhiteSpaceStyled>
                              {this.state.showMessage}
                          </DivWhiteSpaceStyled>
                      </Alert>
                      <Button variant="secondary" title="Update the policy to the specified PDP Groups/Subgroups"
                        onClick={this.handleUpdatePdpDeployment}>Update PDP</Button>
                        {this.renderPdpDeploymentCheckboxes()}
                    </PanelDiv>
       );
    }
 }