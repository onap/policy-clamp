/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

import React, { forwardRef } from 'react'
import MaterialTable from "material-table";
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import styled from 'styled-components';
import PolicyToscaService from '../../../api/PolicyToscaService';
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Search from '@material-ui/icons/Search';
import LoopService from '../../../api/LoopService';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import Alert from 'react-bootstrap/Alert';

const ModalStyled = styled(Modal)`
  background-color: transparent;
`
const TextModal = styled.textarea`
  margin-top: 20px;
  white-space: pre;
  background-color: ${ props => props.theme.toscaTextareaBackgroundColor };
  text-align: justify;
  font-size: ${ props => props.theme.toscaTextareaFontSize };
  width: 100%;
  height: 300px;
`
const cellStyle = { border: '1px solid black' };
const headerStyle = { backgroundColor: '#ddd', border: '2px solid black' };
const rowHeaderStyle = { backgroundColor: '#ddd', fontSize: '15pt', text: 'bold', border: '1px solid black' };

export default class ModifyLoopModal extends React.Component {

  state = {
    show: true,
    loopCache: this.props.loopCache,
    content: 'Please select Tosca model to view the details',
    selectedRowData: {},
    toscaPolicyModelsData: [],
    selectedPolicyModelsData: [],
    key: 'add',
    showFailAlert: false,
    toscaColumns: [
      {
        title: "#", field: "index", render: rowData => rowData.tableData.id + 1,
        cellStyle: cellStyle,
        headerStyle: headerStyle
      },
      {
        title: "Policy Model Type", field: "policyModelType",
        cellStyle: cellStyle,
        headerStyle: headerStyle
      },
      {
        title: "Policy Acronym", field: "policyAcronym",
        cellStyle: cellStyle,
        headerStyle: headerStyle
      },
      {
        title: "Policy Name", field: "policyName",
        cellStyle: cellStyle,
        headerStyle: headerStyle
      },
      {
        title: "Version", field: "version",
        cellStyle: cellStyle,
        headerStyle: headerStyle
      },
      {
        title: "Uploaded By", field: "updatedBy",
        cellStyle: cellStyle,
        headerStyle: headerStyle
      },
      {
        title: "Uploaded Date", field: "updatedDate", editable: 'never',
        cellStyle: cellStyle,
        headerStyle: headerStyle
      },
      {
        title: "Created Date", field: "createdDate", editable: 'never',
        cellStyle: cellStyle,
        headerStyle: headerStyle
      }
    ],
    tableIcons: {
      FirstPage: forwardRef((props, ref) => <FirstPage { ...props } ref={ ref }/>),
      LastPage: forwardRef((props, ref) => <LastPage { ...props } ref={ ref }/>),
      NextPage: forwardRef((props, ref) => <ChevronRight { ...props } ref={ ref }/>),
      PreviousPage: forwardRef((props, ref) => <ChevronLeft { ...props } ref={ ref }/>),
      ResetSearch: forwardRef((props, ref) => <Clear { ...props } ref={ ref }/>),
      Search: forwardRef((props, ref) => <Search { ...props } ref={ ref }/>),
      SortArrow: forwardRef((props, ref) => <ArrowUpward { ...props } ref={ ref }/>)
    }
  };

  constructor(props, context) {
    super(props, context);
    this.handleClose = this.handleClose.bind(this);
    this.initializeToscaPolicyModelsInfo = this.initializeToscaPolicyModelsInfo.bind(this);
    this.handleYamlContent = this.handleYamlContent.bind(this);
    this.getToscaPolicyModelYaml = this.getToscaPolicyModelYaml.bind(this);
    this.handleAdd = this.handleAdd.bind(this);
    this.handleRemove = this.handleRemove.bind(this);
    this.initializeToscaPolicyModelsInfo();
  }

  initializeToscaPolicyModelsInfo() {
    var operationalPolicies = this.state.loopCache.getOperationalPolicies();
    var selectedPolicyModels = [];
    for (var policy in operationalPolicies) {
      var newRow = operationalPolicies[policy]["policyModel"];
      newRow["policyName"] = operationalPolicies[policy].name;
      selectedPolicyModels.push(newRow);
    }

    PolicyToscaService.getToscaPolicyModels().then(allToscaModels => {
      this.setState({
        toscaPolicyModelsData: allToscaModels,
        selectedPolicyModelsData: selectedPolicyModels
      });
    });
  }

  getToscaPolicyModelYaml(policyModelType, policyModelVersion) {
    if (typeof policyModelType !== "undefined") {
      PolicyToscaService.getToscaPolicyModelYaml(policyModelType, policyModelVersion).then(toscaYaml => {
        if (toscaYaml.length !== 0) {
          this.setState({ content: toscaYaml })
        } else {
          this.setState({ content: 'No Tosca model Yaml available' })
        }
      });
    } else {
      this.setState({ content: 'Please select Tosca model to view the details' })
    }
  }

  handleYamlContent(event) {
    this.setState({ content: event.target.value });
  }

  handleClose() {
    this.setState({ show: false });
    this.props.history.push('/');
  }

  renderAlert() {
    return (
      <div>
        <Alert variant="danger" show={ this.state.showFailAlert } onClose={ this.disableAlert } dismissible>
          { this.state.showMessage }
        </Alert>
      </div>
    );
  }

  handleAdd() {
    LoopService.addOperationalPolicyType(this.state.loopCache.getLoopName(), this.state.selectedRowData.policyModelType, this.state.selectedRowData.version)
      .then(pars => {
        this.props.loadLoopFunction(this.state.loopCache.getLoopName());
        this.handleClose();
      })
      .catch(error => {
        this.setState({ showFailAlert: true, showMessage: "Adding failed with error: " + error.message });
      });
  }

  handleRemove() {
    LoopService.removeOperationalPolicyType(this.state.loopCache.getLoopName(), this.state.selectedRowData.policyModelType, this.state.selectedRowData.version, this.state.selectedRowData.policyName);
    this.props.loadLoopFunction(this.state.loopCache.getLoopName());
    this.handleClose();
  }

  render() {
    return (
      <ModalStyled size="xl" show={ this.state.show } onHide={ this.handleClose } backdrop="static" keyboard={ false }>
        <Modal.Header closeButton>
          <Modal.Title>Modify Loop Operational Policies</Modal.Title>
        </Modal.Header>
        <Tabs id="controlled-tab-example" activeKey={ this.state.key } onSelect={ key => this.setState({ key, selectedRowData: {} }) }>
          <Tab eventKey="add" title="Add Operational Policies">
            <Modal.Body>
              <MaterialTable
                title={ "View Tosca Policy Models" }
                data={ this.state.toscaPolicyModelsData }
                columns={ this.state.toscaColumns }
                icons={ this.state.tableIcons }
                onRowClick={ (event, rowData) => {
                  this.getToscaPolicyModelYaml(rowData.policyModelType, rowData.version);
                  this.setState({ selectedRowData: rowData })
                } }
                options={ {
                  headerStyle: rowHeaderStyle,
                  rowStyle: rowData => ({
                    backgroundColor: (this.state.selectedRowData !== {} && this.state.selectedRowData.tableData !== undefined
                      && this.state.selectedRowData.tableData.id === rowData.tableData.id) ? '#EEE' : '#FFF'
                  })
                } }
              />
              <div>
                <TextModal value={ this.state.content } onChange={ this.handleYamlContent }/>
              </div>
            </Modal.Body>
            { this.renderAlert() }
          </Tab>
          <Tab eventKey="remove" title="Remove Operational Policies">
            <Modal.Body>
              <MaterialTable
                title={ "Tosca Policy Models already added" }
                data={ this.state.selectedPolicyModelsData }
                columns={ this.state.toscaColumns }
                icons={ this.state.tableIcons }
                onRowClick={ (event, rowData) => {
                  this.setState({ selectedRowData: rowData })
                } }
                options={ {
                  headerStyle: rowHeaderStyle,
                  rowStyle: rowData => ({
                    backgroundColor: (this.state.selectedRowData !== {} && this.state.selectedRowData.tableData !== undefined
                      && this.state.selectedRowData.tableData.id === rowData.tableData.id) ? '#EEE' : '#FFF'
                  })
                } }
              />
            </Modal.Body>
          </Tab>
        </Tabs>
        <Modal.Footer>
          <Button variant="secondary" type="null" onClick={ this.handleClose }>Cancel</Button>
          <Button variant="primary" disabled={ (this.state.key === "remove") } type="submit" onClick={ this.handleAdd }>Add</Button>
          <Button variant="primary" disabled={ (this.state.key === "add") } type="submit" onClick={ this.handleRemove }>Remove</Button>
        </Modal.Footer>

      </ModalStyled>
    );
  }
}
