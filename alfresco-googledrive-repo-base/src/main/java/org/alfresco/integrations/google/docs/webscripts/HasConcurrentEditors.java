/**
 * Copyright (C) 2005-2015 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Alfresco. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.alfresco.integrations.google.docs.webscripts;

import static org.apache.commons.httpclient.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.commons.httpclient.HttpStatus.SC_INTERNAL_SERVER_ERROR;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.integrations.google.docs.exceptions.GoogleDocsAuthenticationException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsRefreshTokenException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsServiceException;
import org.alfresco.integrations.google.docs.service.GoogleDocsService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.api.client.auth.oauth2.Credential;

/**
 * @author Jared Ottley <jared.ottley@alfresco.com>
 */
public class HasConcurrentEditors extends GoogleDocsWebScripts
{
    private GoogleDocsService googledocsService;

    private final static String MODEL_CONCURRENT_EDITORS = "concurrentEditors";
    private final static String PARAM_NODEREF            = "nodeRef";

    public void setGoogledocsService(GoogleDocsService googledocsService)
    {
        this.googledocsService = googledocsService;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        getGoogleDocsServiceSubsystem();

        Map<String, Object> model = new HashMap<>();

        String param_nodeRef = req.getParameter(PARAM_NODEREF);
        NodeRef nodeRef = new NodeRef(param_nodeRef);

        try
        {
            Credential credential = googledocsService.getCredential();

            model.put(MODEL_CONCURRENT_EDITORS,
                googledocsService.hasConcurrentEditors(credential, nodeRef));
        }
        catch (GoogleDocsAuthenticationException | GoogleDocsRefreshTokenException e)
        {
            throw new WebScriptException(SC_BAD_GATEWAY, e.getMessage());
        }
        catch (GoogleDocsServiceException e)
        {
            if (e.getPassedStatusCode() > -1)
            {
                throw new WebScriptException(e.getPassedStatusCode(), e.getMessage());
            }
            else
            {
                throw new WebScriptException(e.getMessage());
            }
        }
        catch (Exception e)
        {
            throw new WebScriptException(SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

        return model;
    }
}
