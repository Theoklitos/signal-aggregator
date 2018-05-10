import React, { Component } from 'react';
import Header from "./header";
import DataTable from './dataTable';
import { getAllSignals } from '../saClient';

class SignalRow extends Component {
    render() {
        var signal = this.props.object
        return(
            <tr className={this.getClassForPl(signal.pl)}>
                <td key={signal.signalProvider}>{signal.signalProvider}</td>
                <td key={signal.instrument}>{signal.instrument}</td>
                <td key={signal.side}>{signal.side}</td>
                <td style={{fontWeight: 'bold'}} key={signal.pl}>{!signal.pl ? "" : signal.pl.toFixed(2)}</td>
                <td key={signal.status}>{signal.status}</td>
                <td key={signal.entryPrice}>{signal.entryPrice}</td>
                <td key={signal.stoploss}>{signal.stoploss}</td>
                <td key={signal.takeprofit}>{signal.takeprofit}</td>
            </tr>
        )};

    getClassForPl(pl) {
        if(pl) {
            return (pl > 0) ? 'profit' : 'loss';
        }
        return "";
    }
}

class Signals extends Component {

    render() {
        return (
            <div>
                <Header/>
                <DataTable columnNames={['Signal Provider','Instrument','Side','P/L (€)','Status','Entry Price','Stop Loss','Take Profit']}
                           rowComponent={SignalRow}
                           fetcherFunction={getAllSignals}
                           freeTextSearch={this.isSignalMatchedByFreeText}
                           checkboxTypeFilters={['LIVE', 'CLOSED', 'STALE']}
                           customCheckboxFilter={{value: 'ZOMBIE', filter: this.isSignalAZombie}}>
                </DataTable>
            </div>
        )
    }

    isSignalMatchedByFreeText(signal, text) {
        var doesTextMatch = (signal.instrument.toLowerCase() + signal.signalProvider.toLowerCase()).indexOf(text) !== -1;
        var doesDateMatch = (signal.startDate.toString().indexOf(text) !== -1);
        return doesTextMatch || doesDateMatch;
    }

    /**
     * we also want to include signals that are closed but their trade is still live (zombies)
     */
    isSignalAZombie(signal) {
        return signal.tradeStatus !== undefined && signal.status === 'CLOSED' && (signal.tradeStatus === 'OPENED' || signal.tradeStatus === 'PENDING');
    }
}

export default Signals;