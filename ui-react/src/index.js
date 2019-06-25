import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';

class UserBar extends React.Component {
	render () {
		const user = this.props.user;
		return (
			<div className="user_name">Hello:{user}
			</div>
		);
	}
}

class ClampLogo extends React.Component {
	render() {
		return (
			<div className="col-md-4 col-lg-4">
				<img className="image_style" alt="" src={require('./images/logo_onap_2017.png')} 
				height="50px"
				width="234px"/>
				<div className="navbar-brand logo">
					&nbsp;&nbsp;
					<b>CLAMP</b>
				</div>
			</div>
		);
	}
}

class ClampHeader extends React.Component {
	render() {
		return (
			<div className='rowC'>
			<ClampLogo />
			<div className="dummy"></div>
			<UserBar />
			</div>
		);
	}
}

ReactDOM.render(
	<ClampHeader />,
	document.getElementById('root')
)
