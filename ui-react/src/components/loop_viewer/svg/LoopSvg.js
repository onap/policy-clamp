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
import { withRouter } from "react-router";
import LoopCache from '../../../api/LoopCache'
import LoopService from '../../../api/LoopService'

const LoopViewSvgDivStyled = styled.div`
	overflow: hidden;
	background-color: ${props => (props.theme.loopViewerBackgroundColor)};
	border: 1px solid;
	border-color: ${props => (props.theme.loopViewerHeaderColor)};
	height: 50%;
`

class LoopViewSvg extends React.Component {

	static emptySvg = "<svg><text x=\"20\" y=\"40\">No LOOP (SVG)</text></svg>";
	static operationalPolicyDataElementId = "OperationalPolicy";


	state = {
		svgContent: LoopViewSvg.emptySvg,
		loopCache: this.props.loopCache,
	}

	constructor(props) {
		super(props);
		this.state.loopCache = props.loopCache;
		this.handleSvgClick = this.handleSvgClick.bind(this);
		this.getSvg = this.getSvg.bind(this);
	}

	shouldComponentUpdate(nextProps, nextState) {
		return this.state.svgContent !== nextState.svgContent;
	}

	componentWillReceiveProps(newProps) {
		this.state.loopCache = newProps.loopCache;
		this.getSvg();
	}

	getSvg() {
		LoopService.getSvg(this.state.loopCache.getLoopName()).then(svgXml => {
			if (svgXml.length != 0) {
				this.setState({ svgContent: svgXml })
			} else {
				this.setState({ svgContent: LoopViewSvg.emptySvg })
			}
		});
	}

	handleSvgClick(event) {
		console.debug("svg click event received");
		var elementName = event.target.parentNode.parentNode.parentNode.getAttribute('data-element-id');
		console.info("SVG element clicked", elementName);
		if (typeof elementName === "undefined" || elementName === "VES") {
			return;
		} else if (elementName === LoopViewSvg.operationalPolicyDataElementId) {
			this.props.history.push('/operationalPolicyModal');
		} else {
			this.props.history.push('/configurationPolicyModal');
		}
	}

	render() {
		return (
			<LoopViewSvgDivStyled id="loop_svg" dangerouslySetInnerHTML={{ __html: this.state.svgContent }} onClick={this.handleSvgClick}>

			</LoopViewSvgDivStyled>
		);
	}
}

export default withRouter(LoopViewSvg);