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

export default class ToscaViewer extends React.Component {

   state = {
        toscaData: this.props.toscaData,
        yamlPolicyTosca: this.getToscaModelYamlFor(this.props.toscaData),
   }

   constructor(props, context) {
        super(props, context);
        this.getToscaModelYamlFor = this.getToscaModelYamlFor.bind(this);
   }

   getToscaModelYamlFor(toscaData) {
        PolicyToscaService.getToscaPolicyModelYaml(toscaData["policyModelType"], toscaData["version"]).then(respYamlPolicyTosca => {
            this.setState({
                            yamlPolicyTosca: respYamlPolicyTosca,
                        })
        });
   }

   render() {
       return (
                    <JsonEditorDiv>
                        <pre>{this.state.yamlPolicyTosca}</pre>
                        <Button variant="secondary" title="Create a new policy version from the defined parameters"
                            onClick={this.handleCreateNewVersion}>Create New Version</Button>
                    </JsonEditorDiv>
       );
   }
}