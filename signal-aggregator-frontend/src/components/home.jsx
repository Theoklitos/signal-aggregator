import React from 'react';
import Header from "./header";
import { Component } from 'react';
import DataElements from './dataElements';
import { getOverview} from "../saClient";
import { getAsEuros, getAsEurosWithColor, pluralizeWord } from "../utils";
import moment from 'moment';

class GenericInformation extends Component {

    render() {
        var overview = this.props.object;
        return(
            <div className="row">
                <div className="col">
                    <p>Signal Aggregator has been up and running for {this.getDurationFromNow(overview.appStartDate)}</p>
                    <br/>
                    <p>Current total P/L: <strong>{getAsEurosWithColor(overview.totalPl, "0€")}</strong></p>
                    <p>Currently tracking {pluralizeWord("trade",overview.numberOfTrackedTrades)}.</p>
                    <br/>
                    <p>Average trade profit: <span style={{color: 'green'}}>{getAsEuros(overview.averageProfit, 'Nothing yet.')}</span></p>
                    <p>Average trade loss: <span style={{color: 'red'}}>{getAsEuros(overview.averageLoss, 'Nothing yet.')}</span></p>
                    <br/>
                    {overview.activeProviders.length === 0 &&
                        <p>No providers are crrently being scraped.</p>
                    }
                    {overview.activeProviders.length > 0 &&
                        this.renderAdapterList(overview)
                    }
                </div>
            </div>
        )
    }

    getDurationFromNow(date) {
        var durationFromNow = moment.duration(moment(date).diff(moment(new Date())));
        return durationFromNow.humanize() + ".";
    }

    renderPlAsEurosWithColor(pl) {
        var euros = getAsEuros(pl);
        var color = '';
        if(pl > 0) {
            color = 'green'
        } else if(pl < 0) {
            color = 'red'
        }
        return <span style={{color: color}}>{euros}</span>
    }

    renderAdapterList(overview) {
        return (
            <div>
                <p>Scraping {pluralizeWord("adapter", overview.activeProviders.length)}:</p>
                <ul>
                    {overview.activeProviders.map((name, id) => {
                        return <li key={name}>{name}</li>
                    })}
                </ul>
            </div>
        )
    }
}

class Home extends Component {
    render() {
        return(
            <div>
                <Header/>
                <DataElements
                    elementComponent={GenericInformation}
                    fetcherFunction={getOverview}>
                </DataElements>
            </div>
        )
    }
}

export default Home;