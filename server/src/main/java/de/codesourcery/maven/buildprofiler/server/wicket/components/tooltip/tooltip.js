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

var tooltip = {
    listenerAdded : false,
    currentElement : null,
    tooltipDiv : null,
    mouseX : 0,
    mouseY : 0,
    TT_X_OFFSET_PX : 5,
    TT_Y_OFFSET_PX : 5,
    mouseMoved : function(event)
    {
        tooltip.mouseX = event.clientX;
        tooltip.mouseY = event.clientY;
        if ( tooltip.tooltipDiv != null )
        {
            const ttRect = tooltip.currentElement.getBoundingClientRect();

            const mouseX = event.clientX;
            const mouseY = event.clientY;
            const mousePtrOutsideTooltip = ttRect.left > mouseX ||
                ttRect.top > mouseY ||
                (ttRect.left + ttRect.width) <= mouseX ||
                (ttRect.top + ttRect.height) <= mouseY;

            if ( mousePtrOutsideTooltip ) {
                console.debug("Hiding tooltip because mouse is outside");
                tooltip.hideTooltip();
            } else {
                tooltip.positionTooltipDiv(mouseX+tooltip.TT_X_OFFSET_PX, mouseY+tooltip.TT_Y_OFFSET_PX);
            }
        }
    },
    init : function(elementId)
    {
        if ( ! tooltip.listenerAdded ) {
            tooltip.getBody().addEventListener("mousemove", tooltip.mouseMoved);
            tooltip.listenerAdded = true;
        }
        console.log("Attaching tooltip to '"+elementId+"'");
        const element = document.getElementById(elementId);
        element.addEventListener("mouseenter", function() {
            tooltip.showTooltip(elementId);
        });
    },
    isTooltipVisible : function() {
        return tooltip.tooltipDiv != null;
    },
    positionTooltipDiv : function(x,y) {
        tooltip.tooltipDiv.style.left = x;
        tooltip.tooltipDiv.style.top = y;
    },
    showTooltip : function(elementId)
    {
        console.log("mouse-enter '" + elementId + "'");
        if ( ! tooltip.isTooltipVisible() )
        {
            let elem = document.getElementById(elementId);
            if ( elem ) {
                const callbackUrl = elem.getAttribute("callbackUrl");
                const success = function(responseObj)
                {
                    elem = document.getElementById(elementId);
                    if ( responseObj.tooltipText && ! tooltip.isTooltipVisible() && elem != null )
                    {
                        const rect = elem.getBoundingClientRect();
                        const div = document.createElement("div");
                        div.setAttribute("class", "tooltipDiv");
                        div.setAttribute("style", "width:" + responseObj.width+";left: "+(tooltip.mouseX+tooltip.TT_X_OFFSET_PX)+";top: "+(tooltip.mouseY+tooltip.TT_Y_OFFSET_PX));
                        div.innerHTML = responseObj.tooltipText;
                        console.debug("Showing tooltip for '" + elementId + "' at " + rect.top + ", " + rect.left);
                        const body = tooltip.getBody();
                        body.appendChild(div);
                        tooltip.currentElement = elem;
                        tooltip.tooltipDiv = div;
                    }
                };
                const failure = function(xhr) {
                };
                tooltip.sendAjax(callbackUrl,success, failure);
            } else {
                console.error("Failed to find element '" + elementId + "' in DOM");
            }
        }
    },
    getBody : function() {
        return document.getElementsByTagName("body")[0];
    },
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
    hideTooltip : function() {
        if ( tooltip.isTooltipVisible() ) {
            console.debug("Hiding tooltip");
            const body = tooltip.getBody();
            body.removeChild( tooltip.tooltipDiv );
            tooltip.tooltipDiv = null;
            tooltip.currentElement = null;
        }
    }
};