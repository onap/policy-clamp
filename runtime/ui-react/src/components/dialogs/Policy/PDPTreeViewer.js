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
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import FolderIcon from '@material-ui/icons/Folder';
import FolderOpenIcon from '@material-ui/icons/FolderOpen';
import DescriptionIcon from '@material-ui/icons/Description';
// import JSONTree from 'react-json-tree';
import PolicyService from '../../../api/PolicyService';


export default class PDPTreeViewer extends React.Component {

    constructor(props, context) {
        super(props, context);
        // this.createPoliciesTree = this.createPoliciesTree.bind(this);
        this.renderTreeItems = this.renderTreeItems.bind(this);
    }

    state = {
        pdpData: this.props.pdpData,
    }


    componentDidUpdate(prevProps) {
        if (prevProps.pdpData !== this.props.pdpData) { //checking to see is previous props are not the same as new props
            this.setState({pdpData: this.props.pdpData}) //updating state to match the new data that is being called
            //setState calls the render method again to re render the data
        }
    }



    renderTreeItems(nodes) { //this method takes information from the json (nodes) and organizes it by making recursive calls
        if (typeof(nodes) === "undefined") {
            //add logic to deal with an undefined nodes variable
            return (<TreeItem key= "root" nodeID = "0" label = "root" />);
        }
        else { //not undefined
            return nodes.map(function(node){
                if (node.hasOwnProperty('pdpSubgroups')) { //level 1
                    var children = this.renderTreeItems(node.pdpSubgroups);
                    return this.renderTreeItemGroup(node, children);

                } else if (node.hasOwnProperty("pdpType")) { //level 2
                    var children = this.renderTreeItems(node.policies);
                    return this.renderTreeItemsSubgroup(node, children);

                } else if (node.hasOwnProperty("version")) { //level 3
                    return this.renderTreeItemPolicyInfo(node);
                }
            },this);
        }
    };

    //nodes.subgroups.forEach(renderTreeItems(nodes)); recursively is able to run thru the JSON and the if statements below tell it what to call to display the tree items

    // if (nodes.hasOwnProperty("pdpSubgroups") ){
    //     this.renderTreeItemGroup(nodes);
    // }
    // else if (nodes.hasOwnProperty("pdpType")){
    //     this.renderTreeItemsSubgroup(nodes);
    // }
    // else if (nodes.hasOwnProperty("properties")){
    //     this.renderTreeItemPolicyInfo(nodes);
    // }

    // nodes.forEach(node => renderTreeItems(node));

    // return (<TreeItem key={nodes.name} nodeId={nodes.name} label={nodes.name + "("+ nodes.pdpSubgroups + ")"} >
    //         {
    //             Array.isArray(nodes.subgroups) ? nodes.subgroups.map((node) => this.renderTreeItems(node)) : null
    //             //add logic to concatenate "name" and "version"
    //             //var policyName = nodes.subgroups.name
    //             //var policyVersion = nodes.subgroups.version
    //             //var policyInfo = {policyName + " " + policyVersion}
    //         }
    //     </TreeItem>);
    // };

    //if to detect

    renderTreeItemGroup(node, children){
        return (<TreeItem key={node.name} nodeId={node.name} label={node.name + "("+ node.pdpGroupState + ")"}>
            {children}
        </TreeItem>);
    }
    renderTreeItemsSubgroup(node, children){
        return (<TreeItem key={node.pdpType} nodeId={node.pdpType} label={node.pdpType}>
            {children}
        </TreeItem>);
    }

    renderTreeItemPolicyInfo(node){
        return (<TreeItem key={node.name + "_" + node.version} nodeId={node.name + "_" + node.version} label={node.name + " " + node.version}/>);
    }


    render() {
        return (
            <TreeView defaultExpanded={['root']} defaultCollapseIcon={<FolderOpenIcon />}
                      defaultExpandIcon={<FolderIcon />} defaultEndIcon={<DescriptionIcon />} multiSelect>
                {this.renderTreeItems(this.state.pdpData['groups'])}
            </TreeView>
        );
    }
}