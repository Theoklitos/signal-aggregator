import React, { Component } from 'react';
import Header from "./header";
import { getAllProviders } from "../saClient"
import DataElements from "./dataElements";
import { Line } from 'react-chartjs-2';
import { getAsEurosWithColor } from '../utils'

class Provider extends Component {

    render() {
        var provider = this.props.object;
        return(
            <div>
                <div className="row">
                    <div className="col-sm">
                        <h3>{provider.name}</h3>
                    </div>
                </div>
                <div className="row">
                    <div className="col-sm-2">
                        <p className="mt-3">Rank: {provider.rank}</p>
                        <p>Total P/L: <strong>{getAsEurosWithColor(provider.totalPl)}</strong></p>
                        <p>Number of trades: {provider.trades.length}</p>
                    </div>
                    <div className="col-sm-10">
                        <Line data={this.getChartData(provider.trades)} options={this.getChartOptions(provider.trades)} width={400} height={100}/>
                    </div>
                </div>
            </div>
        )};

    getChartData(trades) {
        trades.sort(function(t1,t2) {
            var date1 = new Date(t1.endDate);
            var date2 = new Date(t2.endDate);
            return date1 - date2;
        });

        var totalPl = 0;
        var labels = [];
        var data = trades.map(function (trade){
            if(trade.pl) {
                totalPl += trade.pl;
            }
            labels.push(new Date(trade.endDate))
            return {
                pl: trade.pl,
                x: new Date(trade.endDate),
                y: totalPl
            };
        });

        return {
            // Labels should be Date objects
            labels: labels,
            datasets: [{
                fill: false,
                label: 'Total P/L (€)',
                data: data,
                borderColor: '#fe8b36',
                backgroundColor: '#fe8b36',
                lineTension: 0,
            }]
        }
    }

    getChartOptions(trades) {
        return {
                type: 'line',
            tooltips: {
                callbacks: {
                    title: function(tooltipItem, chart) {
                        return "TITLE";
                    },
                    label: function(tooltipItem, chart) {
                        return tooltipItem.xLabel;
                    }
                }
            },
            responsive: true,
            scales: {
                    xAxes: [{
                        type: 'time',
                        displayFormats: {
                            'millisecond': 'MMM DD',
                            'second': 'MMM DD',
                            'minute': 'MMM DD',
                            'hour': 'MMM DD',
                            'day': 'MMM DD',
                            'week': 'MMM DD',
                            'month': 'MMM DD',
                            'quarter': 'MMM DD',
                            'year': 'MMM DD',
                        },
                        time: {
                            unit: 'day',
                            unitStepSize: 1,
                            displayFormats: {'day': 'MMM DD'}
                        },
                        display: true,
                        scaleLabel: {
                            display: true,
                            labelString: "Trade Close Date",
                        }
                    }],
                    yAxes: [{
                        ticks: {beginAtZero: true},
                        display: true,
                        scaleLabel: {
                            display: true,
                            labelString: "Total P/L (€)",
                        }
                    }]
                }
            }
        }

}

class Providers extends Component {

    render() {
        return (
            <div>
                <Header/>
                <DataElements
                    elementComponent={Provider}
                    fetcherFunction={getAllProviders}>
                </DataElements>
            </div>
        )
    }

}

export default Providers;