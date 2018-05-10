import React, { Component } from 'react';

class Message extends Component {

    render() {
        return(
            <div>
                {this.props.message &&
                    <div className="alert alert-danger" role="alert">
                        {this.props.message.toString()}
                    </div>
                }
            </div>
        )};
}

export default Message;