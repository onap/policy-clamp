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

import { createGlobalStyle } from 'styled-components';

export const GlobalClampStyle = createGlobalStyle`
  body {
    padding: 0;
    margin: 0;
    font-family: ${props => props.theme.fontFamily};
    font-size: ${props => props.theme.fontSize};
    font-weight: normal;
    color: ${props => props.theme.fontNormal};
    background-color: ${props => props.theme.backgroundColor};
  }

 span {
	font-family: ${props => props.theme.fontFamily};
    font-size: ${props => props.theme.fontSize};
    font-weight: bold;
    color: ${props => props.theme.fontNormal};
    background-color: ${props => props.theme.backgroundColor};
  }
  
  a {
	font-family: ${props => props.theme.fontFamily};
    font-size: ${props => props.theme.fontSize};
    font-weight: bold;
    color: ${props => props.theme.fontNormal};
    background-color: ${props => props.theme.backgroundColor};

  }
  
  div {
  	font-family: ${props => props.theme.fontFamily};
    font-size: ${props => props.theme.fontSize};
  	border-radius: 4px;
  	color: ${props => props.theme.fontNormal};
    background-color: ${props => (props.theme.backgroundColor)};
  }
  
  svg {
  	padding: 10px;
	overflow: hidden;
	background-color: ${props => (props.theme.loopViewerBackgroundColor)};
	width: 100%;
	height: 100%;
  }
`

export const DefaultClampTheme = {
	fontDanger: '#eb238e',
	fontWarning: '#eb238e',
	fontLight: '#ffffff',
	fontDark: '#888888',
	fontHighlight: '#ffff00',
	fontNormal: 'black',
	
	backgroundColor: '#eeeeee',
	fontFamily: 'Arial, Sans-serif',
	fontSize: '15px',
	
  	loopViewerBackgroundColor: 'white',
  	loopViewerFontColor: 'yellow',
  	loopViewerHeaderBackgroundColor: '#337ab7',
  	loopViewerHeaderFontColor: 'white',
};
