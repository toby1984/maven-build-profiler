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
package de.codesourcery.maven.buildprofiler.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.codesourcery.maven.buildprofiler.common.BuildResult;
import de.codesourcery.maven.buildprofiler.server.db.DbService;
import de.codesourcery.maven.buildprofiler.shared.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.net.UnknownHostException;

@Controller
@RequestMapping("/api")
public class RestAPI
{
    private static final Logger LOG = LogManager.getLogger( RestAPI.class );

    @Autowired
    private DbService dao;

    @PostMapping("/receive")
    @ResponseBody // needed for void methods, otherwise caller gets 404 after this method completes, see https://stackoverflow.com/questions/32503605/controller-returning-404-page-with-void-method
    void receive(@RequestBody BuildResult data, HttpServletRequest currentRequest) throws JsonProcessingException, UnknownHostException
    {
        LOG.info("Incoming request from " + currentRequest.getRemoteAddr() +
            " (" + currentRequest.getRemoteHost() + "), project '"+data.projectName+"', branch '"+data.branchName+"', GIT hash "+data.gitHash);

        if ( data.jsonSyntaxVersion > Constants.JSON_SYNTAX_VERSION ) {
            LOG.error( "Unsupported JSON syntax version " + data.jsonSyntaxVersion+", client newer than server?");
            throw new RuntimeException( "Unsupported JSON syntax version " + data.jsonSyntaxVersion + ", client newer than server?" );
        }
        dao.save( data );
    }
}
