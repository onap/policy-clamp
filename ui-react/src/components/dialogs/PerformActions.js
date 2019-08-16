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
import LoopActionService from '../../api/LoopActionService';
import Spinner from 'react-bootstrap/Spinner'
import styled from 'styled-components';

const StyledSpinnerDiv = styled.div`
	justify-content: center !important;
	display: flex !important;
`;

export default class PerformActions extends React.Component {
	state = {
		loopName: this.props.loopCache.getLoopName(),
		loopAction: this.props.loopAction
	};
	constructor(props, context) {
		super(props, context);

		this.refreshStatus = this.refreshStatus.bind(this);
	}
	componentWillReceiveProps(newProps) {
		this.setState({
			loopName: newProps.loopCache.getLoopName(),
			loopAction: newProps.loopAction
		});
	}

	componentDidMount() {
		const action = this.state.loopAction;
		const loopName = this.state.loopName;
		console.log("Perform action:" + action);
		LoopActionService.performAction(loopName, action).then(pars => {
			alert("Action " + action + " successfully performed");
			// refresh status and update loop logs
			this.refreshStatus(loopName);
		})
		.catch(error => {
			alert("Action " + action + " failed");
			// refresh status and update loop logs
			this.refreshStatus(loopName);
		});

	}

	refreshStatus(loopName) {
		LoopActionService.refreshStatus(loopName).then(data => {
			this.props.updateLoopFunction(data);
			this.props.history.push('/');
		})
			.catch(error => {
			this.props.history.push('/');
		});
	}

	render() {
		return (
			<StyledSpinnerDiv>
				<Spinner animation="border" role="status">
				</Spinner>
			</StyledSpinnerDiv>
		);
	}
}
