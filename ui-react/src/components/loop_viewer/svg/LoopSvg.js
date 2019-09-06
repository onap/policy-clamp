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
import LoopCache from '../../../api/LoopCache';
import { withRouter } from "react-router";
import LoopService from '../../../api/LoopService';
import LoopComponentConverter from './LoopComponentConverter';

const LoopViewSvgDivStyled = styled.div`
	overflow: hidden;
	background-color: ${props => (props.theme.loopViewerBackgroundColor)};
	border: 1px solid;
	border-color: ${props => (props.theme.loopViewerHeaderColor)};
	margin-left: auto;
	margin-right:auto;
	text-align: center;

`

class LoopViewSvg extends React.Component {

	static emptySvg = "<svg><text x=\"20\" y=\"40\">No LOOP (SVG)</text></svg>";

	state = {
		svgContent: LoopViewSvg.emptySvg,
		loopCache: new LoopCache({}),
		componentModalMapping: new Map([])
	}

	constructor(props) {
		super(props);
		this.handleSvgClick = this.handleSvgClick.bind(this);
		this.getSvg = this.getSvg.bind(this);
		this.state.loopCache = props.loopCache;
		this.state.componentModalMapping = LoopComponentConverter.buildMapOfComponents(props.loopCache);
		this.getSvg(props.loopCache.getLoopName());
	}

	shouldComponentUpdate(nextProps, nextState) {
		return this.state.svgContent !== nextState.svgContent;
	}

	componentWillReceiveProps(newProps) {	
		if (this.state.loopCache !== newProps.loopCache) {
			this.setState({
				loopCache: newProps.loopCache,
				componentModalMapping: LoopComponentConverter.buildMapOfComponents(newProps.loopCache)
			});
			this.getSvg(newProps.loopCache.getLoopName());
		}
	}

	getSvg(loopName) {
		if (typeof loopName !== "undefined") {
			LoopService.getSvg(loopName).then(svgXml => {
				if (svgXml.length !== 0) {
					this.setState({ svgContent: svgXml })
				} else {
					this.setState({ svgContent: LoopViewSvg.emptySvg })
				}
			});
		} else {
			this.setState({ svgContent: LoopViewSvg.emptySvg })
		}
	}

	handleSvgClick(event) {
		console.debug("svg click event received");
		var elementName = event.target.parentNode.parentNode.parentNode.getAttribute('data-element-id');
		console.info("SVG element clicked", elementName);
		this.props.history.push(this.state.componentModalMapping.get(elementName));
	}

	render() {
		return (
			<LoopViewSvgDivStyled dangerouslySetInnerHTML={{ __html: this.state.svgContent }} onClick={this.handleSvgClick}>

			</LoopViewSvgDivStyled>
		);
	}
}

export default withRouter(LoopViewSvg);