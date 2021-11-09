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
import LoopCache from '../../../api/LoopCache';
import styled from 'styled-components';

const LoopLogsHeaderDivStyled = styled.div`
  background-color: ${ props => props.theme.loopLogsHeaderBackgroundColor };
  padding: 10px 10px;
  color: ${ props => props.theme.loopLogsHeaderFontColor };
`
const TableStyled = styled(Table)`

  overflow: auto;
`
const TableRow = ({ logRow }) => (
  <tr>
    <td>{ logRow.logInstant }</td>
    <td>{ logRow.logType }</td>
    <td>{ logRow.logComponent }</td>
    <td>{ logRow.message }</td>
  </tr>

)

export default class LoopLogs extends React.Component {

  state = {
    loopCache: new LoopCache({})
  }

  constructor(props) {
    super(props);
    this.renderLogs = this.renderLogs.bind(this);
    this.state.loopCache = props.loopCache;
  }

  shouldComponentUpdate(nextProps, nextState) {
    return this.state.loopCache !== nextState.loopCache;
  }

  componentWillReceiveProps(newProps) {
    this.setState({
      loopCache: newProps.loopCache
    });
  }

  renderLogs() {
    let logsArray = this.state.loopCache.getLoopLogsArray();
    if (logsArray != null) {
      return (logsArray.map(row => <TableRow key={ row.id } logRow={ row }/>));
    }
  }

  render() {
    return (
      <LoopLogsHeaderDivStyled>
        <label>Loop Logs</label>
        <TableStyled striped hover variant responsive>
          <thead>
          <tr>
            <th><span align="left">Date</span></th>
            <th><span align="left">Type</span></th>
            <th><span align="left">Component</span></th>
            <th><span align="right">Log</span></th>
          </tr>
          </thead>
          <tbody>
          { this.renderLogs() }
          </tbody>
        </TableStyled>
      </LoopLogsHeaderDivStyled>

    );
  }
}
