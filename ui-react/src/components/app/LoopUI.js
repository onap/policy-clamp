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
import MenuBar from '../menu/MenuBar';
import Navbar from 'react-bootstrap/Navbar';
import logo from './logo.png';
import { GlobalClampStyle } from '../../theme/globalStyle.js';

import ClosedLoopSvg from '../loop_viewer/svg/ClosedLoopSvg';
import ClosedLoopLogs from '../loop_viewer/logs/ClosedLoopLogs';
import ClosedLoopStatus from '../loop_viewer/status/ClosedLoopStatus';
import UserService from '../backend_communication/UserService';

const ProjectNameStyled = styled.a`
	vertical-align: middle;
	padding-left: 30px;
	font-size: 30px;

`
const LoopViewDivStyled = styled.div`
	height: 90vh;
	overflow: hidden;
	margin-left: 10px;
	margin-right: 10px;
	margin-bottom: 10px;
	color: ${props => props.theme.loopViewerFontColor};
	background-color: ${props => props.theme.loopViewerBackgroundColor};
	border: 1px solid transparent;
	border-color: ${props => props.theme.loopViewerHeaderBackgroundColor};
`

const LoopViewHeaderDivStyled = styled.div`
	background-color: ${props => props.theme.loopViewerHeaderBackgroundColor};
	padding: 10px 10px;
	color: ${props => props.theme.loopViewerHeaderFontColor};
`

const LoopViewBodyDivStyled = styled.div`
	background-color: ${props => (props.theme.loopViewerBackgroundColor)};
	padding: 10px 10px;
	color: ${props => (props.theme.loopViewerHeaderFontColor)};
	height: 95%;
`

const LoopViewLoopNameSpanStyled = styled.span`
	font-weight: bold;
	color: ${props => (props.theme.loopViewerHeaderFontColor)};
	background-color: ${props => (props.theme.loopViewerHeaderBackgroundColor)};
`

export default class LoopUI extends React.Component {
	state = {
		userName: null,
		loopName: "Empty (NO loop loaded yet)",
	};

	constructor() {
		super();
		this.getUser = this.getUser.bind(this);
	}

 	componentDidMount() {
		 this.getUser();
	 }

	getUser() {
		UserService.LOGIN().then(user => {
			this.setState({userName:user})
		});
	}
		
	renderMenuNavBar() {
		return (
			<MenuBar />
		);
	}

	renderUserLoggedNavBar() {
		return (
			<Navbar.Text>
				Signed in as: <a href="/login">{this.state.userName}</a>
			</Navbar.Text>
		);
	}

	renderLogoNavBar() {
		return (
			<Navbar.Brand>
				<img height="50px" width="234px" src={logo} alt=""/>
				<ProjectNameStyled>CLAMP</ProjectNameStyled>
			</Navbar.Brand>
		);
	}

	renderNavBar() {
		return (
		<Navbar expand="lg">
			{this.renderLogoNavBar()}
			{this.renderMenuNavBar()}
			{this.renderUserLoggedNavBar()}
		</Navbar>
	);
	}
	
	renderLoopViewHeader() {
		return (
			<LoopViewHeaderDivStyled>
				Loop Viewer - <LoopViewLoopNameSpanStyled id="loop_name">{this.state.loopName}</LoopViewLoopNameSpanStyled> 
			</LoopViewHeaderDivStyled>
		);
	}
	
	renderLoopViewBody() {
		return (
			<LoopViewBodyDivStyled>
				<ClosedLoopSvg />
				<ClosedLoopLogs />
				<ClosedLoopStatus />
			</LoopViewBodyDivStyled>
		);
	}
	
	renderLoopViewer() {
		return (
			<LoopViewDivStyled>
					{this.renderLoopViewHeader()}
					{this.renderLoopViewBody()}
			</LoopViewDivStyled>
	 		);
	}
	
	render() {
		return (
			<div id="main_div">
				 	<GlobalClampStyle />
					{this.renderNavBar()}
					{this.renderLoopViewer()}
				</div>
		);
	}
}
