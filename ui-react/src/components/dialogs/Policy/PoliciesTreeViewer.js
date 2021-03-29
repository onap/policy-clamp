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
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import LocalParkingIcon from '@material-ui/icons/LocalParking';


export default class PoliciesTreeViewer extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.createPoliciesTree = this.createPoliciesTree.bind(this);
        this.handleTreeItemClick = this.handleTreeItemClick.bind(this);
    }

    state = {
            display: (this.props.show) ? "block":"none",
            policiesTreeData: this.createPoliciesTree(this.props.policiesData),
    }

    componentDidUpdate(prevProps) {
      if (prevProps.policiesData !== this.props.policiesData) {
        this.setState({policiesTreeData: this.createPoliciesTree(this.props.policiesData)})
      }
    }

    createPoliciesTree(policiesArray) {
        // put my policies array in a Json
        let nodeId=1;
        let policyTree = {id:nodeId++, policyCount:0, name:"ROOT", children:[]};
        policiesArray.forEach(policy => {
            let currentTreeNode = policyTree;
            let policyNameArray = policy.name.split(".");
            policyNameArray.forEach((policyName, index, policyNameArray) => {
                let node = currentTreeNode["children"].find(element => element.name === policyName)
                if (typeof(node) === "undefined") {
                    node={id:nodeId++, policyCount:0, children:[], name:policyName};
                    currentTreeNode["children"].push(node);
                }
                if ((index+1) === policyNameArray.length) {
                    currentTreeNode["policyCount"]+=1;
                }
                currentTreeNode = node;
            })
        })
        return policyTree;
    }

    handleTreeItemClick(event) {
        event.target
    }

    renderTreeItems(nodes) {
        return (<TreeItem key={nodes.id} nodeId={nodes.id} label={nodes.name + "("+ nodes.policyCount + ")"} onNodeSelect={this.handleTreeItemClick}>
          {
            Array.isArray(nodes.children) ? nodes.children.map((node) => this.renderTreeItems(node)) : null
          }
        </TreeItem>);
    };

    render() {
         return (
            <div display={this.state.show}>
                <TreeView display={this.state.show} defaultExpanded={['root']} defaultCollapseIcon={<KeyboardArrowDownIcon />}
                defaultExpandIcon={<ChevronRightIcon />} defaultEndIcon={<LocalParkingIcon />} multiSelect>
                  {this.renderTreeItems(this.state.policiesTreeData)}
                </TreeView>
            </div>
          );
    }
}