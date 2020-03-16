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

package org.alfresco.integrations.google.docs.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.integrations.google.docs.exceptions.GoogleDocsAuthenticationException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsRefreshTokenException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsServiceException;
import org.alfresco.integrations.google.docs.exceptions.MustDowngradeFormatException;
import org.alfresco.integrations.google.docs.exceptions.MustUpgradeFormatException;
import org.alfresco.service.Auditable;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.User;

/**
 * @author Jared Ottley <jared.ottley@alfresco.com>
 */
public interface GoogleDocsService
{
    @Auditable
    Credential getCredential() throws GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException, IOException;

    /**
     * Has the current user completed OAuth2 authentication against Google Docs
     *
     * @return
     */
    @Auditable
    boolean isAuthenticated();

    /**
     * Build the OAuth2 URL needed to authenticate the current user against Google Docs
     *
     * @param state
     * @return
     */
    @Auditable(parameters = {"state"})
    String getAuthenticateUrl(String state) throws GoogleDocsServiceException;

    /**
     * Complete the OAuth2 Dance for the current user, persisting the OAuth2 Tokens.
     *
     * @param authorizationCode
     * @return
     */
    @Auditable
    boolean completeAuthentication(String authorizationCode) throws GoogleDocsServiceException,
        IOException;

    /**
     * Is the Google Docs Integration enabled
     *
     * @return
     */
    @Auditable
    boolean isEnabled();

    @Auditable
    User getDriveUser(Credential credential) throws GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException, IOException;

    /**
     * Create new Google Docs Document
     *
     * @param credential
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    File createDocument(Credential credential, NodeRef nodeRef) throws GoogleDocsServiceException,
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException, IOException;

    /**
     * Create new Google Docs Presentation
     *
     * @param credential
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    File createPresentation(Credential credential, NodeRef nodeRef) throws
        GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Create new Google Docs SpreadSheet
     *
     * @param credential
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    File createSpreadSheet(Credential credential, NodeRef nodeRef) throws
        GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Apply/Update GoogleDocs Aspect
     *
     * @param nodeRef
     * @param file
     * @param newcontent If this is a new node, mark the content as temporary.
     */
    void decorateNode(NodeRef nodeRef, File file, boolean newcontent);

    /**
     * Apply/Update GoogleDocs Aspect
     *
     * @param nodeRef
     * @param file
     * @param revision
     * @param newcontent If this is a new node, mark the content as temporary.
     */
    void decorateNode(NodeRef nodeRef, File file, Revision revision, boolean newcontent);

    /**
     * Apply/Update GoogleDocs Aspect
     *
     * @param nodeRef
     * @param file
     * @param revision
     * @param permissions List of permissions which have been applied to the file in Google, to be stored in against the node, used for re-creating permissions if the remote copy is removed.
     * @param newcontent  If this is a new node, mark the content as temporary.
     */
    void decorateNode(NodeRef nodeRef, File file, Revision revision,
        List<GoogleDocsService.GooglePermission> permissions, boolean newcontent);

    /**
     * Remove Google Docs Aspect
     *
     * @param nodeRef
     */
    void unDecorateNode(NodeRef nodeRef);

    /**
     * Can the mimetype be exported from Google Docs?
     *
     * @param mimetype
     * @return
     * @throws MustUpgradeFormatException   Thrown if the mimetype must be changed to a newer mimetype (ex. sxw -> odt)
     * @throws MustDowngradeFormatException Thrown if the mimetype must be changed to an older mimetype (ex. docx -> doc)
     */
    boolean isExportable(String mimetype) throws MustUpgradeFormatException,
        MustDowngradeFormatException;

    /**
     * Can the mimetype be imported into Google Docs?
     *
     * @param mimetype
     * @return
     */
    boolean isImportable(String mimetype);

    /**
     * List of mimetypes that can be imported into Google Docs and the Google Doc Type
     *
     * @return
     */
    Map<String, String> getImportFormats();

    /**
     * Get the Google Doc Content type. A content type is a mapping of mimetype to Google Doc Type (document, spreadsheet or
     * presentation)
     *
     * @param nodeRef
     * @return
     */
    String getContentType(NodeRef nodeRef);

    /**
     * Retrieve the Google Doc Document associated to this node from Google Docs into the repository. Removes the document from the
     * users Google Drive account
     *
     * @param credential
     * @param nodeRef
     */
    @Auditable(parameters = {"nodeRef"})
    void getDocument(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Retrieve the Google Doc Document associated to this node from Google Docs into the repository. Removes the document from the
     * users Google Drive account if removeFromDrive is true
     *
     * @param credential
     * @param nodeRef
     * @param removeFromDrive
     */
    @Auditable(parameters = {"nodeRef", "removeFrom Drive"})
    void getDocument(Credential credential, NodeRef nodeRef, boolean removeFromDrive) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Retrieve the Google Doc Spreadsheet associated to this node from Google Docs into the repository. Removes the spreadsheet
     * from the users Google Drive account
     *
     * @param credential
     * @param nodeRef
     */
    @Auditable(parameters = {"nodeRef"})
    void getSpreadSheet(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Retrieve the Google Doc Spreadsheet associated to this node from Google Docs into the repository. Removes the spreadsheet
     * from the users Google Drive account if removeFromDrive is true
     *
     * @param credential
     * @param nodeRef
     * @param removeFromDrive
     */
    @Auditable(parameters = {"nodeRef", "removeFromDrive"})
    void getSpreadSheet(Credential credential, NodeRef nodeRef, boolean removeFromDrive) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Retrieve the Google Doc Presentation associated to this node from Google Docs into the repository. Removes the presentation
     * from the users Google Drive account
     *
     * @param credential
     * @param nodeRef
     */
    @Auditable(parameters = {"nodeRef"})
    void getPresentation(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Retrieve the Google Doc Presentation associated to this node from Google Docs into the repository. Removes the presentation
     * from the users Google Drive account if removeFromDrive is true
     *
     * @param credential
     * @param nodeRef
     */
    @Auditable(parameters = {"nodeRef", "removeFromDrive"})
    void getPresentation(Credential credential, NodeRef nodeRef, boolean removeFromDrive) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Upload node to Google Docs
     *
     * @param credential
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    File uploadFile(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * @param credential
     * @param resourceID Google Drive Resource ID
     * @return
     * @throws IOException
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     */
    @Auditable(parameters = {"resourceId"})
    File getDriveFile(Credential credential, String resourceID) throws GoogleDocsServiceException,
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException, IOException;

    /**
     * @param credential
     * @param nodeRef    Alfresco Node which is currently uploaded to Google Drive
     * @return
     * @throws IOException
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     */
    @Auditable(parameters = {"resourceId"})
    File getDriveFile(Credential credential, NodeRef nodeRef) throws GoogleDocsServiceException,
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException, IOException;

    /**
     * @param credential
     * @param nodeRef
     * @param file
     * @return
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsRefreshTokenException
     * @throws IOException
     */
    @Auditable(parameters = {"nodeRef", "driveFile"})
    boolean deleteContent(Credential credential, NodeRef nodeRef, File file) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * @param credential
     * @param nodeRef
     * @return
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsRefreshTokenException
     * @throws IOException
     */
    @Auditable(parameters = {"nodeRef"})
    boolean deleteContent(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Unlock and Undecorate node; Remove content from users Google Account Does not update the content in Alfresco; If content was
     * newly created by GoogleDocsService it will be removed.
     *
     * Method can be run by owner, admin or site manager
     *
     * @param credential
     * @param nodeRef
     * @param file
     * @param forceRemoval ignore <code>GoogleDocsServiceException</code> exceptions when attempting to remove content from user's
     *                     Google account
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsAuthenticationException
     */
    @Auditable(parameters = {"nodeRef", "driveFile", "forceRemoval"})
    void removeContent(Credential credential, NodeRef nodeRef, File file,
        boolean forceRemoval) throws GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException;

    /**
     * Google Doc has Concurrent Editors
     *
     * @param credential
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    boolean hasConcurrentEditors(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException;

    /**
     * @param nodeRef
     * @return
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     */
    @Auditable(parameters = {"nodeRef"})
    Revision getLatestRevision(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException;

    /**
     * @param file
     * @return
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     */
    @Auditable(parameters = {"file"})
    Revision getLatestRevision(Credential credential, File file) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException;

    /**
     * @param nodeRef
     */
    @Auditable(parameters = {"nodeRef"})
    void lockNode(NodeRef nodeRef);

    /**
     * @param nodeRef
     */
    @Auditable(parameters = {"nodeRef"})
    void unlockNode(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    boolean isLockedByGoogleDocs(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    boolean isGoogleDocsLockOwner(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"nodeRef"})
    String getGoogleDocsLockOwner(NodeRef nodeRef);

    /**
     * List the saved Google permissions currently stored for this object.
     *
     * @param nodeRef Noderef identifying the file in the repository
     * @return A list of permissions objects stored for this node, which may be an empty list, or null if nothing is stored
     */
    @Auditable(parameters = {"nodeRef", "qname"})
    List<GooglePermission> getGooglePermissions(
        NodeRef nodeRef, QName qname);

    /**
     * Add permissions on the remote object stored in Google
     *
     * @param file        Drive file instance of the remote object
     * @param permissions A list of permissions objects stored for this node
     */
    @Auditable(parameters = {"nodeRef"})
    void addRemotePermissions(Credential credential, File file,
        List<GooglePermission> permissions) throws GoogleDocsAuthenticationException,
        GoogleDocsServiceException, GoogleDocsRefreshTokenException, IOException;

    @Auditable(parameters = {"nodeRef", "authorityName"})
    boolean isSiteManager(NodeRef nodeRef,
        String authorityName);

    Serializable buildPermissionsPropertyValue(List<GooglePermission> permissions);

    String convertWebViewToEditUrl(NodeRef nodeRef, String url);

    /**
     * Represents a named authority and their role on a Google Docs object. This should be compatible across the document list and
     * the drive APIs.
     *
     * @author Will Abson
     */
    class GooglePermission
    {
        public enum role
        {owner, reader, writer, commenter}

        public enum type
        {user, group, domain, anyone}

        private String authorityId;

        private String authorityType;

        private String roleName;

        public GooglePermission(String authorityId, String authorityType, String roleName)
        {
            this.authorityId = authorityId;
            this.authorityType = authorityType;
            this.roleName = roleName;
        }

        public GooglePermission()
        {
        }

        public String getAuthorityId()
        {
            return authorityId;
        }

        public void setAuthorityId(String authorityId)
        {
            this.authorityId = authorityId;
        }

        public String getAuthorityType()
        {
            return authorityType;
        }

        public void setAuthorityType(String authorityType)
        {
            this.authorityType = authorityType;
        }

        public String getRoleName()
        {
            return roleName;
        }

        public void setRoleName(String roleName)
        {
            this.roleName = roleName;
        }

        public String toString()
        {
            return authorityType + "|" + authorityId + "|" + roleName;
        }

        public static GooglePermission fromString(String input)
        {
            String[] parts = input.split("\\|");
            if (parts.length != 3)
            {
                throw new IllegalArgumentException(
                    "Bad number of paramters in input string '" + input + "'. Need 3, found "
                    + parts.length + ".");
            }
            GooglePermission p = new GooglePermission();
            p.setAuthorityType(parts[0]);
            p.setAuthorityId(parts[1]);
            p.setRoleName(parts[2]);
            return p;
        }
    }
}
