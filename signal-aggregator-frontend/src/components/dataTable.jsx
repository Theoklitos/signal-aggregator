import React, { Component } from 'react';
import { connect } from 'react-redux';
import { fetchObjects, resetObjects } from "../actions"
import Message from './message'

/**
 * A table that updates its own elements via api calls and can also filter
 */
class DataTable extends Component {

    constructor(props) {
        super(props);
        this.updateTextFilter = this.updateTextFilter.bind(this);
        this.updateTypeFilter = this.updateTypeFilter.bind(this);
        this.state = {
            filterText: '',
            signalTypesToShow: ['LIVE'], // by default, show LIVE elements
        }
    }

    render() {
        return(
            <div>
                {this.props.isInitialized &&
                    <div className="container-fluid">
                        <div className="row">
                            <div className="col-sm-12 text-center mt-2">
                                <Message message={this.props.errorMessage}></Message>
                            </div>
                        </div>
                        <div className="row">

                            {this.props.freeTextSearch &&
                                <div className="col-sm-4" style={{margin: '10px 0'}}>
                                    <div className="input-group">
                                        <input className="form-control" type="text" onChange={this.updateTextFilter}></input>
                                    </div>
                                </div>
                            }

                            {this.props.checkboxTypeFilters &&
                                <div className="col-sm-4" style={{margin: '10px 0'}}>
                                    <div className="input-group">
                                    <span style={{display:'inline-block', verticalAlign:'middle', marginTop: '5px'}}>
                                        {this.props.checkboxTypeFilters.map((name, id) => {
                                            return <label>
                                                <input type="checkbox" value={name} key={name} defaultChecked={name === 'LIVE'} onChange={this.updateTypeFilter}/>
                                                &nbsp;{name}&nbsp;&nbsp;
                                            </label>
                                        })}

                                        {this.props.customCheckboxFilter &&
                                            <label>
                                                <input type="checkbox" value={this.props.customCheckboxFilter.value} onChange={this.updateTypeFilter}/>
                                                &nbsp;{this.props.customCheckboxFilter.value}&nbsp;&nbsp;
                                            </label>
                                        }

                                    </span>
                                    </div>
                                </div>
                            }

                            <main className="col-sm-12">
                                <div className="table-responsive">
                                    <table className="table table-striped">
                                        <thead>
                                        <tr>
                                            {this.props.columnNames.map((name, id) => {
                                                return <th key={name}>{name}</th>;
                                            })}
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {this.sortObjects()}
                                        {this.props.objects.map((object, id) => {
                                            if(this.shouldDisplayObject(object)) {
                                                return React.createElement(this.props.rowComponent, {object: object, key: id});
                                            }
                                        })}
                                        </tbody>
                                    </table>
                                </div>
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

    shouldDisplayObject(object) {
        var isIncludedByFreeTextSearchFilter = true;
        var isIncludedByCustomFilter = false;
        if(this.props.freeTextSearch && object) {
            isIncludedByFreeTextSearchFilter = this.props.freeTextSearch(object, this.state.filterText);
        }

        if(this.props.customCheckboxFilter) {
            if(this.state.signalTypesToShow.indexOf(this.props.customCheckboxFilter.value) !== -1) {
                isIncludedByCustomFilter = this.props.customCheckboxFilter.filter(object)
            }
        }

        return (this.state.signalTypesToShow.includes(object.status) || isIncludedByCustomFilter) && isIncludedByFreeTextSearchFilter;
    }

    updateTextFilter(value) {
        var newFilterValue = value.target.value;
        var existingSignalTypesToShow = this.state.signalTypesToShow;
        this.setState({
            filterText: newFilterValue,
            signalTypesToShow: existingSignalTypesToShow
        })
    }

    updateTypeFilter(value) {
        var newFilterValue = value.target.value;
        var isSelected = value.target.checked;
        var existingFilterText = this.state.filterText;
        var modifiedTypesToShow = this.state.signalTypesToShow;
        if(isSelected) {
            modifiedTypesToShow.push(newFilterValue)
        } else {
            var index = modifiedTypesToShow.indexOf(newFilterValue);
            if(index !== -1) {
                modifiedTypesToShow.splice(index,1);
            }
        }
        this.setState({
            filterText: existingFilterText,
            signalTypesToShow: modifiedTypesToShow
        })
    }

    /**
     * sorts the objects based on the sorting function, if any
     */
    sortObjects() {
        if(this.props.sortingFunction) {
            this.props.objects.sort(this.props.sortingFunction);
        }
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

export default connect(mapStateToProps, mapDispatchToProps)(DataTable)