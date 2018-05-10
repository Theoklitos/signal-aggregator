import {
    REQUEST, RECEIVE, RECEIVE_ERROR, RESET
} from "./actions";

const initialState = {
    isFetching: false,
    errorMessage: "",
    items: []
}

export function objects(state = initialState, action) {
    switch (action.type) {
        case REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
                errorMessage: "",
            })
        case RECEIVE:
            return Object.assign({}, state, {
                isInitialized: true,
                isFetching: false,
                errorMessage: "",
                items: action.json
            })
        case RECEIVE_ERROR:
            return Object.assign({}, state, {
                errorMessage: action.message,
                isFetching: false,
                isInitialized: true,
                items: []
            })
        case RESET:
            return Object.assign({}, state, {
                isInitialized: true,
                isFetching: false,
                errorMessage: "",
                items: []
            })
        default:
            return state;
    }
}