/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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
import React from 'react';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import NavDropdown from 'react-bootstrap/NavDropdown';
import OnapConstants from '../../utils/OnapConstants';
import 'bootstrap-css-only/css/bootstrap.min.css';
import styled from 'styled-components';
import { Link } from 'react-router-dom';

const StyledLink = styled(Link)`
    color: ${props => props.theme.menuFontColor};
    background-color: ${props => props.theme.menuBackgroundColor};
    font-weight: normal;
    display: block;
    width: 100%;
    padding: .25rem 1.5rem;
    clear: both;
    text-align: inherit;
    white-space: nowrap;
    border: 0;
    :hover {
        text-decoration: none;
        background-color: ${props => props.theme.menuHighlightedBackgroundColor};
        color:  ${props => props.theme.menuHighlightedFontColor};
    }
`;
const StyledNavLink = styled(Nav.Link)`
    color: ${props => props.theme.menuFontColor};
    background-color: ${props => props.theme.menuBackgroundColor};
    font-weight: normal;
    padding: .25rem 1.5rem;
    :hover {
        background-color: ${props => props.theme.menuHighlightedBackgroundColor};
        color:  ${props => props.theme.menuHighlightedFontColor};
    }
`;

const StyledNavDropdown = styled(NavDropdown)`
    color: ${props => props.theme.menuFontColor};
    & .dropdown-toggle {
    color: ${props => props.theme.menuFontColor};
    background-color: ${props => props.theme.backgroundColor};
    font-weight: normal;
        :hover {
                font-weight: bold;
        }
    }
`;

export default class MenuBar extends React.Component {
    state = {
        loopName: this.props.loopName,
        disabled: true
    };

    componentWillReceiveProps(newProps) {
        if (newProps.loopName !== OnapConstants.defaultLoopName) {
            this.setState({ disabled: false });
        } else {
            this.setState({ disabled: true });
        }
    }

    render () {
        return (
                <Navbar.Collapse>
                    <StyledNavDropdown title="POLICY Framework">
                            <NavDropdown.Item as={StyledLink} to="/viewAllPolicies">View All Policies</NavDropdown.Item>
                    </StyledNavDropdown>
                    <StyledNavDropdown title="CLAMP Options">
                            <NavDropdown.Item as={StyledLink} to="/manageDictionaries">Tosca Metadata Dictionaries</NavDropdown.Item>
                            <NavDropdown.Divider />
                            <NavDropdown.Item as={StyledLink} to="/viewLoopTemplatesModal">View All Loop Templates</NavDropdown.Item>
                    </StyledNavDropdown>
                    <StyledNavDropdown title="LOOP Instance">
                            <NavDropdown.Item as={StyledLink} to="/createLoop">Create</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/openLoop">Open</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/closeLoop" disabled={this.state.disabled}>Close</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/modifyLoop" disabled={this.state.disabled}>Modify</NavDropdown.Item>
                            <NavDropdown.Divider />
                            <NavDropdown.Item as={StyledLink} to="/loopProperties" disabled={this.state.disabled}>Properties</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/refreshStatus" disabled={this.state.disabled}>Refresh Status</NavDropdown.Item>
                    </StyledNavDropdown>
                    <StyledNavDropdown title="LOOP Operations">
                            <NavDropdown.Item as={StyledLink} to="/submit" disabled={this.state.disabled}>Create and deploy to Policy Framework (SUBMIT)</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/stop" disabled={this.state.disabled}>Undeploy from Policy Framework (STOP)</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/restart" disabled={this.state.disabled}>ReDeploy to Policy Framework (RESTART)</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/delete" disabled={this.state.disabled}>Delete loop instance (DELETE)</NavDropdown.Item>
                            <NavDropdown.Divider />
                            <NavDropdown.Item as={StyledLink} to="/deploy" disabled={this.state.disabled}>Deploy to DCAE (DEPLOY)</NavDropdown.Item>
                            <NavDropdown.Item as={StyledLink} to="/undeploy" disabled={this.state.disabled}>UnDeploy to DCAE (UNDEPLOY)</NavDropdown.Item>
                    </StyledNavDropdown>
                    <StyledNavDropdown title="Help">
                            <StyledNavLink href="https://wiki.onap.org/" target="_blank">Wiki</StyledNavLink>
                            <StyledNavLink href="mailto:onap-discuss@lists.onap.org?subject=CLAMP&body=Please send us suggestions or feature enhancements or defect. If possible, please send us the steps to replicate any defect.">Contact Us</StyledNavLink>
                            <NavDropdown.Item as={StyledLink} to="/userInfo">User Info</NavDropdown.Item>
                    </StyledNavDropdown>
                </Navbar.Collapse>
        );
    }
}
