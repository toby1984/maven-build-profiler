/*
 * Copyright © 2023 Tobias Gierke (tobias.gierke@code-sourcery.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    createChart : function(elementId) {
        console.log("Attaching chart to " + elementId);
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