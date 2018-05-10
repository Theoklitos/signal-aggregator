import fetch from 'isomorphic-fetch'

var rootUrl = 'http://localhost:8081/api'
var rootUrl2 = 'http://192.168.0.105:8081/api'

export function getAllSignals() {
    return fetch(rootUrl + '/signals', {
        method: 'GET',
        headers: {
            "Accept": "application/json"
        }
    })
}

export function getAllAggregations() {
    return fetch(rootUrl + '/aggregations', {
        method: 'GET',
        headers: {
            "Accept": "application/json"
        }
    })
}

export function getAllProviders() {
    return fetch(rootUrl + '/providers', {
        method: 'GET',
        headers: {
            "Accept": "application/json"
        }
    })
}

export function getOverview() {
    return fetch(rootUrl + '/overview', {
        method: 'GET',
        headers: {
            "Accept": "application/json",
        }
    })
}