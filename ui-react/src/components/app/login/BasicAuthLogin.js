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
import React from 'react';
import styled from 'styled-components';
import LoopService from '../../backend_communication/LoopService';

const LoginHeaderStyle = styled.span`
  font-size: 20px;
  font-weight: bold;
  padding-left: 10px;
	color: ${props => props.theme.loopViewerHeaderFontColor};
`
const LoginDivStyle = styled.div`
  font-size: 12px;
	background-color: ${props => props.theme.loopViewerHeaderBackgroundColor};
	padding: 10px 10px;
	color: ${props => props.theme.loopViewerHeaderFontColor};
`
const LoginSubmitButtonStyle = styled.button`
  font-size: 12px;
	padding: 5px 10px;
	color: ${props => props.theme.loopViewerHeaderFontColor};
  border: 2px solid;
  border-radius: 8px;
`
const LoginTextInputStyle = styled.input`
  padding: 10px 10px;
  margin-left: 20px;
  border: 1px solid #ced4da;
  border-radius: 3px;
	color: ${props => props.theme.loopViewerHeaderFontColor};
`

export default class BasicAuthLogin extends React.Component {
    constructor(props) {
        super(props);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleChange = this.handleChange.bind(this);
        console.log('BasicAuthLogin');
        this.state = {
            username: '',
            password: '',
            submitted: 'false'
        };
    }

    handleChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    }

    handleSubmit(e) {
            e.preventDefault();
            this.setState({ submitted: true });
            const { username, password } = this.state;
            LoopService.login(username, password)
            .then(
              user => {
                  const { from } = { from: { pathname: "/" } };
                  this.props.history.push(from);
              },
              error => {
                  const { from } = { from: { pathname: "/loginFailed" } };
                  this.props.history.push(from);
                  console.log ("Basic login failed");
              }
        );
    }

    render() {
      const { username, password, submitted} = this.state;
        return (
            <div>
                <LoginHeaderStyle>Login</LoginHeaderStyle>
                <form name="form" onSubmit={this.handleSubmit}>
                    <LoginDivStyle className={(submitted && !username ? ' has-error' : '')}>
                        <label htmlFor="username">Username</label>
                        <LoginTextInputStyle name="username" value={username} onChange={this.handleChange} />
                        {submitted && !username &&
                            <div className="help-block">Username is required</div>
                        }
                    </LoginDivStyle>
                    <LoginDivStyle className={(submitted && !password ? ' has-error' : '')}>
                        <label htmlFor="password">Password</label>
                        <LoginTextInputStyle type="password" name="password" value={password} onChange={this.handleChange} />
                        {submitted && !password &&
                            <div className="help-block">Password is required</div>
                        }
                    </LoginDivStyle>
                    <LoginDivStyle>
                        <LoginSubmitButtonStyle className="btn btn-primary">Login</LoginSubmitButtonStyle>
                    </LoginDivStyle>
                </form>
            </div>
        );
    }
}
