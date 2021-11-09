/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import styled from 'styled-components';
import Alert from 'react-bootstrap/Alert';
import { Input, InputLabel, Button, SvgIcon } from "@material-ui/core";
import PublishIcon from '@material-ui/icons/Publish';
import PolicyService from '../../../api/PolicyService';

const ModalStyled = styled(Modal)`
  background-color: transparent;
`

const StyledMessagesDiv = styled.div`
  overflow: auto;
  max-height: 300px;
`


export default class PolicyToscaFileSelector extends React.Component {

  state = {
    show: this.props.show,
    alertMessages: [],
  }

  constructor(props, context) {
    super(props, context);
    this.handleClose = this.handleClose.bind(this);
    this.onFileChange = this.onFileChange.bind(this);
  }

  componentDidUpdate(prevProps) {
    if (this.props.show !== this.state.show) {
      this.setState({ show: this.props.show });
    }
  }

  handleClose() {
    this.props.disableFunction();
    this.setState({ alertMessages: [] });
  }

  onFileChange(target) {
    this.setState({ alertMessages: [] });
    target.currentTarget.files.forEach(file => {
      const fileReader = new FileReader();
      fileReader.readAsDataURL(file);
      fileReader.onload = (content) => {
        PolicyService.sendNewPolicyModel(atob(content.target.result.split(",")[1])).then(respModelCreate => {
          if (typeof (respModelCreate) === "undefined") {
            //it indicates a failure
            this.setState(state => {
              return {
                alertMessages: [...state.alertMessages, (<Alert variant="danger"><Alert.Heading>{ file.name }</Alert.Heading><p>Policy Tosca Model Creation Failure</p>
                  <hr/>
                  <p>Type: { file.type }</p><p>Size: { file.size }</p></Alert>)]
              };
            });
          } else {
            this.props.toscaTableUpdateFunction();
            this.setState(state => {
              return {
                alertMessages: [...state.alertMessages, (<Alert variant="success"><Alert.Heading>{ file.name }</Alert.Heading><p>Policy Tosca Model Created Successfully</p>
                  <hr/>
                  <p>Type: { file.type }</p><p>Size: { file.size }</p></Alert>)]
              };
            });
          }
        });
      };
    });

  }

  render() {
    return (
      <ModalStyled size="lg" show={ this.state.show } onHide={ this.handleClose } backdrop="static" keyboard={ false }>
        <Modal.Header closeButton>
          <Modal.Title>Create New Policy Tosca Model</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group as={ Row } controlId="formPlaintextEmail">
            <Col sm="10">
              <input type="file" multiple accept=".yaml,.yml" id="fileUploadButton" style={ { display: 'none' } } onChange={ this.onFileChange }/>
              <label htmlFor={ 'fileUploadButton' }>
                <Button color="primary" variant="contained" component="span"
                        startIcon={
                          <SvgIcon fontSize="small">
                            <PublishIcon/>
                          </SvgIcon>
                        }>
                  Upload Files
                </Button>
                <p>(Only YAML files are supported)</p>
              </label>
              <StyledMessagesDiv>
                { this.state.alertMessages }
              </StyledMessagesDiv>
            </Col>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={ this.handleClose }>Close</Button>
        </Modal.Footer>
      </ModalStyled>
    );
  }
}
