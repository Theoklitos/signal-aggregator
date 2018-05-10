export const REQUEST = 'REQUEST'
export const RECEIVE = 'RECEIVE'
export const RECEIVE_ERROR = 'RECEIVE_ERROR'
export const RESET = 'RESET'

// ==========================================================================================================================================
// Asynchronous (and generic) object fetching from the backend
// ==========================================================================================================================================

function request() {
    return {
        type: REQUEST
    }
}

function receive(json) {
    return {
        type: RECEIVE,
        json: json
    }
}

function receiveError(message) {
    return {
        type: RECEIVE_ERROR,
        message: message
    }
}

export function resetObjects() {
    return {
        type: RESET
    }
}

/**
 * asynchronously fetches stuff from a backend
 */
export function fetchObjects(fetcherFunction) {
    return function (dispatch) {
        dispatch(request())
        fetcherFunction().then(response => {
            if(response.status === 401) {
                dispatch(receiveError("Unauthorized!"))
                return;
            } else if(response.status === 500) {
                dispatch(receiveError("Something is wrong with the server!"))
                return;
            } else
            response.json().then(json => {
                dispatch(receive(json))
            });
        }).catch(error => {
            dispatch(receiveError("Could not fetch data!"))
        })
    }
}