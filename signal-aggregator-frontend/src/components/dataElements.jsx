import React, { Component } from 'react';
import { connect } from 'react-redux';
import { fetchObjects, resetObjects } from "../actions"
import Message from "./message"

/**
 * A series of elements that are updated from some API
 */
class DataElements extends Component {

    render() {
        return(
            <div>
                {this.props.isInitialized &&
                    <div className="container-fluid">
                        <div className="row">
                            <div className="col-sm-12 text-center mt-2">
                                <Message message={this.props.errorMessage}></Message>
                            </div>
                            <main className="col-12">

                                {!this.props.objects || this.props.objects.length == 0 &&
                                    <h4>No data available.</h4>
                                }

                                {this.props.objects.constructor === Object &&
                                    React.createElement(this.props.elementComponent, {object: this.props.objects})
                                }

                                {this.props.objects.constructor === Array &&
                                    this.props.objects.map((object, id) => {
                                        //return React.createElement("div",{className: "container"}, React.createElement(this.props.elementComponent, {object: object}));
                                        return React.createElement(this.props.elementComponent, {object: object, key: id});
                                    })
                                }
                            </main>
                        </div>
                    </div>
                }

                {!this.props.isInitialized &&
                    <div className="loader">
                        <div>
                            Loading...
                        </div>
                    </div>
                }
            </div>
        )};

    componentDidMount() {
        this.props.update();
        this.interval = setInterval(this.props.update, 10000)
    }

    componentWillUnmount() {
        clearInterval(this.interval);
        this.props.reset();
    }
}

function mapStateToProps(state) {
    return {
        objects: state.objects.items,
        errorMessage: state.objects.errorMessage,
        isInitialized: state.objects.isInitialized,
    }
}

function mapDispatchToProps(dispatch, ownProps) {
    return {
        update: function() {
            dispatch(fetchObjects(ownProps.fetcherFunction)); // the thunk asynchronous action is dispatched
        },
        reset: function() {
            dispatch(resetObjects());
        }
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(DataElements)