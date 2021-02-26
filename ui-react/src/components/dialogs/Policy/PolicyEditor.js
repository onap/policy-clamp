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

import React from 'react'
import PolicyToscaService from '../../../api/PolicyToscaService';
import JSONEditor from '@json-editor/json-editor';
import styled from 'styled-components';
import Button from 'react-bootstrap/Button';

const JsonEditorDiv = styled.div`
    margin-top: 20px;
    background-color: ${props => props.theme.toscaTextareaBackgroundColor};
    text-align: justify;
    font-size: ${props => props.theme.toscaTextareaFontSize};
    width: 100%;
    height: 30%;
`

export default class PolicyEditor extends React.Component {

   state = {
        policyData: this.props.policyData,
        jsonEditor: null,
   }

   constructor(props, context) {
        super(props, context);
        this.createJsonEditor = this.createJsonEditor.bind(this);
        this.getToscaModelForPolicy = this.getToscaModelForPolicy.bind(this);
   }

   getToscaModelForPolicy(policyData) {
        PolicyToscaService.getToscaPolicyModel(policyData["type"], policyData["type_version"]).then(respJsonPolicyTosca => {
            this.setState({
                            jsonSchemaPolicyTosca: respJsonPolicyTosca,
                            jsonEditor:this.createJsonEditor(respJsonPolicyTosca, this.state.policyData["properties"]),
                        })
        });
   }

   componentDidMount() {
        this.getToscaModelForPolicy(this.state.policyData);
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

   render() {
       return (
                    <JsonEditorDiv>
                        <h5>Policy Properties Editor</h5>
                        <div id="policy-editor" title="Policy Properties"/>
                        <Button variant="secondary" title="Create a new policy version from the defined parameters"
                            onClick={this.handleCreateNewVersion}>Create New Version</Button>
                    </JsonEditorDiv>
       );
   }
}