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
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Switch from '@material-ui/core/Switch';
import MaterialTable from "material-table";
import PolicyService from '../../../api/PolicyService';
import PolicyToscaService from '../../../api/PolicyToscaService';
import Select from 'react-select';
import JSONEditor from '@json-editor/json-editor';
import OnapUtils from '../../../utils/OnapUtils';
import Alert from 'react-bootstrap/Alert';

const DivWhiteSpaceStyled = styled.div`
	white-space: pre;
`

const ModalStyled = styled(Modal)`
    @media (min-width: 1200px) {
        .modal-xl {
            max-width: 96%;
        }
    }
    background-color: transparent;
`
const JsonEditorDiv = styled.div`
    margin-top: 20px;
    background-color: ${props => props.theme.toscaTextareaBackgroundColor};
    text-align: justify;
    font-size: ${props => props.theme.toscaTextareaFontSize};
    width: 100%;
    height: 30%;
    border: 1px solid black;
`


const standardCellStyle = { border: '1px solid black' };
const cellPdpGroupStyle = { backgroundColor: '#039be5', color: '#FFF', border: '1px solid black'};
const headerStyle = { backgroundColor: '#ddd',    border: '2px solid black' };
const rowHeaderStyle = {backgroundColor:'#ddd',  fontSize: '15pt', text: 'bold', border: '1px solid black'};

export default class ViewAllPolicies extends React.Component {
  state = {
        show: true,
        content: 'Please select a policy to display it',
        selectedRowId: -1,
        policiesListData: [],
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
        tableIcons: {
            Add: forwardRef((props, ref) => <AddBox {...props} ref={ref} />),
            Check: forwardRef((props, ref) => <Check {...props} ref={ref} />),
            Clear: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
            Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref} />),
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
        this.createJsonEditor = this.createJsonEditor.bind(this);
        this.handlePrefixGrouping = this.handlePrefixGrouping.bind(this);
        this.handleDeletePolicy = this.handleDeletePolicy.bind(this);
        this.handleUpdatePolicy = this.handleUpdatePolicy.bind(this);
        this.handleCreateNewVersion = this.handleCreateNewVersion.bind(this);
        this.disableAlert = this.disableAlert.bind(this);
        this.getAllPolicies();

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

    createJsonEditor(toscaModel, editorData) {
        document.getElementById("policy-editor").innerHTML = "";
        JSONEditor.defaults.themes.myBootstrap4 = JSONEditor.defaults.themes.bootstrap4.extend({
                getTab: function(text,tabId) {
                    var liel = document.createElement('li');
                    liel.classList.add('nav-item');
                    var ael = document.createElement("a");
                    ael.classList.add("nav-link");
                    ael.setAttribute("style",'padding:10px;max-width:160px;');
                    ael.setAttribute("href", "#" + tabId);
                    ael.setAttribute('data-toggle', 'tab');
                    text.setAttribute("style",'word-wrap:break-word;');
                    ael.appendChild(text);
                    liel.appendChild(ael);
                    return liel;
                }
            });
        return new JSONEditor(document.getElementById("policy-editor"),
        {
              schema: toscaModel,
              startval: editorData,
              theme: 'myBootstrap4',
              object_layout: 'grid',
              disable_properties: false,
              disable_edit_json: false,
              disable_array_reorder: true,
              disable_array_delete_last_row: true,
              disable_array_delete_all_rows: false,
              array_controls_top: true,
              keep_oneof_values: false,
              collapsed:true,
              show_errors: 'always',
              display_required_only: false,
              show_opt_in: false,
              prompt_before_delete: true,
              required_by_default: false
        })
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

    handleOnRowClick(rowData) {
        PolicyToscaService.getToscaPolicyModel(rowData["type"], rowData["type_version"]).then(respJsonPolicyTosca => {
            this.setState({
                selectedRowId: rowData.tableData.id,
                selectedRowIdJsonSchema: respJsonPolicyTosca,
                selectedRowIdPolicyProperties: rowData["properties"],
                jsonEditorForPolicy: this.createJsonEditor(respJsonPolicyTosca, rowData["properties"])
                });
        });
    }

    handlePrefixGrouping(event) {
        this.setState({prefixGrouping: event.target.checked});
    }

    handleDeletePolicy(event, rowData) {
        return null;
    }

    customValidation(editorData) {
        // method for sub-classes to override with customized validation
        return [];
    }

    handleCreateNewVersion() {
        var editorData = this.state.jsonEditorForPolicy.getValue();
        var errors = this.state.jsonEditorForPolicy.validate();
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
            let newPolicy = JSON.parse(JSON.stringify(this.state.policiesListData[this.state.selectedRowId]));
            newPolicy["properties"] = editorData;
            let newVersion = this.bumpVersion(newPolicy["version"]);
            newPolicy["version"] = newVersion;
            newPolicy["metadata"]["policy-version"] = newVersion;
            // Remove stuff added by UI
            delete newPolicy["tableData"];
            PolicyService.createNewPolicy(newPolicy["type"], newPolicy["type_version"], newPolicy).then(respPolicyCreation => {
                if (respPolicyCreation === "") {
                    //it indicates a failure
                    this.setState({
                        showFailAlert: true,
                        showMessage: 'Policy Creation Failure'
                    });
                } else {
                    this.setState({
                        showSuccessAlert: true,
                        showMessage: 'Policy in version ' + newVersion + ' created successfully'
                    });
                }
            })
        }
    }

    bumpVersion(versionToBump) {
        let semVer = versionToBump.split(".");
        return parseInt(semVer[0])+1 + "." + semVer[1] + "." + semVer[2];
    }

    handleUpdatePolicy() {
        this.setState({ show: false });
        this.props.history.push('/')
    }

    disableAlert() {
        this.setState ({ showSuccessAlert: false, showFailAlert: false });
    }

    render() {
    return (
            <ModalStyled size="xl" show={this.state.show} onHide={this.handleClose} backdrop="static" keyboard={false}>
                <Modal.Header closeButton>
                </Modal.Header>

                <Modal.Body>
                  <FormControlLabel
                        control={<Switch checked={this.state.prefixGrouping} onChange={this.handlePrefixGrouping} />}
                        label="Group by prefix"
                      />
                   <MaterialTable
                      title={"View All Policies in Policy Engine"}
                      data={this.state.policiesListData}
                      columns={this.state.policyColumnsDefinition}
                      icons={this.state.tableIcons}
                      onRowClick={(event, rowData) => {this.handleOnRowClick(rowData)}}
                      options={{
                          grouping: true,
                          exportButton: true,
                          headerStyle:rowHeaderStyle,
                          rowStyle: rowData => ({
                            backgroundColor: (this.state.selectedRowId !== -1 && this.state.selectedRowId === rowData.tableData.id) ? '#EEE' : '#FFF'
                          })
                      }}
                      actions={[
                          {
                            icon: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref} />),
                            tooltip: 'Delete Policy',
                            onClick: (event, rowData) => this.handleDeletePolicy(event, rowData)
                          }
                      ]}
                />
                <JsonEditorDiv>
                    <h5>Policy Properties Editor</h5>
                    <div id="policy-editor" title="Policy Properties"/>
                    <Button variant="secondary" title="Create a new policy version from the defined parameters"
                        onClick={this.handleCreateNewVersion}>Create New Version</Button>
                    <Button variant="secondary" title="Update the current policy version, BE CAREFUL this will undeploy the policy from PDP, delete it and then recreate the policy"
                        onClick={this.handleUpdatePolicy}>Update Current Version</Button>
                </JsonEditorDiv>
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
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={this.handleClose}>Close</Button>
               </Modal.Footer>
      </ModalStyled>
      );
    }
  }