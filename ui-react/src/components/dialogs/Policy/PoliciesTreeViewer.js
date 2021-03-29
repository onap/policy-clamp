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
import CloseIcon from '@material-ui/icons/Close';

export default class PoliciesTreeViewer extends React.Component {
    constructor(props, context) {
        super(props, context);
//        this.handleClose = this.handleClose.bind(this);
        this.createPoliciesTree = this.createPoliciesTree.bind(this);
    }

    state = {
            display: (this.props.show) ? "block":"none",
            policiesData: this.createPoliciesTree(this.props.policiesData),
    }

    componentDidUpdate(prevProps) {
      if (prevProps.policiesData !== this.props.policiesData) {
        this.setState({policiesData: this.createPoliciesTree(this.props.policiesData)})
      }
    }

    createPoliciesTree(policiesArray) {
        // put my tree in a Json
        let policyTree = {};
        policiesArray.forEach(policy => {
            let currentTreeNode = policyTree;
            let policyNameArray = policy.name.split(".");
            policyNameArray.forEach((policyName, index, policyNameArray) => {
                if (typeof(currentTreeNode[policyName]) === "undefined") {
                    if((index+1) < policyNameArray.length) {
                       currentTreeNode[policyName] = {};
                    } else {
                        currentTreeNode[policyName] = [policy];
                    }
                }
                currentTreeNode = currentTreeNode[policyName];
            })
        })
        return policyTree;
    }

    renderTreeItems() {

    }

    render() {
         return (
            <div display={this.state.show}>
                <TreeView display={this.state.show} defaultExpanded={['1']} defaultCollapseIcon={<ChevronRightIcon />}
                defaultExpandIcon={<KeyboardArrowDownIcon />} defaultEndIcon={<CloseIcon />}>
                  <TreeItem nodeId="1" label="Main">
                    <TreeItem nodeId="2" label="Hello" />
                    <TreeItem nodeId="3" label="Subtree with children">
                      <TreeItem nodeId="6" label="Hello" />
                      <TreeItem nodeId="7" label="Sub-subtree with children">
                        <TreeItem nodeId="9" label="Child 1" />
                        <TreeItem nodeId="10" label="Child 2" />
                        <TreeItem nodeId="11" label="Child 3" />
                      </TreeItem>
                      <TreeItem nodeId="8" label="Hello" />
                    </TreeItem>
                    <TreeItem nodeId="4" label="World" />
                    <TreeItem nodeId="5" label="Something something" />
                  </TreeItem>
                </TreeView>
            </div>
          );
    }
}