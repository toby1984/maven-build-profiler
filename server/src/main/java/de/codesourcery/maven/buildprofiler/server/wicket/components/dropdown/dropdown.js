"use strict";

var dropdown = {
    /** create custom meta-data we're using to keep track of popup state */
    createItemState : function(dropDownElement) {
        const data = {
            dropDownElement : dropDownElement,
            popupElement: null,
            keyPressed : function(event)
            {
                // TODO: Ignore meta-keys, enter
                if ( event.ctrlKey || event.key === 'Enter' ) {
                    return;
                }
                if (data.popupElement) {
                    if ( event.key === 'Backspace' ) {
                        const s = data.popupElement.innerText;
                        if ( s ) {
                            data.popupElement.innerText = s.substring(0, s.length - 1);
                        }
                    } else {
                        data.popupElement.innerText += event.key;
                    }
                } else {
                    if ( event.key === 'Backspace' ) {
                        return;
                    }
                    const popupElem = document.createElement("div");
                    popupElem.setAttribute("class", "dropdown-popup");
                    popupElem.innerText = event.key;
                    data.dropDownElement.after(popupElem);
                    data.popupElement = popupElem;
                }
            }
        };
        return data;
    },
    /** Get custom meta-data we attached to the <select/> DOM element */
    getData : function(dropDownElement) {
       return dropDownElement.dropDownData;
    },
    /** Create our custom meta-data to attach to the <select/> DOM element */
    initData : function(dropDownElement) {
        const newData = dropdown.createItemState(dropDownElement);
        dropDownElement.dropDownData = newData;
        return newData;
    },
    /** start listening for keypress events */
    attach : function(elementId) {
        const dropDownElement = document.getElementById(elementId);
        if ( dropDownElement ) {
            if ( ! dropdown.data(dropDownElement) ) {
                const data = dropdown.initData(dropDownElement);
                dropDownElement.addEventListener("keypress", data.keyPressed );

            }
        } else {
            console.error("Failed to locate DOM element '" + elementId + "'");
        }
    }
};