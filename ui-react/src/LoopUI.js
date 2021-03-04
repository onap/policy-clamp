/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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
import MenuBar from './components/menu/MenuBar';
import Navbar from 'react-bootstrap/Navbar';
import logo from './logo.png';
import { GlobalClampStyle } from './theme/globalStyle.js';
import OnapConstants from './utils/OnapConstants';

import SvgGenerator from './components/loop_viewer/svg/SvgGenerator';
import LoopLogs from './components/loop_viewer/logs/LoopLogs';
import LoopStatus from './components/loop_viewer/status/LoopStatus';
import UserService from './api/UserService';
import LoopCache from './api/LoopCache';
import LoopActionService from './api/LoopActionService';

import { Route } from 'react-router-dom'
import CreateLoopModal from './components/dialogs/Loop/CreateLoopModal';
import OpenLoopModal from './components/dialogs/Loop/OpenLoopModal';
import ModifyLoopModal from './components/dialogs/Loop/ModifyLoopModal';
import PolicyModal from './components/dialogs/Policy/PolicyModal';
import ViewAllPolicies from './components/dialogs/Policy/ViewAllPolicies';
import LoopPropertiesModal from './components/dialogs/Loop/LoopPropertiesModal';
import UserInfoModal from './components/dialogs/UserInfoModal';
import LoopService from './api/LoopService';
import ViewLoopTemplatesModal from './components/dialogs/Tosca/ViewLoopTemplatesModal';
import ManageDictionaries from './components/dialogs/ManageDictionaries/ManageDictionaries';
import PerformAction from './components/dialogs/PerformActions';
import RefreshStatus from './components/dialogs/RefreshStatus';
import DeployLoopModal from './components/dialogs/Loop/DeployLoopModal';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';

import { Link } from 'react-router-dom';

const StyledMainDiv = styled.div`
    background-color: ${props => props.theme.backgroundColor};
`

const StyledSpinnerDiv = styled.div`
    justify-content: center !important;
    display: flex !important;
`;

const ProjectNameStyled = styled.a`
    vertical-align: middle;
    padding-left: 30px;
    font-size: 36px;
    font-weight: bold;
`

const StyledRouterLink = styled(Link)`
    color: ${props => props.theme.menuFontColor};
    background-color: ${props => props.theme.backgroundColor};
`

const StyledLoginInfo = styled.a`
    color: ${props => props.theme.menuFontColor};
    background-color: ${props => props.theme.backgroundColor};
`

const LoopViewDivStyled = styled.div`
    height: 100%;
    overflow: hidden;
    margin-left: 10px;
    margin-right: 10px;
    margin-bottom: 10px;
    color: ${props => props.theme.loopViewerFontColor};
    background-color: ${props => props.theme.loopViewerBackgroundColor};
    border: 1px solid transparent;
    border-color: ${props => props.theme.loopViewerHeaderBackgroundColor};
`

const LoopViewHeaderDivStyled = styled.div`
    background-color: ${props => props.theme.loopViewerHeaderBackgroundColor};
    padding: 10px 10px;
    color: ${props => props.theme.loopViewerHeaderFontColor};
`

const LoopViewBodyDivStyled = styled.div`
    background-color: ${props => (props.theme.loopViewerBackgroundColor)};
    padding: 10px 10px;
    color: ${props => (props.theme.loopViewerHeaderFontColor)};
    height: 95%;
`

export default class LoopUI extends React.Component {

    state = {
        userName: null,
        loopName: OnapConstants.defaultLoopName,
        loopCache: new LoopCache({}),
        showSucAlert: false,
        showFailAlert: false,
        busyLoadingCount: 0
    };

    constructor() {
        super();
        this.getUser = this.getUser.bind(this);
        this.updateLoopCache = this.updateLoopCache.bind(this);
        this.loadLoop = this.loadLoop.bind(this);
        this.closeLoop = this.closeLoop.bind(this);
        this.showSucAlert =  this.showSucAlert.bind(this);
        this.showFailAlert =  this.showFailAlert.bind(this);
        this.disableAlert =  this.disableAlert.bind(this);
        this.setBusyLoading = this.setBusyLoading.bind(this);
        this.clearBusyLoading = this.clearBusyLoading.bind(this);
        this.isBusyLoading = this.isBusyLoading.bind(this);
        this.renderGlobalStyle = this.renderGlobalStyle.bind(this);
        this.renderSvg = this.renderSvg.bind(this);
    }

    componentWillMount() {
        this.getUser();
    }

    getUser() {
        UserService.login().then(user => {
            this.setState({ userName: user })
        });
    }

    renderMenuNavBar() {
        return (
            <MenuBar loopName={this.state.loopName}/>
        );
    }

    renderUserLoggedNavBar() {
        return (
            <Navbar.Text>
            <StyledLoginInfo>Signed in as: </StyledLoginInfo>
                <StyledRouterLink to="/userInfo">{this.state.userName}</StyledRouterLink>
            </Navbar.Text>
        );
    }

    renderLogoNavBar() {
        return (
            <Navbar.Brand>
                <img height="50px" width="234px" src={logo} alt="" />
                <ProjectNameStyled>CLAMP</ProjectNameStyled>
            </Navbar.Brand>
        );
    }

    renderAlertBar() {
        return (
            <div>
                <Alert variant="success" show={this.state.showSucAlert} onClose={this.disableAlert} dismissible>
                    {this.state.showMessage}
                </Alert>
                <Alert variant="danger" show={this.state.showFailAlert} onClose={this.disableAlert} dismissible>
                    {this.state.showMessage}
                </Alert>
            </div>
        );
    }

    renderNavBar() {
        return (
            <Navbar >
                {this.renderLogoNavBar()}
                <Navbar.Toggle aria-controls="responsive-navbar-nav" />
                {this.renderMenuNavBar()}
                {this.renderUserLoggedNavBar()}
            </Navbar>
        );
    }

    renderLoopViewHeader() {
        return (
            <LoopViewHeaderDivStyled>
                Loop Viewer - {this.state.loopName} - ({this.state.loopCache.getTemplateName()})
            </LoopViewHeaderDivStyled>
        );
    }

    renderSvg() {
        return (
            <SvgGenerator loopCache={this.state.loopCache} clickable={true} generatedFrom={SvgGenerator.GENERATED_FROM_INSTANCE} isBusyLoading={this.isBusyLoading}/>
        )
    }
    renderLoopViewBody() {
        return (
            <LoopViewBodyDivStyled>
                {this.renderSvg()}
                <LoopStatus loopCache={this.state.loopCache}/>
                <LoopLogs loopCache={this.state.loopCache} />
            </LoopViewBodyDivStyled>
        );
    }

    getLoopCache() {
        return this.state.loopCache;

    }

    renderLoopViewer() {
        return (
            <LoopViewDivStyled>
                {this.renderLoopViewHeader()}
                {this.renderLoopViewBody()}
            </LoopViewDivStyled>
        );
    }

    updateLoopCache(loopJson) {

        // If call with an empty object for loopJson, this is a reset to empty
        // from someplace like PerformActions for the case where we are "deleting"
        // a Control Loop model. Set the loopName to the default.

        if (loopJson === null) {
            this.setState({ loopName: OnapConstants.defaultLoopName });
            this.setState({ loopCache: new LoopCache({}) });
        } else {
            this.setState({ loopCache: new LoopCache(loopJson) });
            this.setState({ loopName: this.state.loopCache.getLoopName() });
        }
        console.info(this.state.loopName+" loop loaded successfully");
        }

    showSucAlert(message) {
        this.setState ({ showSucAlert: true, showMessage:message });
    }

    showFailAlert(message) {
        this.setState ({ showFailAlert: true, showMessage:message });
    }

    disableAlert() {
        this.setState ({ showSucAlert: false, showFailAlert: false });
    }

    loadLoop(loopName) {
        this.setBusyLoading();
        LoopService.getLoop(loopName).then(loop => {
            console.debug("Updating loopCache");
            LoopActionService.refreshStatus(loopName).then(data => {
                this.updateLoopCache(data);
                this.clearBusyLoading();
                this.props.history.push('/');
            })
            .catch(error => {
                this.updateLoopCache(loop);
                this.clearBusyLoading();
                this.props.history.push('/');
            });
        });
    }

    setBusyLoading() {
        this.setState((state,props) => ({ busyLoadingCount: ++state.busyLoadingCount }));
    }

    clearBusyLoading() {
        this.setState((state,props) => ({ busyLoadingCount: --state.busyLoadingCount }));
    }

    isBusyLoading() {
        if (this.state.busyLoadingCount === 0) {
            return false;
        } else {
            return true;
        }
    }

    closeLoop() {
        this.setState({ loopCache: new LoopCache({}), loopName: OnapConstants.defaultLoopName });
        this.props.history.push('/');
    }

    renderRoutes() {
        return(
            <React.Fragment>
                <Route path="/viewLoopTemplatesModal" render={(routeProps) => (<ViewLoopTemplatesModal {...routeProps} />)} />
                <Route path="/manageDictionaries" render={(routeProps) => (<ManageDictionaries {...routeProps} />)} />
                <Route path="/viewAllPolicies" render={(routeProps) => (<ViewAllPolicies {...routeProps} />)} />
                <Route path="/policyModal/:policyInstanceType/:policyName" render={(routeProps) => (<PolicyModal {...routeProps}
                    loopCache={this.getLoopCache()}
                    loadLoopFunction={this.loadLoop}/>)}
                />
                <Route path="/createLoop" render={(routeProps) => (<CreateLoopModal {...routeProps}
                    loadLoopFunction={this.loadLoop} />)}
                />
                <Route path="/openLoop" render={(routeProps) => (<OpenLoopModal {...routeProps}
                    loadLoopFunction={this.loadLoop} />)}
                />
                <Route path="/loopProperties" render={(routeProps) => (<LoopPropertiesModal {...routeProps}
                    loopCache={this.getLoopCache()}
                    loadLoopFunction={this.loadLoop}/>)}
                />
                <Route path="/modifyLoop" render={(routeProps) => (<ModifyLoopModal {...routeProps}
                    loopCache={this.getLoopCache()}
                    loadLoopFunction={this.loadLoop}/>)}
                />

                <Route path="/userInfo" render={(routeProps) => (<UserInfoModal {...routeProps} />)} />
                <Route path="/closeLoop" render={this.closeLoop} />

                <Route path="/submit" render={(routeProps) => (<PerformAction {...routeProps}
                    loopAction="submit"
                    loopCache={this.getLoopCache()}
                    updateLoopFunction={this.updateLoopCache}
                    showSucAlert={this.showSucAlert}
                    showFailAlert={this.showFailAlert}
                    setBusyLoading={this.setBusyLoading}
                    clearBusyLoading={this.clearBusyLoading}/>)}
                />
                <Route path="/stop" render={(routeProps) => (<PerformAction {...routeProps}
                    loopAction="stop"
                    loopCache={this.getLoopCache()}
                    updateLoopFunction={this.updateLoopCache}
                    showSucAlert={this.showSucAlert}
                    showFailAlert={this.showFailAlert}
                    setBusyLoading={this.setBusyLoading}
                    clearBusyLoading={this.clearBusyLoading}/>)}
                />
                <Route path="/restart" render={(routeProps) => (<PerformAction {...routeProps}
                    loopAction="restart"
                    loopCache={this.getLoopCache()}
                    updateLoopFunction={this.updateLoopCache}
                    showSucAlert={this.showSucAlert}
                    showFailAlert={this.showFailAlert}
                    setBusyLoading={this.setBusyLoading}
                    clearBusyLoading={this.clearBusyLoading}/>)}
                />
                <Route path="/delete" render={(routeProps) => (<PerformAction {...routeProps}
                    loopAction="delete"
                    loopCache={this.getLoopCache()}
                    updateLoopFunction={this.updateLoopCache}
                    showSucAlert={this.showSucAlert}
                    showFailAlert={this.showFailAlert}
                    setBusyLoading={this.setBusyLoading}
                    clearBusyLoading={this.clearBusyLoading}/>)}
                />
                <Route path="/undeploy" render={(routeProps) => (<PerformAction {...routeProps}
                    loopAction="undeploy"
                    loopCache={this.getLoopCache()}
                    updateLoopFunction={this.updateLoopCache}
                    showSucAlert={this.showSucAlert}
                    showFailAlert={this.showFailAlert}
                    setBusyLoading={this.setBusyLoading}
                    clearBusyLoading={this.clearBusyLoading}/>)}
                />
                <Route path="/deploy" render={(routeProps) => (<DeployLoopModal {...routeProps}
                    loopCache={this.getLoopCache()}
                    updateLoopFunction={this.updateLoopCache}
                    showSucAlert={this.showSucAlert}
                    showFailAlert={this.showFailAlert}/>)}
                />
                <Route path="/refreshStatus" render={(routeProps) => (<RefreshStatus {...routeProps}
                    loopCache={this.getLoopCache()}
                    updateLoopFunction={this.updateLoopCache}
                    showSucAlert={this.showSucAlert}
                    showFailAlert={this.showFailAlert}/>)}
                />
            </React.Fragment>
        );
    }

        renderGlobalStyle() {
                return (
                        <GlobalClampStyle />
                );
        };


    renderSpinner() {
        if (this.isBusyLoading()) {
            return (
                <StyledSpinnerDiv>
                    <Spinner animation="border" role="status">
                        <span className="sr-only">Loading...</span>
                    </Spinner>
                </StyledSpinnerDiv>
            );
        } else {
            return (<div></div>);
        }
    }

    render() {
        return (
                <StyledMainDiv id="main_div">
                    {this.renderGlobalStyle()}
                    {this.renderRoutes()}
                    {this.renderSpinner()}
                    {this.renderAlertBar()}
                    {this.renderNavBar()}
                    {this.renderLoopViewer()}
                </StyledMainDiv>
        );
    }
}
