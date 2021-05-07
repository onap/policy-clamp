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

		if (action === 'delete') {
			if (window.confirm('You are about to remove Control Loop Model "' + loopName +
					'". Select OK to continue with deletion or Cancel to keep the model.') === false) {
				return;
			}
		}

		this.props.setBusyLoading(); // Alert top level to start block user clicks

		LoopActionService.performAction(loopName, action)
		.then(pars => {
			this.props.showSucAlert("Action " + action + " successfully performed");
			if (action === 'delete') {
				this.props.updateLoopFunction(null);
				this.props.history.push('/');
			} else {
				// refresh status and update loop logs
				this.refreshStatus(loopName);
			}
		})
		.catch(error => {
			this.props.showFailAlert("Action " + action + " failed");
			// refresh status and update loop logs
			this.refreshStatus(loopName);
		})
		.finally(() => this.props.clearBusyLoading());
	}

	refreshStatus(loopName) {

		this.props.setBusyLoading();

		LoopActionService.refreshStatus(loopName)
		.then(data => {
			this.props.updateLoopFunction(data);
			this.props.history.push('/');
		})
		.catch(error => {
			this.props.history.push('/');
		})
		.finally(() => this.props.clearBusyLoading());
	}

	render() {
		return null;
	}
}
