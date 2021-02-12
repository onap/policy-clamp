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
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Search from '@material-ui/icons/Search';
import MaterialTable from "material-table";
import PolicyService from '../../../api/PolicyService';
import PolicyToscaService from '../../../api/PolicyToscaService';
import Select from 'react-select';
import JSONEditor from '@json-editor/json-editor';

const ModalStyled = styled(Modal)`
	@media (min-width: 1200px) {
		.modal-xl {
			max-width: 96%;
		}
	}
    background-color: transparent;
`

const standardCellStyle = { border: '1px solid black' };
const cellPdpGroupStyle = { backgroundColor: '#039be5', color: '#FFF', border: '1px solid black'};
const headerStyle = { backgroundColor: '#ddd',    border: '2px solid black' };
const rowHeaderStyle = {backgroundColor:'#ddd',  fontSize: '15pt', text: 'bold', border: '1px solid black'};

export default class ViewAllPolicies extends React.Component {
  state = {
        show: true,
        content: 'Please select a policy to display it',
        selectedRow: -1,
        policiesListData: [],
        policyColumnsDefinition: [
            {
                title: "#", field: "index", render: rowData => rowData.tableData.id + 1,
                cellStyle: standardCellStyle,
                headerStyle: headerStyle
            },
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
                render: rowData => this.renderPdpGroupDropBox(rowData)
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
            FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref} />),
            LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref} />),
            NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
            PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref} />),
            ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
            Search: forwardRef((props, ref) => <Search {...props} ref={ref} />),
            SortArrow: forwardRef((props, ref) => <ArrowUpward {...props} ref={ref} />)
        }
    };

    constructor(props, context) {
        super(props, context);
        this.handleClose = this.handleClose.bind(this);
        this.renderPdpGroupDropBox = this.renderPdpGroupDropBox.bind(this);
        this.handlePdpGroupChange = this.handlePdpGroupChange.bind(this);
        this.createJsonEditor = this.createJsonEditor.bind(this);
        this.getAllPolicies();

    }

    handlePdpGroupChange(e) {
        let pdpSplit = e.value.split("/");
        let selectedPdpGroup = pdpSplit[0];
        let selectedSubPdpGroup = pdpSplit[1];
        if (typeof selectedSubPdpGroup !== "undefined") {
            let temp = this.state.policiesListData;
            temp[this.state.selectedRow]["pdpGroupInfo"] = {"pdpGroup":selectedPdpGroup,"pdpSubGroup":selectedSubPdpGroup};
            this.setState({policiesListData: temp});
        } else {
            delete this.state.policiesListData[this.state.selectedRow]["pdpGroupInfo"];
        }
    }

    createJsonEditor(toscaModel, editorData) {
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
        {   schema: toscaModel,
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
                selectedRow: rowData.tableData.id,
                selectedRowJsonSchema: respJsonPolicyTosca,
                selectedRowPolicyProperties: rowData["properties"],
                jsonEditorForPolicy: this.createJsonEditor(respJsonPolicyTosca, rowData["properties"])
                });
    	});
    }

    render() {
    return (
            <ModalStyled size="xl" show={this.state.show} onHide={this.handleClose} backdrop="static"  keyboard={false}>
                <Modal.Header closeButton>
                </Modal.Header>
                <Modal.Body>
                   <MaterialTable
                      title={"View All Policies in Policy Engine"}
                      data={this.state.policiesListData}
                      columns={this.state.policyColumnsDefinition}
                      icons={this.state.tableIcons}
                      onRowClick={(event, rowData) => {this.handleOnRowClick(rowData)}}
                      options={{
                          headerStyle:rowHeaderStyle,
                          rowStyle: rowData => ({
                          backgroundColor: (this.state.selectedRow !== -1 && this.state.selectedRow === rowData.tableData.id) ? '#EEE' : '#FFF'
                          })
                      }}
                />
                <div id="policy-editor" />
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={this.handleClose}>Close</Button>
               </Modal.Footer>
      </ModalStyled>
      );
    }
  }