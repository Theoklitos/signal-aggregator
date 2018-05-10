import React from 'react'
import ReactDOM from 'react-dom'
import { createStore, combineReducers, applyMiddleware } from 'redux'
import thunkMiddleware from 'redux-thunk'
import { createLogger } from 'redux-logger'
import { Provider } from 'react-redux'
import { Router, Route, browserHistory } from 'react-router'
import { syncHistoryWithStore, routerReducer } from 'react-router-redux'

import Home from './components/home'
import Signals from './components/signals'
import Providers from './components/providers'
import Aggregations from './components/aggregations'
import { objects } from './reducers'

import './sa.css';
import 'bootstrap/dist/css/bootstrap.css';

const store = createStore(
    combineReducers({
        objects,
        routing: routerReducer,
    }), {
        objects: { items: [], isFetching: false, isInitialized: false }
    },
    applyMiddleware(
        thunkMiddleware,
        createLogger()
    )
)

// Create an enhanced history that syncs navigation events with the store
const history = syncHistoryWithStore(browserHistory, store)

// Now you can dispatch navigation actions2 from anywhere!
// store.dispatch(push('/foo'))

ReactDOM.render(
    <Provider store={store}>
        { /* Tell the Router to use our enhanced history */ }
            <Router history={history}>
                <Route path="/" component={Home} />
                <Route exact path="/signals" component={Signals} />
                <Route exact path="/aggregations" component={Aggregations} />
                <Route exact path="/providers" component={Providers} />
            </Router>
    </Provider>,
    document.getElementById('root')
)

