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

import React, { forwardRef } from 'react'
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import styled from 'styled-components';
import TemplateService from '../../../api/TemplateService';
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Search from '@material-ui/icons/Search';
import MaterialTable from "material-table";

const ModalStyled = styled(Modal)`
	background-color: transparent;
`
const LoopViewSvgDivStyled = styled.div`
	overflow: hidden;
	background-color: ${props => (props.theme.loopViewerBackgroundColor)};
	border-color: ${props => (props.theme.loopViewerHeaderColor)};
	margin-left: auto;
	margin-right:auto;
	text-align: center;
	margin-top: 20px;
`
const SvgContainerDivStyled = styled.div`
	border: 1px solid;
`

const cellStyle = { border: '1px solid black' };
const headerStyle = { backgroundColor: '#ddd',	border: '2px solid black'	};
const rowHeaderStyle = {backgroundColor:'#ddd',  fontSize: '15pt', text: 'bold', border: '1px solid black'};

export default class ViewLoopTemplatesModal extends React.Component {
  state = {
        show: true,
		content: 'Please select a loop template to display it',
		selectedRow: -1,
		loopTemplateData: [],
		loopTemplateColumnsDefinition: [
			{ title: "#", field: "index", render: rowData => rowData.tableData.id + 1,
				cellStyle: cellStyle,
				headerStyle: headerStyle
			},
			{ title: "Template Name", field: "name",
				cellStyle: cellStyle,
				headerStyle: headerStyle
			},
			{ title: "Service Model Name", field: "modelService.serviceDetails.name",
				cellStyle: cellStyle,
				headerStyle: headerStyle
			},
			{ title: "Loop Type Allowed", field: "allowedLoopType",
				cellStyle: cellStyle,
				headerStyle: headerStyle
			},
			{ title: "# Instances Allowed", field: "maximumInstancesAllowed",
				cellStyle: cellStyle,
				headerStyle: headerStyle
			},
			{ title: "Modified Date", field: "updatedDate", editable: 'never',
				cellStyle: cellStyle,
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
		this.getBlueprintMicroServiceTemplate = this.getBlueprintMicroServiceTemplate.bind(this);
		this.getBlueprintMicroServiceTemplates();
	}

	getBlueprintMicroServiceTemplates() {
		TemplateService.getBlueprintMicroServiceTemplates().then(loopTemplateData => {
		this.setState({ loopTemplateData: loopTemplateData })
		});
	}

	getBlueprintMicroServiceTemplate(templateName) {
	    if (typeof templateName !== "undefined") {
		    TemplateService.getBlueprintMicroServiceTemplateSvg(templateName).then(svgXml => {
				if (svgXml.length !== 0) {
					this.setState({ content: svgXml })
				} else {
					this.setState({ content: 'Please select a loop template to view the details' })

				}
			});
		} else {
          	this.setState({ content: 'Please select a loop template to view the details' })
          	}
    }

	handleClose() {
		this.setState({ show: false });
		this.props.history.push('/')
	}

	render() {
    return (
    		<ModalStyled size="xl" show={this.state.show} onHide={this.handleClose} backdrop="static">
    			<Modal.Header closeButton>
    			</Modal.Header>
    			<Modal.Body>
    			   <MaterialTable
    				  title={"View Blueprint MicroService Templates"}
    				  data={this.state.loopTemplateData}
					  columns={this.state.loopTemplateColumnsDefinition}
					  icons={this.state.tableIcons}
    			      onRowClick={(event, rowData) => {this.getBlueprintMicroServiceTemplate(rowData.name);this.setState({selectedRow: rowData.tableData.id})}}
					  options={{
					      headerStyle:rowHeaderStyle,
					      rowStyle: rowData => ({
					      backgroundColor: (this.state.selectedRow !== -1 && this.state.selectedRow === rowData.tableData.id) ? '#EEE' : '#FFF'
                          })
                      }}
                  />
	              <SvgContainerDivStyled>
		            <LoopViewSvgDivStyled dangerouslySetInnerHTML={{ __html: this.state.content }} value={this.state.content} >
					</LoopViewSvgDivStyled>
	              </SvgContainerDivStyled>
                </Modal.Body>
                <Modal.Footer>
                	<Button variant="secondary" onClick={this.handleClose}>Close</Button>
               </Modal.Footer>
      </ModalStyled>
      );
    }
  }
