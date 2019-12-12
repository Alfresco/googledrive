/*
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.integrations.google.docs.webscripts;

import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ALF_SITES_PATH_FQNS_ELEMENT;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.ASPECT_EDITING_IN_GOOGLE;
import static org.alfresco.model.ContentModel.ASPECT_TEMPORARY;
import static org.apache.commons.httpclient.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST;
import static org.apache.commons.httpclient.HttpStatus.SC_CONFLICT;
import static org.apache.commons.httpclient.HttpStatus.SC_FORBIDDEN;
import static org.apache.commons.httpclient.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_ACCEPTABLE;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.integrations.google.docs.exceptions.GoogleDocsAuthenticationException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsRefreshTokenException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsServiceException;
import org.alfresco.integrations.google.docs.service.GoogleDocsService;
import org.alfresco.integrations.google.docs.utils.FileNameUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.model.File;

public class DiscardContent extends GoogleDocsWebScripts
{
    private static final Log log = LogFactory.getLog(DiscardContent.class);

    private GoogleDocsService  googledocsService;
    private TransactionService transactionService;
    private SiteService        siteService;
    private FileNameUtil       fileNameUtil;

    private static final String JSON_KEY_NODEREF  = "nodeRef";
    private static final String JSON_KEY_OVERRIDE = "override";

    private static final String MODEL_SUCCESS = "success";

    public void setGoogledocsService(GoogleDocsService googledocsService)
    {
        this.googledocsService = googledocsService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }

    public void setFileNameUtil(FileNameUtil fileNameUtil)
    {
        this.fileNameUtil = fileNameUtil;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        getGoogleDocsServiceSubsystem();

        Map<String, Object> model = new HashMap<>();

        Map<String, Serializable> map = parseContent(req);
        final NodeRef nodeRef = (NodeRef) map.get(JSON_KEY_NODEREF);

        final Credential credential;
        boolean deleted = false;

        if (!nodeService.hasAspect(nodeRef, ASPECT_EDITING_IN_GOOGLE))
        {
            throw new WebScriptException(SC_NOT_ACCEPTABLE,
                "Missing Google Docs Aspect on " + nodeRef.toString());
        }
        try
        {
            //if the user is the lock owner run as the calling user else if the calling user is a Site Admin masquerade as the
            // Google Docs lock owner
            if (googledocsService.isGoogleDocsLockOwner(nodeRef))
            {
                credential = googledocsService.getCredential();

                if (!Boolean.valueOf(map.get(JSON_KEY_OVERRIDE).toString()))
                {
                    SiteInfo siteInfo = null;
                    String pathElement = getPathElement(nodeRef, 2);

                    //Is the node in a site?
                    if (pathElement.equals(ALF_SITES_PATH_FQNS_ELEMENT))
                    {
                        siteInfo = fileNameUtil.resolveSiteInfo(nodeRef);
                    }
                    //The second part of this test maybe too exclusive.  What if the user has write permissions to the node
                    // but not membership in the containing site? Should the test just ask if the user has write permission
                    // to the node?
                    if (siteInfo == null || siteService.isMember(siteInfo.getShortName(),
                        AuthenticationUtil.getRunAsUser()))
                    {
                        if (googledocsService.hasConcurrentEditors(credential, nodeRef))
                        {
                            throw new WebScriptException(SC_CONFLICT,
                                "Node: " + nodeRef.toString() + " has concurrent editors.");
                        }
                    }
                    else
                    {
                        throw new AccessDeniedException(
                            "Access Denied.  You do not have the appropriate permissions to perform this operation.");
                    }
                }

                deleted = delete(credential, nodeRef);
            }
            else if (googledocsService.isSiteManager(nodeRef,
                AuthenticationUtil.getFullyAuthenticatedUser()))
            {
                final String lockOwner = googledocsService.getGoogleDocsLockOwner(nodeRef);

                if (lockOwner != null)
                {
                    deleted = AuthenticationUtil.runAs(() -> {
                        boolean deletedAsUser;

                        try
                        {
                            deletedAsUser = delete(null, nodeRef);
                        }
                        // If we are unable to delete the document from Drive (because we no longer have permission) we still need to unlock the document
                        // and clean up (undecorate) the node.
                        catch (GoogleDocsServiceException | GoogleDocsAuthenticationException e)
                        {
                            Throwable thrown = e.getCause();

                            if ((thrown instanceof GoogleJsonResponseException ||
                                 thrown instanceof TokenResponseException))
                            {
                                final int status1;

                                if (thrown instanceof GoogleJsonResponseException)
                                {
                                    status1 = ((GoogleJsonResponseException) thrown).getStatusCode();
                                }
                                else
                                {
                                    status1 = ((TokenResponseException) thrown).getStatusCode();
                                }

                                if (status1 == SC_UNAUTHORIZED || status1 == SC_BAD_REQUEST)
                                {
                                    log.info(
                                        "Unable to access " + nodeRef + " as " + lockOwner);
                                    googledocsService.unlockNode(nodeRef);
                                    googledocsService.unDecorateNode(nodeRef);

                                    if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
                                    {
                                        nodeService.deleteNode(nodeRef);
                                    }
                                }
                                else
                                {
                                    throw e;
                                }

                                deletedAsUser = true;
                            }
                            else
                            {
                                throw e;
                            }
                        }
                        catch (IllegalStateException e)
                        {
                            log.info("Unable to access " + nodeRef + " as " + lockOwner);
                            googledocsService.unlockNode(nodeRef);
                            googledocsService.unDecorateNode(nodeRef);

                            if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
                            {
                                nodeService.deleteNode(nodeRef);
                            }

                            deletedAsUser = true;
                        }

                        return deletedAsUser;
                    }, lockOwner);
                }
            }

            model.put(MODEL_SUCCESS, deleted);
        }
        catch (InvalidNodeRefException e)
        {
            throw new WebScriptException(SC_NOT_FOUND, e.getMessage());
        }
        catch (IOException e)
        {
            throw new WebScriptException(SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
        catch (GoogleDocsAuthenticationException | GoogleDocsRefreshTokenException e)
        {
            throw new WebScriptException(SC_BAD_GATEWAY, e.getMessage());
        }
        catch (GoogleDocsServiceException e)
        {
            if (e.getPassedStatusCode() == SC_NOT_FOUND)
            {
                // This code will make changes after the rollback has occurred to clean up the node: remove the lock and the Google
                // Docs aspect. If it has the temporary aspect it will also remove the node from Alfresco
                AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter()
                {
                    public void afterCommit()
                    {
                        transactionService.getRetryingTransactionHelper().doInTransaction(
                            () -> {
                                AuthenticationUtil.runAsSystem(() -> {
                                    googledocsService.unlockNode(nodeRef);
                                    googledocsService.unDecorateNode(nodeRef);

                                    if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
                                    {
                                        nodeService.deleteNode(nodeRef);
                                    }

                                    return null;
                                });

                                return null;
                            }, false, true);
                    }
                });
                model.put(MODEL_SUCCESS, true);
            }
            else if (e.getPassedStatusCode() > -1)
            {
                throw new WebScriptException(e.getPassedStatusCode(), e.getMessage());
            }
            throw new WebScriptException(e.getMessage());
        }
        catch (AccessDeniedException e)
        {
            // This code will make changes after the rollback has occurred to clean up the node: remove the lock and the Google
            // Docs aspect. If it has the temporary aspect it will also remove the node from Alfresco
            AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter()
            {
                public void afterRollback()
                {
                    transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
                        File file = googledocsService.getDriveFile(null, nodeRef);
                        googledocsService.unlockNode(nodeRef);
                        boolean deleted1 = googledocsService.deleteContent(null, nodeRef, file);

                        if (deleted1)
                        {
                            AuthenticationUtil.runAsSystem(() -> {
                                googledocsService.unlockNode(nodeRef);
                                googledocsService.unDecorateNode(nodeRef);

                                if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
                                {
                                    nodeService.deleteNode(nodeRef);
                                }

                                return null;
                            });
                        }

                        return null;
                    }, false, true);
                }
            });

            throw new WebScriptException(SC_FORBIDDEN, e.getMessage(), e);
        }

        return model;
    }

    /**
     * Delete the node from Google. If the node has the temporary aspect it is also removed from Alfresco.
     *
     * @param nodeRef
     * @return
     * @throws InvalidNodeRefException
     * @throws IOException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     */
    private boolean delete(Credential credential, NodeRef nodeRef)
        throws InvalidNodeRefException,
        IOException,
        GoogleDocsServiceException,
        GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException
    {
        File file = googledocsService.getDriveFile(credential, nodeRef);
        googledocsService.unlockNode(nodeRef);
        boolean deleted = googledocsService.deleteContent(credential, nodeRef, file);

        if (deleted)
        {
            if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
            {
                nodeService.deleteNode(nodeRef);
            }
        }

        return deleted;
    }

    private Map<String, Serializable> parseContent(final WebScriptRequest req)
    {
        final Map<String, Serializable> result = new HashMap<>();
        Content content = req.getContent();
        String jsonStr = null;
        JSONObject json;

        try
        {
            if (content == null || content.getSize() == 0)
            {
                throw new WebScriptException(SC_BAD_REQUEST, "No content sent with request.");
            }

            jsonStr = content.getContent();
            log.debug("Parsed JSON: " + jsonStr);

            if (jsonStr == null || jsonStr.trim().length() == 0)
            {
                throw new WebScriptException(SC_BAD_REQUEST, "No content sent with request.");
            }

            json = new JSONObject(jsonStr);

            if (!json.has(JSON_KEY_NODEREF))
            {
                throw new WebScriptException(SC_BAD_REQUEST,
                    "Key " + JSON_KEY_NODEREF + " is missing from JSON: "
                    + jsonStr);
            }
            NodeRef nodeRef = new NodeRef(json.getString(JSON_KEY_NODEREF));
            result.put(JSON_KEY_NODEREF, nodeRef);

            if (json.has(JSON_KEY_OVERRIDE))
            {
                result.put(JSON_KEY_OVERRIDE, json.getBoolean(JSON_KEY_OVERRIDE));
            }
            else
            {
                result.put(JSON_KEY_OVERRIDE, false);
            }
        }
        catch (final IOException e)
        {
            throw new WebScriptException(SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
        catch (final JSONException e)
        {
            throw new WebScriptException(SC_BAD_REQUEST, "Unable to parse JSON: " + jsonStr);
        }
        catch (final WebScriptException e)
        {
            throw e; // Ensure WebScriptExceptions get rethrown verbatim
        }
        catch (final Exception e)
        {
            throw new WebScriptException(SC_BAD_REQUEST, "Unable to parse JSON '" + jsonStr + "'.",
                e);
        }

        return result;
    }
}
