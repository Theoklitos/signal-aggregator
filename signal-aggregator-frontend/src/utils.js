import React from 'react';

/**
 * returns a string of a euro amount, also appends the sign
 */
export function getAsEuros(decimal, whatToReturnIfNull = "") {
    if(!decimal) {
        return whatToReturnIfNull;
    }
    return decimal.toFixed(2) + "€";
}

/**
 * given a number and a word, will add an "s" to the end if the number if > 1
 */
export function pluralizeWord(word, number) {
    if(number === 1) {
        return number + " " + word;
    } else if(number > 1 || number == 0) {
        return number + " " + word + "s";
    } else {
        return "";
    }
}

export function getAsEurosWithColor(decimal, whatToReturnIfNull = "") {
    var euros = getAsEuros(decimal, whatToReturnIfNull);
    var color = '';
    if(decimal > 0) {
        color = 'green'
    } else if(decimal < 0) {
        color = 'red'
    }
    return <span style={{color: color}}>{euros}</span>
}