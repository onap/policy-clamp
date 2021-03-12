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

import React, { forwardRef } from 'react'
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import styled from 'styled-components';
import AddBox from '@material-ui/icons/AddBox';
import ArrowDownward from '@material-ui/icons/ArrowDownward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import DeleteRoundedIcon from '@material-ui/icons/DeleteRounded';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import DehazeIcon from '@material-ui/icons/Dehaze';
import ArrowForwardIosIcon from '@material-ui/icons/ArrowForwardIos';
import AddIcon from '@material-ui/icons/Add';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Switch from '@material-ui/core/Switch';
import MaterialTable from "material-table";
import PolicyService from '../../../api/PolicyService';
import PolicyToscaService from '../../../api/PolicyToscaService';
import Select from 'react-select';
import Alert from 'react-bootstrap/Alert';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import PolicyEditor from './PolicyEditor';
import ToscaViewer from './ToscaViewer';

const DivWhiteSpaceStyled = styled.div`
    white-space: pre;
`

const ModalStyled = styled(Modal)`
    @media (min-width: 1000px) {
        .modal-xl {
            max-width: 96%;
        }
    }
    background-color: transparent;
`
const DetailedRow = styled.div`
    margin: 0 auto;
    background-color: ${props => props.theme.policyEditorBackgroundColor};
    font-size: ${props => props.theme.policyEditorFontSize};
    width: 97%;
    margin-left: auto;
    margin-right: 0;
`


const standardCellStyle = { backgroundColor: '#039be5', color: '#FFF', border: '1px solid black' };
const cellPdpGroupStyle = { backgroundColor: '#039be5', color: '#FFF', border: '1px solid black'};
const headerStyle = { backgroundColor: '#ddd',    border: '2px solid black' };
const rowHeaderStyle = {backgroundColor:'#ddd',  fontSize: '15pt', text: 'bold', border: '1px solid black'};

export default class ViewAllPolicies extends React.Component {
  state = {
        show: true,
        content: 'Please select a policy to display it',
        selectedRowId: -1,
        policiesListData: [],
        toscaModelsListData: [],
        jsonEditorForPolicy: new Map(),
        prefixGrouping: false,
        showSuccessAlert: false,
        showFailAlert: false,
        policyColumnsDefinition: [
            {
                title: "Policy Name", field: "name",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Policy Version", field: "version",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Policy Type", field: "type",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Policy Type Version", field: "type_version",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Deployed in PDP", field: "pdpGroupInfo.pdpGroup",
                cellStyle: cellPdpGroupStyle,
                headerStyle: headerStyle,
                render: rowData => this.renderPdpGroupDropBox(rowData),
                grouping: false
            },
            {
                title: "PDP Group", field: "pdpGroupInfo.pdpGroup",
                cellStyle: cellPdpGroupStyle,
                headerStyle: headerStyle
            },
            {
                title: "PDP SubGroup", field: "pdpGroupInfo.pdpSubGroup",
                cellStyle: cellPdpGroupStyle,
                headerStyle: headerStyle
            }
        ],
        toscaColumnsDefinition: [
            {
                title: "Policy Model Type", field: "policyModelType",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Policy Acronym", field: "policyAcronym",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Version", field: "version",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Uploaded By", field: "updatedBy",
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
            {
                title: "Uploaded Date", field: "updatedDate", editable: 'never',
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            }
        ],
        tableIcons: {
            Add: forwardRef((props, ref) => <AddBox {...props} ref={ref} />),
            Check: forwardRef((props, ref) => <Check {...props} ref={ref} />),
            Clear: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
            Delete: forwardRef((props, ref) => <DeleteRoundedIcon {...props} ref={ref} />),
            DetailPanel: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
            Edit: forwardRef((props, ref) => <Edit {...props} ref={ref} />),
            Export: forwardRef((props, ref) => <SaveAlt {...props} ref={ref} />),
            Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref} />),
            FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref} />),
            LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref} />),
            NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
            PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref} />),
            ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
            Search: forwardRef((props, ref) => <Search {...props} ref={ref} />),
            SortArrow: forwardRef((props, ref) => <ArrowDownward {...props} ref={ref} />),
            ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref} />),
            ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref} />)
        }
    };

    constructor(props, context) {
        super(props, context);
        this.handleClose = this.handleClose.bind(this);
        this.renderPdpGroupDropBox = this.renderPdpGroupDropBox.bind(this);
        this.handlePdpGroupChange = this.handlePdpGroupChange.bind(this);
        this.handlePrefixGrouping = this.handlePrefixGrouping.bind(this);
        this.handleDeletePolicy = this.handleDeletePolicy.bind(this);
        this.disableAlert = this.disableAlert.bind(this);
        this.getAllPolicies = this.getAllPolicies.bind(this);
        this.getAllToscaModels = this.getAllToscaModels.bind(this);
        this.getAllPolicies();
        this.getAllToscaModels();
    }

    getAllToscaModels() {
        PolicyToscaService.getToscaPolicyModels().then(toscaModelsList => {
            this.setState({ toscaModelsListData: toscaModelsList });
        });
    }

    handlePdpGroupChange(e) {
        let pdpSplit = e.value.split("/");
        let selectedPdpGroup = pdpSplit[0];
        let selectedSubPdpGroup = pdpSplit[1];
        if (typeof selectedSubPdpGroup !== "undefined") {
            let temp = this.state.policiesListData;
            temp[this.state.selectedRowId]["pdpGroupInfo"] = {"pdpGroup":selectedPdpGroup,"pdpSubGroup":selectedSubPdpGroup};
            this.setState({policiesListData: temp});
        } else {
            delete this.state.policiesListData[this.state.selectedRowId]["pdpGroupInfo"];
        }
    }

    renderPdpGroupDropBox(dataRow) {
        let optionItems = [{label: "NOT DEPLOYED", value: "NOT DEPLOYED"}];
        let selectedItem = {label: "NOT DEPLOYED", value: "NOT DEPLOYED"};
        if (typeof dataRow.supportedPdpGroups !== "undefined") {
            for (const pdpGroup of dataRow["supportedPdpGroups"]) {
                for (const pdpSubGroup of Object.values(pdpGroup)[0]) {
                    optionItems.push({ label: Object.keys(pdpGroup)[0]+"/"+pdpSubGroup,
                    value: Object.keys(pdpGroup)[0]+"/"+pdpSubGroup });
                }
            }
        }
        if (typeof dataRow.pdpGroupInfo !== "undefined") {
            selectedItem = {label: dataRow["pdpGroupInfo"]["pdpGroup"]+"/"+dataRow["pdpGroupInfo"]["pdpSubGroup"],
            value: dataRow["pdpGroupInfo"]["pdpGroup"]+"/"+dataRow["pdpGroupInfo"]["pdpSubGroup"]};
        }
        return (<div style={{width: '250px'}}><Select value={selectedItem} options={optionItems} onChange={this.handlePdpGroupChange}/></div>);
    }

    getAllPolicies() {
        PolicyService.getPoliciesList().then(allPolicies => {
            this.setState({ policiesListData: allPolicies["policies"] })
        });
    }

    handleClose() {
        this.setState({ show: false });
        this.props.history.push('/')
    }

    handlePrefixGrouping(event) {
        this.setState({prefixGrouping: event.target.checked});
    }

    handleDeletePolicy(event, rowData) {
        PolicyService.deletePolicy(rowData["type"], rowData["type_version"], rowData["name"],rowData["version"]).then(
            respPolicyDeletion => {
                if (typeof(respPolicyDeletion) === "undefined") {
                    //it indicates a failure
                    this.setState({
                        showFailAlert: true,
                        showMessage: 'Policy Deletion Failure'
                    });
                } else {
                    this.setState({
                        showSuccessAlert: true,
                        showMessage: 'Policy successfully Deleted'
                    });
                }
                this.getAllPolicies();
            }
        )
    }

    disableAlert() {
        this.setState ({ showSuccessAlert: false, showFailAlert: false });
    }

    renderPoliciesTab() {
        return (
                <Tab eventKey="policies" title="Policies in Policy Framework">
                        <Modal.Body>
                          <FormControlLabel
                                control={<Switch checked={this.state.prefixGrouping} onChange={this.handlePrefixGrouping} />}
                                label="Group by prefix"
                              />
                           <MaterialTable
                              title={"Policies"}
                              data={this.state.policiesListData}
                              columns={this.state.policyColumnsDefinition}
                              icons={this.state.tableIcons}
                              onRowClick={(event, rowData, togglePanel) => togglePanel()}
                              options={{
                                  grouping: true,
                                  exportButton: true,
                                  headerStyle:rowHeaderStyle,
                                  rowStyle: rowData => ({
                                    backgroundColor: (this.state.selectedRowId !== -1 && this.state.selectedRowId === rowData.tableData.id) ? '#EEE' : '#FFF'
                                  }),
                                  actionsColumnIndex: -1
                              }}
                              detailPanel={[
                                {
                                  icon: ArrowForwardIosIcon,
                                  tooltip: 'Show Configuration',
                                  render: rowData => {
                                    return (
                                        <DetailedRow>
                                            <PolicyEditor policyModelType={rowData["type"]} policyModelTypeVersion={rowData["type_version"]} policyName={rowData["name"]} policyVersion={rowData["version"]} policyProperties={rowData["properties"]} policyUpdateFunction={this.getAllPolicies} />
                                        </DetailedRow>
                                    )
                                  },
                                },
                                {
                                  icon: DehazeIcon,
                                  tooltip: 'Show Raw Data',
                                  render: rowData => {
                                    return (
                                        <DetailedRow>
                                            <pre>{JSON.stringify(rowData, null, 2)}</pre>
                                        </DetailedRow>
                                    )
                                  },
                                },
                              ]}
                              actions={[
                                  {
                                    icon: forwardRef((props, ref) => <DeleteRoundedIcon {...props} ref={ref} />),
                                    tooltip: 'Delete Policy',
                                    onClick: (event, rowData) => this.handleDeletePolicy(event, rowData)
                                  }
                              ]}
                           />
                        </Modal.Body>
                    </Tab>
        );
    }

    renderToscaTab() {
        return (
                    <Tab eventKey="tosca models" title="Tosca Models in Policy Framework">
                        <Modal.Body>
                          <FormControlLabel
                                control={<Switch checked={this.state.prefixGrouping} onChange={this.handlePrefixGrouping} />}
                                label="Group by prefix"
                              />
                           <MaterialTable
                              title={"Tosca Models"}
                              data={this.state.toscaModelsListData}
                              columns={this.state.toscaColumnsDefinition}
                              icons={this.state.tableIcons}
                              onRowClick={(event, rowData, togglePanel) => togglePanel()}
                              options={{
                                  grouping: true,
                                  exportButton: true,
                                  headerStyle:rowHeaderStyle,
                                  rowStyle: rowData => ({
                                    backgroundColor: (this.state.selectedRowId !== -1 && this.state.selectedRowId === rowData.tableData.id) ? '#EEE' : '#FFF'
                                  }),
                                  actionsColumnIndex: -1
                              }}
                              detailPanel={[
                                {
                                  icon: ArrowForwardIosIcon,
                                  tooltip: 'Show Tosca',
                                  render: rowData => {
                                    return (
                                        <DetailedRow>
                                            <ToscaViewer toscaData={rowData} />
                                        </DetailedRow>
                                    )
                                  },
                                },
                                {
                                  icon: DehazeIcon,
                                  tooltip: 'Show Raw Data',
                                  render: rowData => {
                                    return (
                                        <DetailedRow>
                                            <pre>{JSON.stringify(rowData, null, 2)}</pre>
                                        </DetailedRow>
                                    )
                                  },
                                },
                                {
                                  icon: AddIcon,
                                  tooltip: 'Create a policy from this model',
                                  render: rowData => {
                                    return (
                                        <DetailedRow>
                                            <PolicyEditor policyModelType={rowData["policyModelType"]} policyModelTypeVersion={rowData["version"]} policyProperties={{}} policyUpdateFunction={this.getAllPolicies} />
                                        </DetailedRow>
                                    )
                                  },
                                },
                              ]}
                           />
                        </Modal.Body>
                    </Tab>
        );
    }

    render() {
    return (
            <ModalStyled size="xl" show={this.state.show} onHide={this.handleClose} backdrop="static" keyboard={false}>
                <Modal.Header closeButton>
                </Modal.Header>
                <Tabs id="controlled-tab-example" activeKey={this.state.key} onSelect={key => this.setState({ key, selectedRowData: {} })}>
                    {this.renderPoliciesTab()}
                    {this.renderToscaTab()}
                </Tabs>
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
                <Modal.Footer>
                    <Button variant="secondary" onClick={this.handleClose}>Close</Button>
               </Modal.Footer>
      </ModalStyled>
      );
    }
  }