import React, { Component } from 'react';
import Header from "./header";
import { getAllAggregations } from "../saClient"
import DataTable from './dataTable';
import moment from 'moment';

class AggregationRow extends Component {

    render() {
        var aggregation = this.props.object;
        return(
            <tr>
                <td key={aggregation.instrument}>{aggregation.instrument}</td>
                <td key={aggregation.side}>{aggregation.side}</td>
                <td key={aggregation.rank}>{aggregation.rank}</td>
                <td key={aggregation.signals}>{aggregation.signals.length}</td>
                <td key={aggregation.status}>{aggregation.status}</td>
                <td key={aggregation.detectionDate}>{moment(aggregation.detectionDate).format("hh:mm:ss Do MMM YYYY")}</td>
                <td key={aggregation.endDate}>{(aggregation.endDate) ? moment(aggregation.endDate).format("hh:mm:ss Do MMM YYYY") : ""}</td>

            </tr>
        )};
}

class Aggregations extends Component {

    render() {
        return (
            <div>
                <Header/>
                <DataTable columnNames={['Instrument', 'Side', 'Rank', 'Signals', 'Status', 'Detection Date', 'End Date']}
                           rowComponent={AggregationRow}
                           fetcherFunction={getAllAggregations}
                           freeTextSearch={this.isAggregationMatchedByFreeText}
                           checkboxTypeFilters={['LIVE', 'CLOSED']}
                           sortingFunction={this.sortAggregations}>
                </DataTable>
            </div>
        )
    }

    isAggregationMatchedByFreeText(aggregation, text) {
        var doesTextMatch = (aggregation.instrument.toLowerCase().indexOf(text) !== -1);
        var doesDateMatch = (aggregation.detectionDate.toString().indexOf(text) !== -1);
        return doesTextMatch || doesDateMatch;
    }

    sortAggregations(ag1, ag2) {
        var date1 = new Date(ag1.detectionDate);
        var date2 = new Date(ag2.detectionDate);
        return date2 - date1;
    }
}

export default Aggregations;