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
import LoopCache from '../../../api/LoopCache';
import PolicyService from '../../../api/PolicyService';
import Select from 'react-select';

const ModalStyled = styled(Modal)`
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
        this.getAllPolicies();
    }

	handlePdpGroupChange(e) {
	}

    renderPdpGroupDropBox(dataRow) {
        let optionItems = [{label: "NONE", value: "NONE"}];
        let selectedItem = {label: "NONE", value: "NONE"};
        if (typeof dataRow.supportedPdpGroups !== "undefined") {
            optionItems.push(...dataRow["supportedPdpGroups"].map(entry =>
            {
                return { label: Object.keys(entry)[0], value: Object.keys(entry)[0] }
            }));
        }
        if (typeof dataRow.pdpGroupInfo !== "undefined") {
            selectedItem = {label: dataRow["pdpGroupInfo"]["pdpGroup"], value: dataRow["pdpGroupInfo"]["pdpGroup"]};
        }
        return (<Select value={selectedItem} options={optionItems} onChange={this.handlePdpGroupChange}/>);
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
                      onRowClick={(event, rowData) => {this.setState({selectedRow: rowData.tableData.id})}}
                      options={{
                          headerStyle:rowHeaderStyle,
                          rowStyle: rowData => ({
                          backgroundColor: (this.state.selectedRow !== -1 && this.state.selectedRow === rowData.tableData.id) ? '#EEE' : '#FFF'
                          })
                      }}
                />
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={this.handleClose}>Close</Button>
               </Modal.Footer>
      </ModalStyled>
      );
    }
  }
