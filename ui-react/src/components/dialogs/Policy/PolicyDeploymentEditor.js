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

import React, { forwardRef } from 'react';
import Modal from 'react-bootstrap/Modal';
import styled from 'styled-components';
import Button from 'react-bootstrap/Button';

const ModalStyled = styled(Modal)`
    @media (min-width: 500px) {
        .modal-xl {
            max-width: 50%;
        }
    }
    background-color: transparent;
`

export default class PolicyDeploymentEditor extends React.Component {

    state = {
            policyData: this.props.policyData,
    };

    constructor(props, context) {
        super(props, context);
        this.handleClose = this.handleClose.bind(this);
    }

    handleClose() {
        this.setState({ show: false });

    }

    render() {
     return (
             <ModalStyled size="xl" show={this.state.show} onHide={this.handleClose} backdrop="static" keyboard={false}>
                 <Modal.Header closeButton>
                 </Modal.Header>

                 <Modal.Footer>
                     <Button variant="secondary" onClick={this.handleClose}>Close</Button>
                </Modal.Footer>
             </ModalStyled>
       );
    }
 }