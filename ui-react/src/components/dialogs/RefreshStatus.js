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
import Spinner from 'react-bootstrap/Spinner';
import styled from 'styled-components';

const StyledSpinnerDiv = styled.div`
	justify-content: center !important;
	display: flex !important;
`;

export default class RefreshStatus extends React.Component {
	state = {
		loopName: this.props.loopCache.getLoopName()
	};

	componentWillReceiveProps(newProps) {
		this.setState({
			loopName: newProps.loopCache.getLoopName()
		});
	}

	componentDidMount() {
		// refresh status and update loop logs
		LoopActionService.refreshStatus(this.state.loopName).then(data => {
			this.props.showSucAlert("Status successfully refreshed");
			this.props.updateLoopFunction(data);
			this.props.history.push('/');
		})
		.catch(error => {
			this.props.showFailAlert("Status refreshing failed");
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
