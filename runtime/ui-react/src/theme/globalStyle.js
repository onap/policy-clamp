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
	}

	span {
		font-family: ${props => props.theme.fontFamily};
		font-size: ${props => props.theme.fontSize};
		font-weight: bold;
	}

	a {
		font-family: ${props => props.theme.fontFamily};
		font-size: ${props => props.theme.fontSize};
		font-weight: bold;
	}

	div {
		font-family: ${props => props.theme.fontFamily};
		font-size: ${props => props.theme.fontSize};
		border-radius: 4px;
		margin-top: 1px;
	}

	label {
		font-family: ${props => props.theme.fontFamily};
		font-size: ${props => props.theme.fontSize};
		font-weight: bold;
	}

	button {
		font-family: ${props => props.theme.fontFamily};
		font-size: ${props => props.theme.fontSize};
		font-weight: bold;
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
	fontSize: '16px',

	loopViewerBackgroundColor: 'white',
	loopViewerFontColor: 'yellow',
	loopViewerHeaderBackgroundColor: '#337ab7',
	loopViewerHeaderFontColor: 'white',

    loopLogsHeaderBackgroundColor:  'white',
    loopLogsHeaderFontColor: 'black',

	menuBackgroundColor: 'white',
	menuFontColor: 'black',
	menuHighlightedBackgroundColor: '#337ab7',
	menuHighlightedFontColor: 'white',

	toscaTextareaBackgroundColor: 'white',
	toscaTextareaFontSize: '13px',

	policyEditorBackgroundColor: 'white',
	policyEditorFontSize: '13px'
};
