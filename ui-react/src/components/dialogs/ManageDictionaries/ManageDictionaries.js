/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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


import React from 'react';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import styled from 'styled-components';
import TemplateMenuService from '../../../api/TemplateService';
import MaterialTable, {MTableToolbar} from "material-table";
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';
import Grid from '@material-ui/core/Grid';
import { forwardRef }  from 'react';
import AddBox from '@material-ui/icons/AddBox';
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import VerticalAlignTopIcon from '@material-ui/icons/VerticalAlignTop';
import VerticalAlignBottomIcon from '@material-ui/icons/VerticalAlignBottom';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';


const ModalStyled = styled(Modal)`
	background-color: transparent;
`
const cellStyle = { border: '1px solid black' };
const headerStyle = { backgroundColor: '#ddd',	border: '2px solid black'	};
const rowHeaderStyle = {backgroundColor:'#ddd',  fontSize: '15pt', text: 'bold', border: '1px solid black'};
var dictList = [];

function SelectSubDictType(props) {
	const {onChange} = props;
	const selectedValues = (e) => {
		var options = e.target.options;
		var SelectedDictTypes = '';
		for (var dictType = 0, values = options.length; dictType < values; dictType++) {
			if (options[dictType].selected) {
				SelectedDictTypes = SelectedDictTypes.concat(options[dictType].value);
				SelectedDictTypes = SelectedDictTypes.concat('|');
			}
		}
		SelectedDictTypes = SelectedDictTypes.slice(0,-1);
		onChange(SelectedDictTypes);
	}
	return(
		<div>
			<select multiple={true}  onChange={selectedValues}>
				<option value="string">string</option>
				<option value="number">number</option>
				<option value="datetime">datetime</option>
				<option value="map">map</option>
				<option value="json">json</option>
			</select>
		</div>
	)
}

function SubDict(props) {
	const {onChange} = props;
	const subDicts = [];
	subDicts.push('Default');
	for(var item in dictList) {
		if(dictList[item].secondLevelDictionary === 1) {
			subDicts.push(dictList[item].name);
		}
	};
	subDicts.push('');
 	var optionItems = subDicts.map(
		(item) => <option key={item}>{item}</option>
	  );
 	function selectedValue (e) {
		onChange(e.target.value);
	}
	return(
		<select onChange={selectedValue} >
			{optionItems}
		</select>
	)
}

export default class ManageDictionaries extends React.Component {
	constructor(props, context) {
		super(props, context);
		this.handleClose = this.handleClose.bind(this);
		this.getDictionary = this.getDictionary.bind(this);
		this.getDictionaryElements = this.getDictionaryElements.bind(this);
		this.clickHandler = this.clickHandler.bind(this);
		this.addDictionary = this.addDictionary.bind(this);
		this.deleteDictionary = this.deleteDictionary.bind(this);
		this.fileSelectedHandler = this.fileSelectedHandler.bind(this);
		this.state = {
			show: true,
			selectedFile: '',
			dictNameFlag: false,
			exportFilename: '',
			content: null,
			newDict: '',
			newDictItem: '',
			delDictItem: '',
			addDict: false,
			delData: '',
			delDict: false,
			validImport: false,
			dictionaryNames: [],
			dictionaryElements: [],
      tableIcons: {
		Add: forwardRef((props, ref) => <AddBox {...props} ref={ref} />),
        Check: forwardRef((props, ref) => <Check {...props} ref={ref} />),
        Clear: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
        Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref} />),
        DetailPanel: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
        Edit: forwardRef((props, ref) => <Edit {...props} ref={ref} />),
        Export: forwardRef((props, ref) => <VerticalAlignBottomIcon {...props} ref={ref} />),
        Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref} />),
        FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref} />),
        LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref} />),
        NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
        PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref} />),
        ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
        Search: forwardRef((props, ref) => <Search {...props} ref={ref} />),
        SortArrow: forwardRef((props, ref) => <ArrowUpward {...props} ref={ref} />),
        ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref} />),
        ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref} />)
      },
			dictColumns: [
				{
					title: "Dictionary Name", field: "name",editable: 'onAdd',
					cellStyle: cellStyle,
					headerStyle: headerStyle
				},
				{
					title: "Sub Dictionary ?", field: "secondLevelDictionary", lookup: {0: 'No', 1: 'Yes'},
					cellStyle: cellStyle,
					headerStyle: headerStyle
				},
				{
					title: "Dictionary Type", field: "subDictionaryType",lookup: {string: 'string', number: 'number'},
					cellStyle: cellStyle,
					headerStyle: headerStyle
				},
				{
					title: "Updated By", field: "updatedBy", editable: 'never',
					cellStyle: cellStyle,
					headerStyle: headerStyle
				},
				{
					title: "Last Updated Date", field: "updatedDate", editable: 'never',
					cellStyle: cellStyle,
					headerStyle: headerStyle
				}
			],
			dictElementColumns: [
				{
					title: "Element Short Name", field: "shortName",editable: 'onAdd',
					cellStyle: cellStyle,
					headerStyle: headerStyle
				},
        {
					title: "Element Name", field: "name",
					cellStyle: cellStyle,
					headerStyle: headerStyle
				},
				{
					title: "Element Description", field: "description",
					cellStyle: cellStyle,
					headerStyle: headerStyle
				 },
				 {
					title: "Element Type", field: "type",
					editComponent: props => (
						<div>
							<SelectSubDictType  value={props.value} onChange={props.onChange} />
						</div>
					),
					cellStyle: cellStyle,
					headerStyle: headerStyle
				 },
				 {  
				    title: "Sub-Dictionary", field: "subDictionary",
				      editComponent: props => (
						 <div>
							 <SubDict  value={props.value} onChange={props.onChange} />
						 </div>
				      ),
				    cellStyle: cellStyle,
				    headerStyle: headerStyle
				 },
				{     
					title: "Updated By", field: "updatedBy", editable: 'never',
					cellStyle: cellStyle,
					headerStyle: headerStyle
				},
				{
					title: "Updated Date", field: "updatedDate", editable: 'never',
					cellStyle: cellStyle,
					headerStyle: headerStyle
				}
			]
		}
	}

	componentWillMount() {
        this.getDictionary();
    }

    getDictionary() {
        TemplateMenuService.getDictionary().then(dictionaryNames => {
            this.setState({ dictionaryNames: dictionaryNames })
        });
    }

    getDictionaryElements(dictionaryName) {
        TemplateMenuService.getDictionaryElements(dictionaryName).then(dictionaryElements => {
            dictList = this.state.dictionaryNames;
            this.setState({ dictionaryElements: dictionaryElements.dictionaryElements});
        });
    }

    clickHandler(rowData)   {
        this.setState({
            dictNameFlag: false,
            addDict: false,
    });
    }

    handleClose() {
        this.setState({ show: false });
        this.props.history.push('/');
    }

    addDictionary() {
        var modifiedData = [];
        if(this.state.newDict !== '') {
            modifiedData = this.state.newDict;
        } else {
            modifiedData = {"name": this.state.dictionaryName, 'dictionaryElements': this.state.newDictItem};
        }
        if(this.state.newDictItem === '') {
            TemplateMenuService.insDictionary(modifiedData).then(resp => {
            });
        } else {
            TemplateMenuService.insDictionaryElements(modifiedData).then(resp => {
            });
        }
    }

    deleteDictionary() {
        var modifiedData = [];
        if(this.state.delData !== '') {
            modifiedData = this.state.delData.name;
        } else {
            modifiedData = {"name": this.state.dictionaryName, "shortName": this.state.delDictItem.shortName};
        }
        if(this.state.delDictItem === '') {
            TemplateMenuService.deleteDictionary(modifiedData).then(resp => {
            });
        } else {
            TemplateMenuService.deleteDictionaryElements(modifiedData).then(resp => {
            });
        }
    }

    fileSelectedHandler = (event) => {
        const text = this;
        var dictionaryElements = [];
        if (event.target.files[0].type === 'text/csv' ) {
            if (event.target.files && event.target.files[0]) {
                let reader = new FileReader();
                reader.onload = function(e) {
                    var dictElems = reader.result.split('\n');
                    var jsonObj = [];
                    var headers = dictElems[0].split(',');
                    for(var i = 0; i < dictElems.length; i++) {
                        var data = dictElems[i].split(',');
                        var obj = {};
                        for(var j = 0; j < data.length; j++) {
                            obj[headers[j].trim()] = data[j].trim();
                        }
                        jsonObj.push(obj);
                    }
                    JSON.stringify(jsonObj);
                    const dictKeys = ['Element Short Name','Element Name','Element Description','Element Type','Sub-Dictionary'];
                    const mandatoryKeys = [ 'Element Short Name', 'Element Name', 'Element Type' ];
                    const validTypes = ['string','number','datetime','json','map'];
                    if (!dictElems){
                        
                        text.setState({validData: false});
                    } else if (headers.length !== dictKeys.length){
                        text.setState({validImport: false});
                    } else {
                        var subDictionaries = [];
                        for(var item in dictList) {
                            if(dictList[item].secondLevelDictionary === 1) {
                                subDictionaries.push(dictList[item].name);
                            }
                        };
                        subDictionaries = subDictionaries.toString();
                        var row = 0;
                        for (var dictElem of jsonObj){
                            ++row;
                            for (var itemKey in dictElem){
                                var value = dictElem[itemKey].trim();
                                if (dictKeys.indexOf(itemKey) < 0){
                                    var errorMessage = 'unknown field name of, ' + itemKey + ', found in CSV header';
                                    text.setState({validImport: false});
                                    alert(errorMessage);
                                    break;
                                } else if (value === "" && mandatoryKeys.indexOf(itemKey) >= 0){
                                    errorMessage = 'value for ' + itemKey + ', at row #, ' + row + ', is empty but required';
                                    text.setState({validImport: false});
                                    alert(errorMessage);
                                    break;
                                } else if (itemKey === 'Element Type' && validTypes.indexOf(value) < 0 && row > 1) {
                                    errorMessage = 'invalid dictElemenType of ' + value + ' at row #' + row;
                                    text.setState({validImport: false});
                                    alert(errorMessage);
                                    break;
                                } else if (value !== "" && itemKey === 'Sub-Dictionary' && subDictionaries.indexOf(value) < 0 && row > 1) {
                                    errorMessage = 'invalid subDictionary of ' + value + ' at row #' + row;
                                    text.setState({validImport: false});
                                    alert(errorMessage);
                                }
                            }
                        }
                    }
                    const headerKeys = ['shortName','name','description','type','subDictionary'];

                    for(i = 1; i < dictElems.length; i++) {
                        data = dictElems[i].split(',');
                        obj = {};
                        for(j = 0; j < data.length; j++) {
                            obj[headerKeys[j].trim()] = data[j].trim();
                        }
                        dictionaryElements.push(obj);
                    }
                    text.setState({newDictItem: dictionaryElements, addDict: true});
                }
                reader.readAsText(event.target.files[0]);
            }
            this.setState({selectedFile: event.target.files[0]})
        } else {
            text.setState({validImport: false});
            alert('Please upload .csv extention files only.');
        }

    }
    
    render() {
        return (
            <ModalStyled size="xl" show={this.state.show} onHide={this.handleClose} backdrop="static" keyboard={false} >
                <Modal.Header closeButton>
                    <Modal.Title>Manage Dictionaries</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {!this.state.dictNameFlag? <MaterialTable
                        title={"Dictionary List"}
                        data={this.state.dictionaryNames}
                        columns={this.state.dictColumns}
                        icons={this.state.tableIcons}
                        onRowClick={(event, rowData) => {this.getDictionaryElements(rowData.name);this.setState({dictNameFlag: true, exportFilename: rowData.name, dictionaryName: rowData.name})}}
                        options={{
                            headerStyle: rowHeaderStyle,
                        }}
                        editable={{
                            onRowAdd: newData =>
                            new Promise((resolve, reject) => {
                                setTimeout(() => {
                                    {
                                        const dictionaryNames = this.state.dictionaryNames;
                                        var validData =  true;
                                        if(/[^a-zA-Z0-9-_.]/.test(newData.name)) {
                                            validData = false;
                                            alert('Please enter alphanumberic input. Only allowed special characters are:(period, hyphen, underscore)');
                                        }
                                        for (var i = 0; i < this.state.dictionaryNames.length; i++) {
                                            if (this.state.dictionaryNames[i].name === newData.name) {
                                                validData = false;
                                                alert(newData.name + ' dictionary name already exists')
                                            }
                                        }
                                        if(validData){
                                            dictionaryNames.push(newData);
                                            this.setState({ dictionaryNames }, () => resolve());
                                            this.setState({addDict: true, newDict: newData});
                                        }
                                    }
                                    resolve();
                                }, 1000);
                            }),
                            onRowUpdate: (newData, oldData) =>
                            new Promise((resolve, reject) => {
                                setTimeout(() => {
                                    {
                                        const dictionaryNames = this.state.dictionaryNames;
                                        var validData =  true;
                                        if(/[^a-zA-Z0-9-_.]/.test(newData.name)) {
                                            validData = false;
                                            alert('Please enter alphanumberic input. Only allowed special characters are:(period, hyphen, underscore)');
                                        }
                                        if(validData){
                                            const index = dictionaryNames.indexOf(oldData);
                                            dictionaryNames[index] = newData;
                                            this.setState({ dictionaryNames }, () => resolve());
                                            this.setState({addDict: true, newDict: newData});
                                        }
                                    }
                                    resolve();
                                }, 1000);
                            }),
                            onRowDelete: oldData =>
                new Promise((resolve, reject) => {
                                setTimeout(() => {
                                    {
                                        let data = this.state.dictionaryNames;
                    const index = data.indexOf(oldData);
                    data.splice(index, 1);
                    this.setState({ data }, () => resolve());
                                        this.setState({delDict: true, delData: oldData})
                    }
                    resolve()
                }, 1000)
                })
                        }}
                        />:""
                    }
                    {this.state.dictNameFlag? <MaterialTable
                        title={"Dictionary Elements List"}
                        data={this.state.dictionaryElements}
                        columns={this.state.dictElementColumns}
                        icons={this.state.tableIcons}
                        options={{
                            exportButton: true,
                            exportFileName: this.state.exportFilename,
                            headerStyle:{backgroundColor:'white',  fontSize: '15pt', text: 'bold', border: '1px solid black'}
                        }}
                        components={{
                            Toolbar: props => (
                                <div>
                                    <MTableToolbar {...props} />
                                <div>
                                    <Grid item container xs={12} alignItems="flex-end" direction="column" justify="flex-end">
                                        <Tooltip title="Import" placement = "bottom">
                                            <IconButton aria-label="import" onClick={() => this.fileUpload.click()}>
                                                <VerticalAlignTopIcon />
                                            </IconButton>
                                        </Tooltip>
                                    </Grid>
                                </div>
                                <input type="file" ref={(fileUpload) => {this.fileUpload = fileUpload;}} style={{ visibility: 'hidden'}} onChange={this.fileSelectedHandler} />
                                </div>
                            )
                        }}
                        editable={{
                            onRowAdd: newData =>
                            new Promise((resolve, reject) => {
                                setTimeout(() => {
                                    {
                                        const dictionaryElements = this.state.dictionaryElements;
                                        var validData =  true;
                                        for (var i = 0; i < this.state.dictionaryElements.length; i++) {
                                            if (this.state.dictionaryElements[i].shortName === newData.shortName) {
                                                validData = false;
                                                alert(newData.shortname + 'short name already exists')
                                            }
                                        }
                                        if(/[^a-zA-Z0-9-_.]/.test(newData.shortName)) {
                                            validData = false;
                                            alert('Please enter alphanumberic input. Only allowed special characters are:(period, hyphen, underscore)');
                                        }
                                        if(!newData.type){
                                            validData = false;
                                            alert('Element Type cannot be null');
                                        }
                                        if(validData){
                                            dictionaryElements.push(newData);
                                            this.setState({ dictionaryElements }, () => resolve());
                                            this.setState({addDict: true, newDictItem: [newData]});
                                        }
                                    }
                                    resolve();
                                }, 1000);
                            }),
                            onRowUpdate: (newData, oldData) =>
                            new Promise((resolve, reject) => {
                                setTimeout(() => {
                                    {
                                        const dictionaryElements = this.state.dictionaryElements;
                                        var validData =  true;
                                        if(!newData.type){
                                            validData = false;
                                            alert('Element Type cannot be null');
                                        }
                                        if(validData){
                                            const index = dictionaryElements.indexOf(oldData);
                                            dictionaryElements[index] = newData;
                                            this.setState({ dictionaryElements }, () => resolve());
                                            this.setState({addDict: true, newDictItem: [newData]});
                                        }
                                    }
                                    resolve();
                                }, 1000);
                            }),
                            onRowDelete: oldData =>
                new Promise((resolve, reject) => {
                                setTimeout(() => {
                                    {
                                        let data = this.state.dictionaryElements;
                    const index = data.indexOf(oldData);
                    data.splice(index, 1);
                    this.setState({ data }, () => resolve());
                                        this.setState({delDict: true, delDictItem: oldData})
                    }
                    resolve()
                }, 1000)
                })
                        }}
                        />:""
                    }
                    {this.state.dictNameFlag?<button onClick={this.clickHandler} style={{marginTop: '25px'}}>Go Back to Dictionaries List</button>:""}
                    {this.state.addDict && this.addDictionary()}
                    {this.state.delDict && this.deleteDictionary()}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" type="null" onClick={this.handleClose}>Close</Button>
                </Modal.Footer>
            </ModalStyled>
        );
    }
}					
