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
import Table from 'react-bootstrap/Table';
import styled from 'styled-components';
import LoopCache from '../../../api/LoopCache';

const LoopStatusViewDivStyled = styled.div`
  background-color: ${ props => props.theme.loopViewerHeaderBackgroundColor };
  padding: 10px 10px;
  color: ${ props => props.theme.loopViewerHeaderFontColor };
`

const TableStyled = styled(Table)`
  overflow: auto;
`

const TableRow = ({ statusRow }) => (
  <tr>
    <td>{ statusRow.componentName }</td>
    <td>{ statusRow.stateName }</td>
    <td>{ statusRow.description }</td>
  </tr>

)

export default class LoopStatus extends React.Component {
  state = {
    loopCache: new LoopCache({})
  }

  constructor(props) {
    super(props);
    this.renderStatus = this.renderStatus.bind(this);
    this.state.loopCache = props.loopCache;
  }


  renderStatus() {
    let componentStates = this.state.loopCache.getComponentStates();
    if (componentStates != null) {
      return Object.keys(componentStates).map((key) => {
        console.debug("Adding status for: ", key);
        var res = {}
        res[key] = this.state.loopCache.getComponentStates()[key];
        return (<TableRow key={ key } statusRow={ {
          'componentName': key,
          'stateName': this.state.loopCache.getComponentStates()[key].componentState.stateName,
          'description': this.state.loopCache.getComponentStates()[key].componentState.description
        } }/>)
      })

    }
  }

  shouldComponentUpdate(nextProps, nextState) {
    return this.state.loopCache !== nextState.loopCache;
  }

  componentWillReceiveProps(newProps) {
    this.setState({
      loopCache: newProps.loopCache
    });
  }

  render() {
    return (
      <LoopStatusViewDivStyled>
        <label>Loop Status: { this.state.loopCache.getComputedState() }
        </label>

        <div>
          <TableStyled striped hover variant responsive>
            <thead>
            <tr>
              <th><span align="left">Component Name</span></th>
              <th><span align="left">Component State</span></th>
              <th><span align="right">Description</span></th>
            </tr>
            </thead>
            <tbody>
            { this.renderStatus() }
            </tbody>
          </TableStyled>
        </div>
      </LoopStatusViewDivStyled>
    );
  }
}

