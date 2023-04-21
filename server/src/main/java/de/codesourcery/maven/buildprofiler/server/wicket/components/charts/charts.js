"use strict";

var charts = {

    sendAjax : function(callbackUrl, successCallback, failureCallback)
    {
        const xhr = new XMLHttpRequest();

        xhr.onreadystatechange = function ()
        {
            // Only run if the request is complete
            if (xhr.readyState !== 4) {
                return;
            }

            // Process our return data
            if (xhr.status >= 200 && xhr.status < 300) {
                console.debug("XMLHTTP request returned '" + xhr.responseText + "'");
                successCallback(JSON.parse(xhr.responseText));
            } else {
                console.error("XMLHTTP request failed (" + xhr.status + ")");
                if ( failureCallback ) {
                    failureCallback(xhr);
                }
            }
        };
        xhr.open('GET', callbackUrl );
        xhr.send();
    },

    linechart : function(elementId) {
        console.log("Attaching linechart to " + elementId);
        const elem = document.getElementById(elementId);
        if ( elem )
        {
            const callbackUrl = elem.getAttribute("callbackUrl");
            // perform XHR request to retrieve chart properties
            const success = chartConfig => {
                new frappe.Chart("#" + elementId, chartConfig );
            };
            charts.sendAjax(callbackUrl, success);
        } else {
            console.error("Failed to find chart DOM element '" + elementId + "'");
        }
    }
};