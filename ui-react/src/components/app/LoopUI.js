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

const ProjectNameStyle = styled.a`
	vertical-align: middle;
	padding-left: 30px;
	font-size: 30px;

`
const LoopViewDivStyle = styled.div`
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

const LoopViewHeaderDivStyle = styled.div`
	background-color: ${props => props.theme.loopViewerHeaderBackgroundColor};
	padding: 10px 10px;
	color: ${props => props.theme.loopViewerHeaderFontColor};
`

const LoopViewBodyDivStyle = styled.div`
	background-color: ${props => (props.theme.loopViewerBackgroundColor)};
	padding: 10px 10px;
	color: ${props => (props.theme.loopViewerHeaderFontColor)};
	height: 95%;
`

const LoopViewLoopNameSpanStyle = styled.span`
	font-weight: bold;
	color: ${props => (props.theme.loopViewerHeaderFontColor)};
	background-color: ${props => (props.theme.loopViewerHeaderBackgroundColor)};
`

export default class LoopUI extends React.Component {

	loopName="Empty (NO loop loaded yet)";
		
	renderMenuNavBar() {
		return (
			<MenuBar />
		);
	}
	
	renderUserLoggedNavBar() {
		return (
			<Navbar.Text>
				Signed in as: <a href="login">{localStorage.getItem('user')}</a>
			</Navbar.Text>
		);
	}
	
	renderLogoNavBar() {
		return (
			<Navbar.Brand>
				<img height="50px" width="234px" src={logo} alt=""/>
				<ProjectNameStyle>CLAMP</ProjectNameStyle>
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
			<LoopViewHeaderDivStyle>
				Loop Viewer - <LoopViewLoopNameSpanStyle id="loop_name">{this.loopName}</LoopViewLoopNameSpanStyle> 
			</LoopViewHeaderDivStyle>
		);
	}
	
	renderLoopViewBody() {
		return (
			<LoopViewBodyDivStyle>
				<ClosedLoopSvg />
				<ClosedLoopLogs />
				<ClosedLoopStatus />
			</LoopViewBodyDivStyle>
		);
	}
	
	renderLoopViewer() {
		return (
			<LoopViewDivStyle>
					{this.renderLoopViewHeader()}
					{this.renderLoopViewBody()}
			</LoopViewDivStyle>
	 		);
	}
	
	render() {
		return (
				<div>
				 	<GlobalClampStyle />
					{this.renderNavBar()}
					{this.renderLoopViewer()}
				</div>
		);
	}
}
