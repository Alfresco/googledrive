/**
 * Copyright (C) 2005-2018 Alfresco Software Limited.
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

package org.alfresco.integrations.google.docs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Jared Ottley <jared.ottley@alfresco.com>
 */
public interface GoogleDocsConstants
{

    // OAuth2 Credential Store -- remotesystem name
    String REMOTE_SYSTEM = "googledocs";

    // Google OAuth2 redirect URI
    String REDIRECT_URI                = "https://www.alfresco.com/google-auth-return.html";
    String CLIENT_SECRET_WEB           = "web";
    String CLIENT_SECRET_REDIRECT_URIS = "redirect_uris";

    // Google OAuth2 Scopes
    String       SCOPE  = "https://docs.google.com/feeds/ https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email";
    List<String> SCOPES = Collections.unmodifiableList(
        Arrays.asList("https://docs.google.com/feeds/",
            "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/userinfo.email"));

    // Google docsService Client Name
    String APPLICATION_NAME = "Alfresco-GoogleDocs/3.1";

    // Google contentTypes
    String DOCUMENT_TYPE     = "document";
    String PRESENTATION_TYPE = "presentation";
    String SPREADSHEET_TYPE  = "spreadsheet";

    // Google Docs Mimetypes
    String DOCUMENT_MIMETYPE     = "application/vnd.google-apps.document";
    String SPREADSHEET_MIMETYPE  = "application/vnd.google-apps.spreadsheet";
    String PRESENTATION_MIMETYPE = "application/vnd.google-apps.presentation";
    String FOLDER_MIMETYPE       = "application/vnd.google-apps.folder";

    // Google mimetypes
    String MIMETYPE_DOCUMENT     = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    String MIMETYPE_PRESENTATION = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    String MIMETYPE_SPREADSHEET  = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    String MIMETYPE_ODT          = "application/vnd.oasis.opendocument.text";

    // Google New Document Names
    String NEW_DOCUMENT_NAME     = "Untitled Document";
    String NEW_PRESENTATION_NAME = "Untitled Presentation";
    String NEW_SPREADSHEET_NAME  = "Untitled Spreadsheet";

    // Google Drive Root Folder Id
    String ROOT_FOLDER_ID = "root";

    // Google Drive Alfresco Working Directory
    String ALF_TEMP_FOLDER      = "Alfresco Working Directory";
    String ALF_TEMP_FOLDER_DESC = "Alfresco - Google Docs Working Directory";

    String ALF_SHARED_FILES_FOLDER = "Shared Files";
    String ALF_MY_FILES_FOLDER     = "My Files";

    String ALF_SHARED_PATH_FQNS_ELEMENT = "{http://www.alfresco.org/model/application/1.0}shared";
    String ALF_SITES_PATH_FQNS_ELEMENT  = "{http://www.alfresco.org/model/site/1.0}sites";

    String GOOGLE_ERROR_UNMUTABLE = "File not mutable";

    /*
     * There is no standard 419. Some say not set (like Alfresco); Apache says WebDav INSUFFICIENT_SPACE_ON_RESOURCE.
     *
     * Cut our loses and create our own.
     */
    int STATUS_INTEGIRTY_VIOLATION = 419;
}
