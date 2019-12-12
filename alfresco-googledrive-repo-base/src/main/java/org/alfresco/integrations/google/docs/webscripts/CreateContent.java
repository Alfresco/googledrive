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

import static org.alfresco.integrations.google.docs.GoogleDocsConstants.DOCUMENT_TYPE;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.MIMETYPE_DOCUMENT;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.MIMETYPE_PRESENTATION;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.MIMETYPE_SPREADSHEET;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.NEW_DOCUMENT_NAME;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.NEW_PRESENTATION_NAME;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.NEW_SPREADSHEET_NAME;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.PRESENTATION_TYPE;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.SPREADSHEET_TYPE;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.apache.commons.httpclient.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.commons.httpclient.HttpStatus.SC_CONFLICT;
import static org.apache.commons.httpclient.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.httpclient.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.commons.httpclient.HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.integrations.google.docs.exceptions.GoogleDocsAuthenticationException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsRefreshTokenException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsServiceException;
import org.alfresco.integrations.google.docs.service.GoogleDocsService;
import org.alfresco.integrations.google.docs.utils.FileNameUtil;
import org.alfresco.repo.management.subsystems.ApplicationContextFactory;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;

/**
 * @author Jared Ottley <jared.ottley@alfresco.com>
 */
public class CreateContent extends GoogleDocsWebScripts
{
    private static final Log log = LogFactory.getLog(CreateContent.class);

    private final static String FILENAMEUTIL = "fileNameUtil";

    private GoogleDocsService googledocsService;
    private FileFolderService fileFolderService;

    private FileNameUtil fileNameUtil;

    private final static String PARAM_TYPE   = "contenttype";
    private final static String PARAM_PARENT = "parent";

    private final static String MODEL_NODEREF    = "nodeRef";
    private final static String MODEL_EDITOR_URL = "editorUrl";

    public void setGoogledocsService(GoogleDocsService googledocsService)
    {
        this.googledocsService = googledocsService;
    }

    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    public void setFileNameUtil(FileNameUtil fileNameUtil)
    {
        this.fileNameUtil = fileNameUtil;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        // Set Service Beans
        this.getGoogleDocsServiceSubsystem();

        Map<String, Object> model = new HashMap<>();

        if (!googledocsService.isEnabled())
        {
            throw new WebScriptException(SC_SERVICE_UNAVAILABLE, "Google Docs Disabled");
        }

        String contentType = req.getParameter(PARAM_TYPE);
        NodeRef parentNodeRef = new NodeRef(req.getParameter(PARAM_PARENT));

        log.debug("ContentType: " + contentType + "; Parent: " + parentNodeRef);

        NodeRef newNode;
        File file;
        try
        {
            Credential credential = googledocsService.getCredential();
            switch (contentType)
            {
            case DOCUMENT_TYPE:
                newNode = createFile(parentNodeRef, contentType, MIMETYPE_DOCUMENT);
                file = googledocsService.createDocument(credential, newNode);
                break;
            case SPREADSHEET_TYPE:
                newNode = createFile(parentNodeRef, contentType, MIMETYPE_SPREADSHEET);
                file = googledocsService.createSpreadSheet(credential, newNode);
                break;
            case PRESENTATION_TYPE:
                newNode = createFile(parentNodeRef, contentType, MIMETYPE_PRESENTATION);
                file = googledocsService.createPresentation(credential, newNode);
                break;
            default:
                throw new WebScriptException(SC_UNSUPPORTED_MEDIA_TYPE,
                    "Content Type Not Found.");
            }

            googledocsService.decorateNode(newNode, file,
                googledocsService.getLatestRevision(credential, file), true);
        }
        catch (GoogleDocsServiceException e)
        {
            if (e.getPassedStatusCode() > -1)
            {
                throw new WebScriptException(e.getPassedStatusCode(), e.getMessage());
            }
            throw new WebScriptException(e.getMessage());
        }
        catch (GoogleDocsAuthenticationException | GoogleDocsRefreshTokenException e)
        {
            throw new WebScriptException(SC_BAD_GATEWAY, e.getMessage());
        }
        catch (Exception e)
        {
            throw new WebScriptException(SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

        googledocsService.lockNode(newNode);

        model.put(MODEL_NODEREF, newNode.toString());
        model.put(MODEL_EDITOR_URL, file.getWebViewLink());

        return model;
    }

    /**
     * Create a new content item for a document, spreadsheet or presentation which is to be edited in Google Docs
     *
     * <p>The name of the file is generated automatically, based on the type of content. In the event of a clash with
     * an existing file, the file name will have a numeric suffix placed on the end of it before the file extension,
     * which will be incremented until a valid name is found.</p>
     *
     * @param parentNodeRef NodeRef identifying the folder where the content will be created
     * @param contentType   The type of content to be created, one of 'document', 'spreadsheet' or 'presentation'
     * @param mimetype      The mimetype of the new content item, used to determine the file extension to add
     * @return A FileInfo object representing the new content item. Call fileInfo.getNodeRef() to get the nodeRef
     */
    private NodeRef createFile(final NodeRef parentNodeRef, final String contentType,
        final String mimetype)
    {
        final String baseName = getNewFileName(contentType), fileExt =
            fileNameUtil.getExtension(mimetype);
        final StringBuilder sb = new StringBuilder(baseName);
        if (fileExt != null && !fileExt.equals(""))
        {
            sb.append(".").append(fileExt);
        }
        int i = 0, maxCount = 1000; // Limit the damage should something go horribly wrong and a FileExistsException is always thrown

        while (i <= maxCount)
        {
            List<String> parts = new ArrayList<>(1);
            parts.add(QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                sb.toString()).toPrefixString());
            try
            {
                if (fileFolderService.resolveNamePath(parentNodeRef, parts, false) == null)
                {
                    return fileFolderService.create(parentNodeRef, sb.toString(),
                        TYPE_CONTENT).getNodeRef();
                }
                log.debug("Filename " + sb.toString() + " already exists");
                String name = fileNameUtil.incrementFileName(sb.toString());
                sb.replace(0, sb.length(), name);
                if (log.isDebugEnabled())
                {
                    log.debug("new file name " + sb.toString());
                }
            }
            catch (FileNotFoundException e) // We should never catch this because we set mustExist=false
            {
                throw new WebScriptException(SC_INTERNAL_SERVER_ERROR,
                    "Unexpected FileNotFoundException", e);
            }
            i++;
        }
        throw new WebScriptException(SC_CONFLICT,
            "Too many untitled files. Try renaming some existing documents.");
    }

    /**
     * Get the default new content name
     *
     * @param type
     * @return
     */
    private static String getNewFileName(String type)
    {
        String name = null;
        switch (type)
        {
        case DOCUMENT_TYPE:
            name = NEW_DOCUMENT_NAME;
            break;
        case SPREADSHEET_TYPE:
            name = NEW_SPREADSHEET_NAME;
            break;
        case PRESENTATION_TYPE:
            name = NEW_PRESENTATION_NAME;
            break;
        }

        return name;
    }

    protected void getGoogleDocsServiceSubsystem()
    {
        ApplicationContextFactory subsystem = (ApplicationContextFactory) applicationContext
            .getBean(GOOGLEDOCS_DRIVE_SUBSYSTEM);
        ConfigurableApplicationContext childContext = (ConfigurableApplicationContext) subsystem.getApplicationContext();
        setGoogledocsService((GoogleDocsService) childContext.getBean(GOOGLEDOCSSERVICE));
        setFileNameUtil((FileNameUtil) childContext.getBean(FILENAMEUTIL));
    }
}
