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

import static java.util.stream.Collectors.toCollection;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ALF_MY_FILES_FOLDER;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ALF_SHARED_FILES_FOLDER;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ALF_SHARED_PATH_FQNS_ELEMENT;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ALF_SITES_PATH_FQNS_ELEMENT;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ALF_TEMP_FOLDER;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ALF_TEMP_FOLDER_DESC;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.APPLICATION_NAME;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.CLIENT_SECRET_REDIRECT_URIS;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.CLIENT_SECRET_WEB;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.DOCUMENT_MIMETYPE;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.FOLDER_MIMETYPE;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.GOOGLE_ERROR_UNMUTABLE;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.MIMETYPE_DOCUMENT;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.MIMETYPE_PRESENTATION;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.MIMETYPE_SPREADSHEET;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.NEW_DOCUMENT_NAME;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.NEW_PRESENTATION_NAME;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.NEW_SPREADSHEET_NAME;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.PRESENTATION_MIMETYPE;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.REDIRECT_URI;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.REMOTE_SYSTEM;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.ROOT_FOLDER_ID;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.SCOPES;
import static org.alfresco.integrations.google.docs.GoogleDocsConstants.SPREADSHEET_MIMETYPE;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.ASPECT_EDITING_IN_GOOGLE;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.ASPECT_SHARED_IN_GOOGLE;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.PROP_CURRENT_PERMISSIONS;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.PROP_DRIVE_WORKING_FOLDER;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.PROP_EDITORURL;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.PROP_LOCKED;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.PROP_PERMISSIONS;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.PROP_RESOURCE_ID;
import static org.alfresco.integrations.google.docs.GoogleDocsModel.PROP_REVISION_ID;
import static org.alfresco.model.ContentModel.ASPECT_TEMPORARY;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.model.ContentModel.PROP_GOOGLEUSERNAME;
import static org.alfresco.model.ContentModel.PROP_NAME;
import static org.alfresco.model.ContentModel.PROP_VERSION_TYPE;
import static org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST;
import static org.apache.commons.httpclient.HttpStatus.SC_FORBIDDEN;
import static org.apache.commons.httpclient.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.integrations.google.docs.exceptions.GoogleDocsAuthenticationException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsRefreshTokenException;
import org.alfresco.integrations.google.docs.exceptions.GoogleDocsServiceException;
import org.alfresco.integrations.google.docs.exceptions.MustDowngradeFormatException;
import org.alfresco.integrations.google.docs.exceptions.MustUpgradeFormatException;
import org.alfresco.integrations.google.docs.exceptions.NotInGoogleDriveException;
import org.alfresco.integrations.google.docs.utils.FileNameUtil;
import org.alfresco.integrations.google.docs.utils.FileRevisionComparator;
import org.alfresco.query.CannedQueryPageDetails;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.lock.mem.LockState;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteServiceImpl;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.oauth2.OAuth2CredentialsStoreService;
import org.alfresco.service.cmr.remotecredentials.OAuth2CredentialsInfo;
import org.alfresco.service.cmr.remoteticket.NoSuchSystemException;
import org.alfresco.service.cmr.repository.AspectMissingException;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.Resource;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;
import com.google.api.services.drive.model.User;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;

/**
 * @author Jared Ottley <jared.ottley@alfresco.com>
 */
public class GoogleDocsServiceImpl implements GoogleDocsService
{
    private static final Log log = LogFactory.getLog(GoogleDocsServiceImpl.class);

    // Services
    private OAuth2CredentialsStoreService oauth2CredentialsStoreService;

    private HttpTransport  httpTransport;
    private JacksonFactory jsonFactory;

    private GoogleClientSecrets clientSecrets;

    private FileFolderService fileFolderService;
    private NodeService       nodeService;
    private LockService       lockservice;
    private MimetypeService   mimetypeService;
    private BehaviourFilter   behaviourFilter;
    private ActivityService   activityService;
    private SiteService       siteService;
    private TenantService     tenantService;
    private PersonService     personService;
    private AuthorityService  authorityService;

    private DictionaryService dictionaryService;
    private FileNameUtil      filenameUtil;

    // Property Mappings
    private Map<String, String>              importFormats     = new HashMap<>();
    private Map<String, Map<String, String>> exportFormats     = new HashMap<>();
    private Map<String, String>              upgradeMappings   = new HashMap<>();
    private Map<String, String>              downgradeMappings = new HashMap<>();

    // New Content
    private Resource newDocument;
    private Resource newSpreadsheet;
    private Resource newPresentation;

    // Time (in seconds) between last edit and now to consider edits as
    // concurrent
    private int idleThreshold = 0;

    private boolean enabled = true;

    private String clientSecret;

    // Activities
    private static final String FILE_ADDED   = "org.alfresco.documentlibrary.file-added";
    private static final String FILE_UPDATED = "org.alfresco.documentlibrary.file-updated";

    public void setImportFormats(Map<String, String> importFormats)
    {
        this.importFormats = importFormats;
    }

    public void setExportFormats(Map<String, Map<String, String>> exportFormats)
    {
        this.exportFormats = exportFormats;
    }

    public void setUpgradeMappings(Map<String, String> upgradeMappings)
    {
        this.upgradeMappings = upgradeMappings;
    }

    public void setDowngradeMappings(Map<String, String> downgradeMappings)
    {
        this.downgradeMappings = downgradeMappings;
    }

    public void setOauth2CredentialsStoreService(
        OAuth2CredentialsStoreService oauth2CredentialsStoreService)
    {
        this.oauth2CredentialsStoreService = oauth2CredentialsStoreService;
    }

    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setLockService(LockService lockService)
    {
        this.lockservice = lockService;
    }

    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    public void setActivityService(ActivityService activityService)
    {
        this.activityService = activityService;
    }

    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }

    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setFileNameUtil(FileNameUtil fileNameUtil)
    {
        this.filenameUtil = fileNameUtil;
    }

    public Map<String, String> getImportFormats()
    {
        return importFormats;
    }

    public void setNewDocument(Resource newDocument)
    {
        this.newDocument = newDocument;
    }

    public void setNewSpreadsheet(Resource newSpreadsheet)
    {
        this.newSpreadsheet = newSpreadsheet;
    }

    public void setNewPresentation(Resource newPresentation)
    {
        this.newPresentation = newPresentation;
    }

    public void setIdleThreshold(int idleThreshold)
    {
        this.idleThreshold = idleThreshold;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void init() throws IOException
    {
        httpTransport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();

        if (isBlank(clientSecret))
        {
            clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(
                GoogleDocsServiceImpl.class.getResourceAsStream("client_secret.json")));
        }
        else
        {
            clientSecrets = GoogleClientSecrets.load(jsonFactory, new StringReader(clientSecret));
        }
    }

    // Required fields from the response
    private static final String ALL_PROPERTY_FIELDS = "*";

    public void setClientSecret(String clientSecret)
        throws GoogleDocsServiceException
    {
        if (validateClientSecret(clientSecret))
        {
            this.clientSecret = clientSecret;
        }
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Can the mimetype be imported from Google Docs to Alfresco?
     *
     * @param mimetype
     * @return boolean
     */
    public boolean isImportable(String mimetype)
    {
        return importFormats.containsKey(mimetype);
    }

    /**
     * Get the Google document type (Document, Spreadsheet, Presentation)
     *
     * @param mimetype
     * @return String
     */
    private String getImportType(String mimetype)
    {
        return importFormats.get(mimetype);
    }

    /**
     * @param mimetype
     * @return
     * @throws
     */
    public boolean isExportable(String mimetype) throws MustUpgradeFormatException,
        MustDowngradeFormatException
    {
        if (isUpgrade(mimetype))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Mimetype " + mimetype +
                          " cannot be exported directly and will be upgraded");
            }
            throw new MustUpgradeFormatException();
        }
        else if (isDownGrade(mimetype))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Mimetype " + mimetype +
                          " cannot be exported directly and will be upgraded");
            }
            throw new MustDowngradeFormatException();
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Mimetype " + mimetype + " can be exported");
            }
        }

        String type = getImportType(mimetype);
        Set<String> exportMimetypes = getExportableMimeTypes(type);

        return exportMimetypes.contains(mimetype);
    }

    /**
     * Get a Set of all the mimetypes that can be exported for the Google Document type
     *
     * @param type
     * @return Set
     */
    private Set<String> getExportableMimeTypes(String type)
    {
        if (exportFormats.containsKey(type))
        {
            return exportFormats.get(type).keySet();
        }
        return new HashSet<>();
    }

    /**
     * Will the mimetype be upgraded if exported to Google Docs?
     *
     * @param mimetype
     * @return
     */
    private boolean isUpgrade(String mimetype)
    {
        return upgradeMappings.containsKey(mimetype);
    }

    /**
     * Will the mimetype be downgraded if exported to Google Docs?
     *
     * @param mimetype
     * @return
     */
    private boolean isDownGrade(String mimetype)
    {
        return downgradeMappings.containsKey(mimetype);
    }

    public String getContentType(NodeRef nodeRef)
    {
        String mimetype = fileFolderService.getFileInfo(nodeRef).getContentData().getMimetype();
        return importFormats.get(mimetype);
    }

    /**
     * @param mimeType Mimetype of the Node
     * @return If the Document must be returned as a different type, returns the new type
     */
    private String validateMimeType(String mimeType)
    {

        if (isDownGrade(mimeType))
        {
            mimeType = downgradeMappings.get(mimeType);
            if (log.isDebugEnabled())
            {
                log.debug("Mimetype will be downgraded to " + mimeType);
            }
        }
        else if (isUpgrade(mimeType))
        {
            mimeType = upgradeMappings.get(mimeType);
            if (log.isDebugEnabled())
            {
                log.debug("Mimetype will be upgraded to " + mimeType);
            }
        }

        return mimeType;
    }

    private GoogleAuthorizationCodeFlow getFlow()
    {
        return new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets,
            SCOPES).setAccessType("offline").setApprovalPrompt("auto").build();
    }

    public Credential getCredential() throws GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException, IOException
    {
        Credential credential = null;

        // OAuth credentials for the current user, if the exist
        OAuth2CredentialsInfo credentialInfo = oauth2CredentialsStoreService
            .getPersonalOAuth2Credentials(REMOTE_SYSTEM);

        if (credentialInfo != null)
        {
            log.debug(
                "OAuth Credentials Exist for " + AuthenticationUtil.getFullyAuthenticatedUser());

            credential = new Credential
                .Builder(BearerToken.authorizationHeaderAccessMethod())
                .setJsonFactory(jsonFactory)
                .setTransport(httpTransport)
                .setClientAuthentication(new ClientParametersAuthentication(
                    clientSecrets.getDetails().getClientId(),
                    clientSecrets.getDetails().getClientSecret()))
                .setTokenServerEncodedUrl(clientSecrets.getDetails().getTokenUri())
                .build();
            credential.setAccessToken(credentialInfo.getOAuthAccessToken())
                      .setRefreshToken(credentialInfo.getOAuthRefreshToken())
                      .setExpirationTimeMilliseconds(
                          credentialInfo.getOAuthTicketExpiresAt().getTime());

            try
            {
                log.debug(
                    "Test oAuth Credentials for " + AuthenticationUtil.getFullyAuthenticatedUser());
                testConnection(credential);
            }
            catch (TokenResponseException e)
            {
                credential = getCredentialAfterRefresh();
            }
        }

        log.debug("Google Docs Credentials Created.");
        return credential;
    }

    private Credential getCredentialAfterRefresh()
        throws GoogleDocsAuthenticationException, IOException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException
    {
        final Credential credential = refreshAccessToken();
        testConnection(credential);
        return credential;
    }

    private void testConnection(Credential credential)
        throws TokenResponseException, GoogleDocsServiceException
    {
        final Oauth2 userInfoService = new Oauth2.Builder(new NetHttpTransport(),
            new JacksonFactory(),
            credential).setApplicationName(APPLICATION_NAME).build();
        final Userinfoplus userInfo;
        try
        {
            userInfo = userInfoService
                .userinfo()
                .get()
                .setFields(ALL_PROPERTY_FIELDS)
                .execute();
        }
        catch (TokenResponseException e)
        {
            //rethrow before it hits IOException (parent object of TokenResponseException
            throw e;
        }
        catch (IOException e)
        {
            throw new GoogleDocsServiceException("Error creating Connection: " + e.getMessage(), e);
        }
        if (userInfo == null || userInfo.getId() == null)
        {
            throw new GoogleDocsServiceException("Error creating Connection: No user");
        }
    }

    private Credential refreshAccessToken() throws GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException, IOException
    {
        log.debug("Refreshing Access Token for " + AuthenticationUtil.getFullyAuthenticatedUser());
        OAuth2CredentialsInfo credentialInfo = oauth2CredentialsStoreService.getPersonalOAuth2Credentials(
            REMOTE_SYSTEM);

        if (credentialInfo.getOAuthRefreshToken() == null)
        {
            throw new GoogleDocsRefreshTokenException(
                "No Refresh Token Provided for " + AuthenticationUtil.getFullyAuthenticatedUser());
        }

        final Credential credential;
        boolean success;
        try
        {
            credential = new Credential.Builder(
                BearerToken.authorizationHeaderAccessMethod()).setJsonFactory(
                jsonFactory).setTransport(httpTransport).setClientAuthentication(
                new ClientParametersAuthentication(clientSecrets.getDetails().getClientId(),
                    clientSecrets.getDetails().getClientSecret())).setTokenServerEncodedUrl(
                clientSecrets.getDetails().getTokenUri()).build();
            credential.setAccessToken(credentialInfo.getOAuthAccessToken()).setRefreshToken(
                credentialInfo.getOAuthRefreshToken()).setExpirationTimeMilliseconds(
                credentialInfo.getOAuthTicketExpiresAt().getTime());

            success = credential.refreshToken();
        }
        catch (GoogleJsonResponseException | TokenResponseException e)
        {
            if (e.getStatusCode() == SC_BAD_REQUEST)
            {
                throw new GoogleDocsAuthenticationException(e.getMessage(), e);
            }
            else if (e.getStatusCode() == SC_UNAUTHORIZED)
            {
                throw new GoogleDocsAuthenticationException("Token Refresh Failed.");
            }
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }

        if (!success)
        {
            throw new GoogleDocsAuthenticationException("No Access Grant Returned.");
        }

        Date expiresIn = null;

        if (credential.getExpirationTimeMilliseconds() != null)
        {
            if (credential.getExpirationTimeMilliseconds() > 0L)
            {
                expiresIn = new Date(
                    new Date().getTime() + credential.getExpirationTimeMilliseconds());
            }
        }

        oauth2CredentialsStoreService.storePersonalOAuth2Credentials(REMOTE_SYSTEM,
            credential.getAccessToken(), credential.getRefreshToken(), expiresIn,
            new Date());

        log.debug("Access Token Refreshed");
        return credential;
    }

    private Drive getDriveApiWithCredentialCheck(Credential credential)
        throws GoogleDocsAuthenticationException, GoogleDocsServiceException, IOException,
        GoogleDocsRefreshTokenException
    {
        // Get the users drive credential if not provided;
        credential = credential == null ? getCredential() : credential;
        return getDriveApi(credential);
    }

    private Drive getDriveApi(Credential credential)
    {
        log.debug("Initiating Google Drive Connection");
        return new Drive.Builder(new NetHttpTransport(), new JacksonFactory(),
            null).setHttpRequestInitializer(credential).setApplicationName(
            APPLICATION_NAME).build();
    }

    /**
     * Has the current user authenticated to Google Drive?
     *
     * @return
     */
    public boolean isAuthenticated()
    {
        boolean authenticated = false;

        OAuth2CredentialsInfo credentialInfo = oauth2CredentialsStoreService.getPersonalOAuth2Credentials(
            REMOTE_SYSTEM);

        if (credentialInfo != null)
        {
            try
            {
                getCredential();
                authenticated = true;
            }
            catch (Exception ignored)
            {
            }
        }

        log.debug("Authenticated: " + authenticated);
        return authenticated;
    }

    /**
     * The oauth authentication url
     *
     * @param state the value of the oauth state parameter to be passed in the authentication url
     * @return The complete oauth authentication url
     */
    public String getAuthenticateUrl(String state) throws GoogleDocsServiceException
    {
        String authenticateUrl = null;

        if (state != null)
        {
            GoogleAuthorizationCodeRequestUrl urlBuilder;

            if (isBlank(getGoogleUserName()))
            {
                urlBuilder = getFlow().newAuthorizationUrl().setRedirectUri(
                    getRedirectUri()).setState(state);
            }
            else
            {
                urlBuilder = getFlow().newAuthorizationUrl().setRedirectUri(
                    getRedirectUri()).setState(state).set("login_hint", getGoogleUserName());
            }

            authenticateUrl = urlBuilder.build();
        }

        log.debug("Authentication URL: " + authenticateUrl);
        return authenticateUrl;
    }

    private String getGoogleUserName()
    {
        final NodeRef person = personService.getPerson(AuthenticationUtil.getRunAsUser());
        return (String) nodeService.getProperty(person, PROP_GOOGLEUSERNAME);
    }

    public boolean completeAuthentication(String authorizationCode) throws
        GoogleDocsServiceException, IOException
    {
        GoogleTokenResponse response = getFlow()
            .newTokenRequest(authorizationCode)
            .setRedirectUri(getRedirectUri())
            .execute();

        try
        {
            // If this is a reauth....we may not get back the refresh token. We
            // need to make sure it is persisted across the "refresh".
            if (response.getRefreshToken() == null)
            {
                log.debug("Missing Refresh Token");

                OAuth2CredentialsInfo credentialInfo = oauth2CredentialsStoreService.getPersonalOAuth2Credentials(
                    REMOTE_SYSTEM);
                // In the "rare" case that no refresh token is returned and the
                // users credentials are no longer there we need to skip this
                // next check
                if (credentialInfo != null)
                {
                    // If there is a persisted refresh ticket...add it to the
                    // accessGrant so that it is persisted across the update
                    if (credentialInfo.getOAuthRefreshToken() != null)
                    {
                        response.setRefreshToken(credentialInfo.getOAuthRefreshToken());

                        log.debug("Persisting Refresh Token across reauth");
                    }
                }
            }

            oauth2CredentialsStoreService.storePersonalOAuth2Credentials(REMOTE_SYSTEM,
                response.getAccessToken(), response.getRefreshToken(),
                new Date(response.getExpiresInSeconds()), new Date());

            log.debug("Authentication Complete: " + true);
            return true;
        }
        catch (NoSuchSystemException e)
        {
            throw new GoogleDocsServiceException(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#createDocument(org.alfresco.service.cmr.repository.NodeRef)
     */
    public File createDocument(Credential credential, NodeRef nodeRef) throws
        GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Create Google Document for node " + nodeRef);

        // New file name
        String name = fileFolderService.getFileInfo(nodeRef).getName();

        // To be editable a new document must use the Google Document mimetype.

        // If the node does not have a name, set a default for the type
        if (name == null)
        {
            name = NEW_DOCUMENT_NAME;
        }

        // Create the working Directory
        File workingDir = createWorkingDirectory(credential, nodeRef);

        // Create document on Drive
        File file = createFileOnDrive(credential, workingDir.getId(), name, "", DOCUMENT_MIMETYPE);

        // Add temporary Node (with Content) to repository.
        ContentWriter writer = fileFolderService.getWriter(nodeRef);
        writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING);
        writer.putContent(newDocument.getInputStream());

        return file;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#createSpreadSheet(org.alfresco.service.cmr.repository.NodeRef
     * )
     */
    public File createSpreadSheet(Credential credential, NodeRef nodeRef) throws
        GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Create Google Spreadsheet for node " + nodeRef);

        String name = fileFolderService.getFileInfo(nodeRef).getName();

        // To be editable, a new spreadsheet must use the Google Spreadsheet mimetype.

        // If the node does not have a name, set a default for the type
        if (name == null)
        {
            name = NEW_SPREADSHEET_NAME;
        }

        // Create the working Directory
        File workingDir = createWorkingDirectory(credential, nodeRef);

        // Create the Google Spreadsheet in the working directory
        File file = createFileOnDrive(credential, workingDir.getId(), name, "",
            SPREADSHEET_MIMETYPE);

        // Add temporary Node (with Content) to the repository
        ContentWriter writer = fileFolderService.getWriter(nodeRef);
        writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET);
        writer.putContent(newSpreadsheet.getInputStream());

        return file;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#createPresentation(org.alfresco.service.cmr.repository.NodeRef
     * )
     */
    public File createPresentation(Credential credential, NodeRef nodeRef) throws
        GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Create Google Presentation for node " + nodeRef);

        String name = fileFolderService.getFileInfo(nodeRef).getName();
        // To be editable a new presentation must use the Google Presentation mimetype

        // If the node does not have a name, set a default for the type
        if (name == null)
        {
            name = NEW_PRESENTATION_NAME;
        }

        // Create the working Directory
        File workingDir = createWorkingDirectory(credential, nodeRef);

        // Create the Google Document in the working directory
        File file = createFileOnDrive(credential, workingDir.getId(), name, "",
            PRESENTATION_MIMETYPE);

        // Add temporary Node (with Content) to repository
        ContentWriter writer = fileFolderService.getWriter(nodeRef);
        writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_PRESENTATION);
        writer.putContent(newPresentation.getInputStream());

        return file;
    }

    /**
     * Get the Document from the users Google Drive account. The Document and its working directory will be removed from their
     * Google Drive account. The editingInGoogle aspect will be removed.
     *
     * @param nodeRef
     * @param resourceID
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsRefreshTokenException
     */
    private void getDocument(Credential credential, NodeRef nodeRef, String resourceID,
        boolean removeFromDrive) throws GoogleDocsAuthenticationException,
        GoogleDocsServiceException, IOException, GoogleDocsRefreshTokenException
    {
        log.debug("Get Google Document for node: " + nodeRef);
        getDriveFileContent(credential, nodeRef, resourceID, removeFromDrive);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.alfresco.integrations.google.docs.service.GoogleDocsService#getDocument(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void getDocument(Credential credential, NodeRef nodeRef, boolean removeFromDrive)
        throws GoogleDocsAuthenticationException, GoogleDocsServiceException, IOException,
        GoogleDocsRefreshTokenException
    {
        final String resourceID = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString();
        if (resourceID == null)
        {
            throw new NotInGoogleDriveException(nodeRef);
        }

        getDocument(credential, nodeRef, resourceID.substring(resourceID.lastIndexOf(':') + 1),
            removeFromDrive);
    }

    public void getDocument(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException, IOException,
        GoogleDocsRefreshTokenException
    {
        final String resourceID = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString();
        if (resourceID == null)
        {
            throw new NotInGoogleDriveException(nodeRef);
        }

        getDocument(credential, nodeRef, resourceID.substring(resourceID.lastIndexOf(':') + 1),
            false);
    }

    /**
     * Get the Document from the users Google Drive account. The Document and its working directory will be removed from their
     * Google Drive account. The editingInGoogle aspect will be removed.
     *
     * @param nodeRef
     * @param resourceID
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsRefreshTokenException
     */
    private void getSpreadSheet(Credential credential, NodeRef nodeRef, String resourceID,
        boolean removeFromDrive) throws GoogleDocsAuthenticationException,
        GoogleDocsServiceException, GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Get Google Spreadsheet for node: " + nodeRef);
        getDriveFileContent(credential, nodeRef, resourceID, removeFromDrive);
    }

    private void getDriveFileContent(Credential credential, NodeRef nodeRef, String resourceID,
        boolean removeFromDrive) throws IOException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException
    {
        final Drive drive = getDriveApiWithCredentialCheck(credential);
        try
        {
            final String contentMimetype = fileFolderService.getFileInfo(
                nodeRef).getContentData().getMimetype();
            final String mimetype = validateMimeType(contentMimetype);
            log.debug("Current mimetype: " + contentMimetype
                      + "; Mimetype of Google Doc: " + mimetype);
            log.debug("Export format: " + mimetype);

            final File file = drive
                .files()
                .get(resourceID.substring(resourceID.lastIndexOf(':') + 1))
                .setFields(ALL_PROPERTY_FIELDS)
                .execute();

            final InputStream inputStream = exportGoodleDriveFile(file, mimetype, drive, nodeRef);

            ContentWriter writer = fileFolderService.getWriter(nodeRef);
            writer.setMimetype(mimetype);
            writer.putContent(inputStream);

            renameNode(nodeRef, file.getName());

            saveSharedInfo(credential, nodeRef,
                resourceID.substring(resourceID.lastIndexOf(':') + 1));

            if (removeFromDrive)
            {
                deleteContent(credential, nodeRef, file);
            }
            else
            {
                nodeService.setProperty(nodeRef, PROP_REVISION_ID,
                    getLatestRevision(credential, file).getId());
            }

            postActivity(nodeRef);

            if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
            {
                nodeService.removeAspect(nodeRef, ASPECT_TEMPORARY);
                log.debug("Temporary Aspect Removed");
            }
        }
        catch (GoogleJsonResponseException e)
        {
            log.error("Failed to get drive file content", e);
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }
        catch (JSONException jsonException)
        {
            throw new GoogleDocsAuthenticationException(
                "Unable to create activity entry: " + jsonException.getMessage(), jsonException);
        }
    }

    private InputStream exportGoodleDriveFile(final File file, final String mimetype,
        final Drive drive, final NodeRef nodeRef) throws IOException
    {
        final Object versionType = fileFolderService.getFileInfo(nodeRef).getProperties()
                                                    .get(PROP_VERSION_TYPE);

        final String editorURL = String.valueOf(fileFolderService
            .getFileInfo(nodeRef).getProperties().getOrDefault(PROP_EDITORURL, ""));

        // Different export mechanisms depending on the GD file mimetype (in GD, not Alfresco)
        if (versionType == null ||
            isGoogleDriveMimeType(mimetype) ||
            editorURL.contains("://docs.google.com/"))
        {
            try
            {
                return drive.files().export(file.getId(), mimetype).executeMediaAsInputStream();
            }
            catch (GoogleJsonResponseException e)
            {
                log.info(
                    "Failed to export GoogleDrive document from GD mimetype (retrying): " + e.getMessage());
            }
        }
        return drive.files().get(file.getId()).executeMediaAsInputStream();
    }

    private static boolean isGoogleDriveMimeType(final String mimeType)
    {
        return mimeType != null && mimeType.startsWith("application/vnd.google-apps.");
    }

    public void getSpreadSheet(Credential credential, NodeRef nodeRef, boolean removeFromDrive)
        throws GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException
    {
        final String resourceID = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString();
        if (resourceID == null)
        {
            throw new NotInGoogleDriveException(nodeRef);
        }

        getSpreadSheet(credential, nodeRef, resourceID.substring(resourceID.lastIndexOf(':') + 1),
            removeFromDrive);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#getSpreadSheet(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void getSpreadSheet(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException, IOException,
        GoogleDocsRefreshTokenException
    {
        final String resourceID = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString();
        if (resourceID == null)
        {
            throw new NotInGoogleDriveException(nodeRef);
        }
        getSpreadSheet(credential, nodeRef, resourceID.substring(resourceID.lastIndexOf(':') + 1),
            false);
    }

    /**
     * Get the Document from the users Google Drive account. The Document and its working directory will be removed from their
     * Google Drive account. The editingInGoogle aspect will be removed.
     *
     * @param nodeRef
     * @param resourceID
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsRefreshTokenException
     */
    private void getPresentation(Credential credential, NodeRef nodeRef, String resourceID,
        boolean removeFromDrive) throws GoogleDocsAuthenticationException,
        GoogleDocsServiceException, GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Get Google Presentation for node: " + nodeRef);
        getDriveFileContent(credential, nodeRef, resourceID, removeFromDrive);
    }

    public void getPresentation(Credential credential, NodeRef nodeRef, boolean removeFromDrive)
        throws GoogleDocsAuthenticationException, GoogleDocsServiceException, IOException,
        GoogleDocsRefreshTokenException
    {
        final String resourceID = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString();
        if (resourceID == null)
        {
            throw new NotInGoogleDriveException(nodeRef);
        }

        getPresentation(credential, nodeRef, resourceID.substring(resourceID.lastIndexOf(':') + 1),
            removeFromDrive);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#getPresentation(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void getPresentation(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException, IOException,
        GoogleDocsRefreshTokenException
    {
        final String resourceID = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString();
        if (resourceID == null)
        {
            throw new NotInGoogleDriveException(nodeRef);
        }

        getPresentation(credential, nodeRef, resourceID.substring(resourceID.lastIndexOf(':') + 1),
            false);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.alfresco.integrations.google.docs.service.GoogleDocsService#uploadFile(org.alfresco.service.cmr.repository.NodeRef)
     */
    public File uploadFile(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Upload " + nodeRef + " to Google");
        Drive drive = getDriveApiWithCredentialCheck(credential);

        File file;

        // It makes me want to cry that they don't support inputStreams.
        java.io.File f = null;

        try
        {
            // Get the reader
            ContentReader reader = fileFolderService.getReader(nodeRef);

            f = java.io.File.createTempFile(nodeRef.getId(), ".tmp", TempFileProvider.getTempDir());
            reader.getContent(f);

            // Get the mimetype
            FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
            String mimetype = fileInfo.getContentData().getMimetype();

            // Create the working Directory
            File workingDir = createWorkingDirectory(credential, nodeRef);

            List<String> parents = Collections.singletonList(workingDir.getId());
            file = new File()
                .setParents(parents)
                .setName(fileInfo.getName())
                .setMimeType(mimetype);

            FileContent fileContent = new FileContent(mimetype, f);
            file = drive.files()
                        .create(file, fileContent)
                        .setFields(ALL_PROPERTY_FIELDS)
                        .execute();
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new GoogleDocsServiceException(e.getMessage(), SC_INTERNAL_SERVER_ERROR, e);
        }
        finally
        {
            if (f != null)
            {
                boolean success = f.delete();

                if (!success)
                {
                    log.debug(
                        "The temporary file used to upload to Google Drive was not successfully removed.");
                }
            }
        }

        return file;
    }

    /**
     * Unlock and Undecorate node; Remove content from users Google Account Does not update the content in Alfresco; If content was
     * newly created by GoogleDocsService it will be removed.
     * <p/>
     * Method can be run by owner, admin or site manager
     *
     * @param nodeRef
     * @param file
     * @param forceRemoval ignore <code>GoogleDocsServiceException</code> exceptions when attempting to remove content from user's
     *                     Google account
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsAuthenticationException
     */
    public void removeContent(Credential credential, NodeRef nodeRef, File file,
        boolean forceRemoval) throws GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException
    {
        if (isGoogleDocsLockOwner(nodeRef) || authorityService.hasAdminAuthority()
            || isSiteManager(nodeRef, AuthenticationUtil.getFullyAuthenticatedUser()))
        {
            unlockNode(nodeRef);
            try
            {
                deleteContent(credential, nodeRef, file); // also undecorates node
            }
            catch (GoogleDocsServiceException e)
            {
                if (forceRemoval)
                {
                    log.debug(
                        "There was an error (" + e.getMessage() + ": " + e.getPassedStatusCode() + ") removing "
                        + file.getName() + " from " + AuthenticationUtil.getFullyAuthenticatedUser()
                        + "'s Google Account. Force Removal ignores the error.");
                }
                else
                {
                    throw e;
                }
            }

            if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
            {
                nodeService.deleteNode(nodeRef);
            }
        }
    }

    /**
     * isSiteManager...also handles nodes not found in a site...
     *
     * @param nodeRef
     * @param authorityName
     * @return
     */
    public boolean isSiteManager(NodeRef nodeRef, String authorityName)
    {
        boolean isSiteManager = false;

        SiteInfo siteInfo = filenameUtil.resolveSiteInfo(nodeRef);

        if (siteInfo != null)
        {
            isSiteManager = SiteServiceImpl.SITE_MANAGER.equals(
                siteService.getMembersRole(siteInfo.getShortName(), authorityName));
        }

        return isSiteManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#deleteContent(org.alfresco.service.cmr.repository.NodeRef,
     * org.springframework.social.google.api.drive.DriveFile)
     */
    public boolean deleteContent(Credential credential, NodeRef nodeRef, File file) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Delete Google Doc for " + nodeRef);
        Drive drive = getDriveApiWithCredentialCheck(credential);

        try
        {
            if (file != null)
            {
                drive.files()
                     .delete(file.getId())
                     .setFields(ALL_PROPERTY_FIELDS)
                     .execute();

                // Delete the Working directory in Google Drive (if it exists....this should handle any migration issues)
                deleteWorkingDirectory(credential, nodeRef);
            }

            unDecorateNode(nodeRef);

            log.debug("Deleted: " + true);
            return true;
        }
        catch (GoogleJsonResponseException e)
        {
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }
    }

    public boolean deleteContent(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsServiceException,
        GoogleDocsRefreshTokenException, IOException
    {
        File file = getDriveFile(credential, nodeRef);
        return deleteContent(credential, nodeRef, file);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#getLatestRevision(org.alfresco.service.cmr.repository.NodeRef
     * )
     */
    public Revision getLatestRevision(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        Revision revision = null;
        try
        {
            if (nodeService.getProperty(nodeRef, PROP_RESOURCE_ID) != null)
            {
                File file = new File().setId(
                    nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString());

                revision = getLatestRevision(credential, file);
            }
        }
        catch (GoogleJsonResponseException e)
        {
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }

        return revision;
    }

    public Revision getLatestRevision(Credential credential, File file) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        Drive drive = getDriveApiWithCredentialCheck(credential);
        Revision revision = null;

        try
        {
            RevisionList revisionList = drive.revisions()
                                             .list(file.getId())
                                             .setFields(ALL_PROPERTY_FIELDS)
                                             .execute();
            List<Revision> fileRevisions = revisionList.getRevisions();

            if (fileRevisions != null)
            {
                fileRevisions.sort(new FileRevisionComparator());

                revision = fileRevisions.get(fileRevisions.size() - 1);
            }
        }
        catch (GoogleJsonResponseException e)
        {
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }

        return revision;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#decorateNode(org.alfresco.service.cmr.repository.NodeRef,
     * org.springframework.social.google.api.drive.DriveFile, boolean)
     */
    public void decorateNode(NodeRef nodeRef, File file, boolean newcontent)
    {
        decorateNode(nodeRef, file, null, null, newcontent);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#decorateNode(org.alfresco.service.cmr.repository.NodeRef,
     * org.springframework.social.google.api.drive.DriveFile, org.springframework.social.google.api.drive.FileRevision, boolean)
     */
    public void decorateNode(NodeRef nodeRef, File file, Revision revision, boolean newcontent)
    {
        decorateNode(nodeRef, file, revision, null, newcontent);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#decorateNode(org.alfresco.service.cmr.repository.NodeRef,
     * org.springframework.social.google.api.drive.DriveFile, org.springframework.social.google.api.drive.FileRevision,
     * java.util.List<org.alfresco.integrations.google.docs.service.GoogleDocsService.GooglePermission>, boolean)
     */
    public void decorateNode(NodeRef nodeRef, File file, Revision revision,
        List<GooglePermission> permissions, boolean newcontent)
    {
        log.debug("Add Google Docs Aspect to " + nodeRef);
        behaviourFilter.disableBehaviour(nodeRef);
        try
        {
            if (newcontent)
            {
                // Mark temporary until first save
                nodeService.addAspect(nodeRef, ASPECT_TEMPORARY, null);
                log.debug("Add Temporary Aspect");
            }

            // Get the googleMetadata to reference the Node
            Map<QName, Serializable> aspectProperties = new HashMap<>();
            aspectProperties.put(PROP_CURRENT_PERMISSIONS,
                buildPermissionsPropertyValue(permissions));
            aspectProperties.put(PROP_RESOURCE_ID, file.getId());
            aspectProperties.put(PROP_EDITORURL, convertWebViewToEditUrl(nodeRef, file.getWebViewLink()));
            aspectProperties.put(PROP_DRIVE_WORKING_FOLDER, file.getParents().get(0));
            if (revision != null)
            {
                aspectProperties.put(PROP_REVISION_ID, revision.getId());
            }
            if (!nodeService.hasAspect(nodeRef, ASPECT_EDITING_IN_GOOGLE))
            {
                nodeService.addAspect(nodeRef, ASPECT_EDITING_IN_GOOGLE, aspectProperties);
            }
            else
            {
                for (Map.Entry<QName, Serializable> prop : aspectProperties.entrySet())
                {
                    nodeService.setProperty(nodeRef, prop.getKey(), prop.getValue());
                }
            }
            log.debug("Resource Id: " + aspectProperties.get(PROP_RESOURCE_ID));
            log.debug("Editor Url:" + aspectProperties.get(PROP_EDITORURL));
            log.debug("Revision Id: "
                      + ((revision != null) ? aspectProperties.get(PROP_REVISION_ID)
                                            : "No file revision provided"));
        }
        finally
        {
            behaviourFilter.enableBehaviour(nodeRef);
        }
    }

    private final static String URL_SEGMEMT_FILE_D = "/file/d/";
    private final static String URL_SEGMEMT_VIEW = "/view";
    private final static String URL_SEGMEMT_EDIT = "/edit";

    private final static String URL_DOCS_DOCUMENT     = "https://docs.google.com/document/d/";
    private final static String URL_DOCS_SPREADSHEET  = "https://docs.google.com/spreadsheets/d/";
    private final static String URL_DOCS_PRESENTATION = "https://docs.google.com/presentation/d/";

    //
    // Convert web view (preview) url to an edit url
    //
    // For example from:
    //      https://drive.google.com/a/alfresco.com/file/d/1ARe2I4tC2k33PXItOJWKfVEQaetf_F91/view?usp=drivesdk
    // to:
    //      https://docs.google.com/spreadsheets/d/1flbjW8a9fI56dPUsgb6_CoiAyNJxliHmVrAuK7KnRM4/edit
    //
    // Note: this may need to be updated in future (eg. if URL format changes)
    //
    public String convertWebViewToEditUrl(NodeRef nodeRef, String url)
    {
        if (url != null)
        {
            int idx1 = url.indexOf(URL_SEGMEMT_FILE_D);
            int idx2 = url.indexOf(URL_SEGMEMT_VIEW);

            if ((idx1 != -1) && (idx2 != -1))
            {
                ContentData contentData = (ContentData)nodeService.getProperty(nodeRef, PROP_CONTENT);
                String mimeType = contentData.getMimetype();

                if (mimeType == null)
                {
                    throw new RuntimeException("Content Type is null");
                }

                // extract id
                String docId = url.substring(idx1 + URL_SEGMEMT_FILE_D.length(), idx2);

                switch (mimeType)
                {
                    case MIMETYPE_DOCUMENT:
                        url = URL_DOCS_DOCUMENT + docId + URL_SEGMEMT_EDIT;
                        break;
                    case MIMETYPE_SPREADSHEET:
                        url = URL_DOCS_SPREADSHEET + docId + URL_SEGMEMT_EDIT;
                        break;
                    case MIMETYPE_PRESENTATION:
                        url = URL_DOCS_PRESENTATION + docId +URL_SEGMEMT_EDIT;
                        break;
                    default:
                        throw new RuntimeException("Unknown Content Type: " + mimeType);
                }
            }
        }

        return url;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.alfresco.integrations.google.docs.service.GoogleDocsService#unDecorateNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void unDecorateNode(NodeRef nodeRef)
    {
        log.debug("Remove Google Docs aspect from " + nodeRef);
        behaviourFilter.disableBehaviour(nodeRef);
        try
        {
            if (nodeService.hasAspect(nodeRef, ASPECT_EDITING_IN_GOOGLE))
            {
                nodeService.removeAspect(nodeRef, ASPECT_EDITING_IN_GOOGLE);
            }
        }
        finally
        {
            behaviourFilter.enableBehaviour(nodeRef);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.alfresco.integrations.google.docs.service.GoogleDocsService#lockNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void lockNode(NodeRef nodeRef)
    {
        if (nodeService.getProperty(nodeRef, PROP_LOCKED) == null ||
            Boolean.FALSE.equals(nodeService.getProperty(nodeRef, PROP_LOCKED)))
        {
            log.debug("Lock Node " + nodeRef + " for Google Docs Editing");
            behaviourFilter.disableBehaviour(nodeRef);
            try
            {
                nodeService.setProperty(nodeRef, PROP_LOCKED, true);
                lockservice.lock(nodeRef, LockType.READ_ONLY_LOCK);
            }
            finally
            {
                behaviourFilter.enableBehaviour(nodeRef);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.alfresco.integrations.google.docs.service.GoogleDocsService#unlockNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void unlockNode(NodeRef nodeRef)
    {
        log.debug("Unlock Node " + nodeRef + " from Google Docs Editing");
        behaviourFilter.disableBehaviour(nodeRef);
        try
        {
            lockservice.unlock(nodeRef);
            nodeService.setProperty(nodeRef, PROP_LOCKED, false);
        }
        finally
        {
            behaviourFilter.enableBehaviour(nodeRef);
        }
    }

    /**
     * Is the node locked by Googledocs? If the document is marked locked in the model, but not locked in the repository, the locked
     * property is set to false
     *
     * @param nodeRef
     * @return
     */
    public boolean isLockedByGoogleDocs(NodeRef nodeRef)
    {

        boolean locked = false;
        Boolean isNodeLocked = (Boolean) nodeService.getProperty(nodeRef, PROP_LOCKED);

        if (isNodeLocked != null && isNodeLocked)
        {
            LockStatus lockStatus = lockservice.getLockStatus(nodeRef);
            if (lockStatus.equals(LockStatus.NO_LOCK))
            {
                // fix broken lock
                behaviourFilter.disableBehaviour(nodeRef);
                nodeService.setProperty(nodeRef, PROP_LOCKED, false);
                behaviourFilter.enableBehaviour(nodeRef);
            }
            else
            {
                locked = true;
            }
        }

        log.debug("Node " + nodeRef + " locked by Google Docs");

        return locked;
    }

    /**
     * @param nodeRef
     * @return Will return false is the document is not locked
     */
    public boolean isGoogleDocsLockOwner(NodeRef nodeRef)
    {
        boolean isOwner = false;

        if (isLockedByGoogleDocs(nodeRef))
        {
            LockStatus lockStatus = lockservice.getLockStatus(nodeRef);
            if (lockStatus.equals(LockStatus.LOCK_OWNER))
            {
                isOwner = true;
            }
        }

        return isOwner;
    }

    /**
     * @param nodeRef
     * @return Return the Google Docs Lock Owner
     */
    public String getGoogleDocsLockOwner(NodeRef nodeRef)
    {
        String lockOwner = null;

        if (isLockedByGoogleDocs(nodeRef))
        {
            LockState lockState = lockservice.getLockState(nodeRef);

            lockOwner = lockState.getOwner();
        }

        return lockOwner;
    }

    /**
     * Find nodes using duplicate name in same context (folder/space).
     *
     * @param nodeRef
     * @param name    if null, name will be pulled from nodeRef
     * @return
     */
    private NodeRef findLastDuplicate(NodeRef nodeRef, String name)
    {
        NodeRef lastDup = null;

        List<Pair<QName, Boolean>> sortProps = new ArrayList<>(1);
        sortProps.add(new Pair<>(PROP_NAME, false));

        if (name == null)
        {
            name = fileFolderService.getFileInfo(nodeRef).getName();
        }

        PagingResults<FileInfo> results = fileFolderService.list(
            nodeService.getPrimaryParent(nodeRef).getParentRef(), true, false,
            addWildCardInName(name,
                fileFolderService.getFileInfo(nodeRef).getContentData().getMimetype()), null,
            sortProps, new PagingRequest(CannedQueryPageDetails.DEFAULT_PAGE_SIZE));

        List<FileInfo> page = results.getPage();
        FileInfo fileInfo;
        if (page.size() > 0)
        {
            fileInfo = page.get(0);
            lastDup = fileInfo.getNodeRef();
        }

        log.debug(
            "NodeRef of most recent duplicate named file: " + (lastDup != null ? lastDup : " no duplicate named files"));
        return lastDup;
    }

    /**
     * Insert wildcard '*' into filename between name and extension
     *
     * @param name
     * @param mimetype
     * @return
     */
    private String addWildCardInName(String name, String mimetype)
    {
        String extension = mimetypeService.getExtension(mimetype);
        return name.substring(0, name.length() - (extension.length() + 1)).concat("*." + extension);
    }

    /**
     * When the file format has changed or a new document is created we need to either change the extension or add an extension
     *
     * @param name
     * @param office2007Pattern
     * @param office1997Pattern
     * @param office2007extension
     * @return
     */
    private String MSofficeExtensionHandler(String name, String office2007Pattern,
        String office1997Pattern,
        String office2007extension)
    {
        Pattern pattern = Pattern.compile(office1997Pattern,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(name);

        if (matcher.find())
        {
            //append the x needed in the filename
            name = name.concat("x");
        }
        else
        {
            Pattern _pattern = Pattern.compile(office2007Pattern,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher _matcher = _pattern.matcher(name);

            if (!_matcher.find())
            {
                name = name.concat(office2007extension);
            }
        }

        return name;
    }

    /**
     * Modify the file extension if the file mimetype has changed. If the name was changed while being edited in google docs update
     * the name in Alfresco. If the name is already in use in the current folder, append -{number} to the name or if it already has
     * a -{number} increment the number for the new file
     *
     * @param nodeRef
     * @param name    New name
     */
    private void renameNode(NodeRef nodeRef, String name)
        throws ConstraintException
    {
        // First, is the file name valid?
        ConstraintDefinition filenameConstraintDef = dictionaryService.getConstraint(
            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "filename"));
        filenameConstraintDef.getConstraint().evaluate(name);

        // Not all file types can be round-tripped. This should correct
        // extensions on files where the format is modified or add an extension
        // to file types where there is no extension
        FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
        String mimetype = fileInfo.getContentData().getMimetype();

        switch (mimetype)
        {
        case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            name = MSofficeExtensionHandler(name, "\\.docx$", "\\.doc$", ".docx");
            break;
        case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
            name = MSofficeExtensionHandler(name, "\\.xlsx$", "\\.xls$", ".xlsx");
            break;
        case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
            name = MSofficeExtensionHandler(name, "\\.pptx$", "\\.ppt$", ".pptx");
            break;
        case "application/vnd.oasis.opendocument.text":
            Pattern odt_pattern = Pattern.compile("\\.odt$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher odt_matcher = odt_pattern.matcher(name);

            if (!odt_matcher.find())
            {
                Pattern sxw_pattern = Pattern.compile("\\.sxw$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                Matcher sxw_matcher = sxw_pattern.matcher(name);

                if (sxw_matcher.find())
                {
                    name = name.substring(0, name.length() - 4);
                    name = name.concat(".odt");
                }
            }
            break;
        default:
            String guessedMimetype = mimetypeService.guessMimetype(name);
            if (!mimetype.equals(guessedMimetype))
            {
                String oldName = name, mimeTypeExtension = mimetypeService.getExtension(mimetype);
                name = name.concat("." + mimeTypeExtension);
                if (log.isInfoEnabled())
                {
                    log.info("Rename file '" + oldName + "' to '" + name + "'");
                }
            }
            break;
        }

        // Get the last known node with the same name (+number) in the same folder
        NodeRef lastDup = findLastDuplicate(nodeRef, name);

        if (lastDup != null)
        {
            // if it is not the same file increment (or add number to) the filename
            if (!lastDup.equals(fileInfo.getNodeRef()))
            {
                name = filenameUtil.incrementFileName(
                    fileFolderService.getFileInfo(lastDup).getName(),
                    fileInfo.getContentData().getMimetype());
            }
        }

        // If there is no change in the name we don't want to make a change in
        // the repo
        if (!fileInfo.getName().equals(name))
        {
            nodeService.setProperty(nodeRef, PROP_NAME, name);
        }
    }

    public boolean hasConcurrentEditors(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        log.debug(
            "Check for Concurrent Editors (Edits that have occured in the last " + idleThreshold + " seconds)");
        Drive drive = getDriveApiWithCredentialCheck(credential);
        boolean concurrentChange = false;

        if (!nodeService.hasAspect(nodeRef, ASPECT_EDITING_IN_GOOGLE))
        {
            throw new AspectMissingException(ASPECT_EDITING_IN_GOOGLE, nodeRef);
        }

        final String resourceID = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID).toString();
        try
        {
            RevisionList revisionList = drive
                .revisions()
                .list(resourceID.substring(resourceID.lastIndexOf(':') + 1))
                .setFields(ALL_PROPERTY_FIELDS)
                .execute();
            List<Revision> revisions = revisionList.getRevisions();

            if (revisions.size() > 1)
            {

                log.debug("Revisions Found");
                revisions.sort(Collections.reverseOrder(new FileRevisionComparator()));

                // Find any revisions occurring within the last 'idleThreshold'
                // seconds
                List<Revision> workingList = new ArrayList<>();

                Calendar bufferTime = Calendar.getInstance();
                bufferTime.add(Calendar.SECOND, -idleThreshold);

                for (Revision entry : revisions)
                {
                    Date d = new Date(entry.getModifiedTime().getValue());
                    if (d.after(new Date(bufferTime.getTimeInMillis())))
                    {
                        workingList.add(entry);
                    }
                    else
                    {
                        // once we past 'idleThreshold' seconds get out of here
                        break;
                    }
                }

                // If there any revisions that occurred within the last
                // 'idleThreshold' seconds of time....
                if (workingList.size() > 0)
                {
                    log.debug("Revisions within threshhold found");
                    // Filter the current user from the list
                    for (int i = workingList.size() - 1; i >= 0; i--)
                    {
                        Revision revision = workingList.get(i);
                        String emailAddress = getDriveUser(credential).getEmailAddress();

                        // if there is no author -- the entry is the initial
                        // creation
                        if (revision.getLastModifyingUser() != null &&
                            revision.getLastModifyingUser().getEmailAddress() != null)
                        {
                            if (revision.getLastModifyingUser().getEmailAddress().equals(
                                emailAddress))
                            {
                                workingList.remove(i);
                            }
                        }
                        else
                        {
                            workingList.remove(i);
                        }
                    }
                }

                // Are there are changes by other users within the last
                // 'idleThreshold' seconds
                if (workingList.size() > 0)
                {
                    log.debug("Revisions not made by current user found.");
                    concurrentChange = true;
                }
            }
            else
            {
                String emailAddress = getDriveUser(credential).getEmailAddress();

                // if the authors list is empty -- the author was the original
                // creator and it is the initial copy
                if (revisions.get(0).getLastModifyingUser() != null &&
                    revisions.get(0).getLastModifyingUser().getEmailAddress() != null)
                {

                    if (!revisions.get(0).getLastModifyingUser().getEmailAddress().equals(
                        emailAddress))
                    {
                        Calendar bufferTime = Calendar.getInstance();
                        bufferTime.add(Calendar.SECOND, -idleThreshold);

                        Date dt = new Date(revisions.get(0).getModifiedTime().getValue());
                        if (dt.before(new Date(bufferTime.getTimeInMillis())))
                        {
                            log.debug("Revisions not made by current user found.");
                            concurrentChange = true;
                        }
                    }
                }
            }
        }
        catch (GoogleJsonResponseException e)
        {
            //GOOGLEDOC-326 - need to handle case where 500 is returned but it actually maybe (or should be) a 404
            if (SC_INTERNAL_SERVER_ERROR == e.getStatusCode())
            {
                File file = getDriveFile(credential, nodeRef);

                if (file == null)
                {
                    throw new GoogleDocsServiceException(
                        "Unable to retrived Revisions. The file can no longer be found in Drive.",
                        SC_NOT_FOUND, e);
                }
            }

            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }

        log.debug("Concurrent Edits: " + concurrentChange);
        return concurrentChange;
    }

    public File getDriveFile(Credential credential, String resourceID) throws
        GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException
    {
        log.debug("Get Document list entry for resource " + resourceID.substring(
            resourceID.lastIndexOf(':') + 1));
        Drive drive = getDriveApiWithCredentialCheck(credential);
        try
        {
            return drive.files()
                        .get(resourceID.substring(resourceID.lastIndexOf(':') + 1))
                        .setFields(ALL_PROPERTY_FIELDS)
                        .execute();
        }
        catch (GoogleJsonResponseException e)
        {
            return null;
        }
    }

    public File getDriveFile(Credential credential, NodeRef nodeRef) throws
        GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException
    {
        final Object resourceIdProperty = nodeService.getProperty(nodeRef, PROP_RESOURCE_ID);
        if (resourceIdProperty == null)
        {
            throw new NotInGoogleDriveException(nodeRef);
        }
        final String resourceID = resourceIdProperty.toString();
        log.debug("Node " + nodeRef + " maps to Resource ID " + resourceID.substring(
            resourceID.lastIndexOf(':') + 1));

        return getDriveFile(credential, resourceID.substring(resourceID.lastIndexOf(':') + 1));
    }

    public User getDriveUser(Credential credential) throws GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException, IOException
    {
        log.debug("Get Google Docs user metadata");

        final Drive drive = getDriveApiWithCredentialCheck(credential);

        try
        {
            final About about = drive.about().get().setFields(ALL_PROPERTY_FIELDS).execute();
            return about.getUser();
        }
        catch (GoogleJsonResponseException e)
        {
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }
    }

    private void postActivity(NodeRef nodeRef) throws JSONException
    {
        log.debug("Create Activity Stream Entry");
        if (!personService.personExists(AuthenticationUtil.getRunAsUser()))
        {
            log.debug("Activity stream entry not created -- user does not exist.");
            return;
        }

        SiteInfo siteInfo = null;
        String pathElement = getPathElement(nodeRef, 2);

        //Is the node in a site?
        if (pathElement.equals(ALF_SITES_PATH_FQNS_ELEMENT))
        {
            siteInfo = filenameUtil.resolveSiteInfo(nodeRef);
        }

        //If this is not in a site following current behaviour we will not updated the activity stream
        if (siteInfo == null)
        {
            log.debug("Activity stream entry not created -- node is not inside a site.");
            return;
        }

        String activityType = FILE_UPDATED;
        if (nodeService.hasAspect(nodeRef, ASPECT_TEMPORARY))
        {
            activityType = FILE_ADDED;
        }

        String siteId = siteInfo.getShortName();

        JSONObject jsonActivityData = new JSONObject();

        PersonInfo personInfo = personService.getPerson(
            personService.getPerson(AuthenticationUtil.getRunAsUser(), false));

        jsonActivityData.put("firstName", personInfo.getFirstName());
        jsonActivityData.put("lastName", personInfo.getLastName());
        jsonActivityData.put("title", fileFolderService.getFileInfo(nodeRef).getName());
        jsonActivityData.put("page", "document-details?nodeRef=" + nodeRef.toString());
        jsonActivityData.put("nodeRef", nodeRef.toString());

        if (AuthenticationUtil.isMtEnabled())
        {
            // MT share - add tenantDomain
            jsonActivityData.put("tenantDomain", tenantService.getCurrentUserDomain());
        }

        activityService.postActivity(activityType, siteId,
            GoogleDocsService.class.getSimpleName(), jsonActivityData.toString());
        log.debug(
            "Post Activity Stream Entry -- type:" + activityType + "; site: " + siteId + "; Data: "
            + jsonActivityData);
    }

    /**
     * Retrieve the file's ACL from Google and return a list of users who are listed in the ACL along with their roles.
     *
     * @param resourceId Identifier for the file on Google
     * @return Map where each represents a username and the value the role name
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     * @throws IOException
     */
    private List<GooglePermission> getFilePermissions(Credential credential,
        String resourceId) throws GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException, IOException
    {
        Drive drive = getDriveApiWithCredentialCheck(credential);
        List<GooglePermission> permissionsMap = new ArrayList<>();

        if (log.isDebugEnabled())
        {
            log.error("Looking up Google user profile");
        }
        User user = getDriveUser(credential);
        log.debug("Fetching permissions for file with resource ID " + resourceId);
        PermissionList permissionList = drive.permissions()
                                             .list(resourceId.substring(
                                                 resourceId.lastIndexOf(':') + 1))
                                             .setFields(ALL_PROPERTY_FIELDS)
                                             .execute();

        for (Permission permission : permissionList.getPermissions())
        {
            String role = permission.getRole(), scope = permission.getEmailAddress();
            String type = permission.getType();

            if (GooglePermission.role.reader.toString().equals(role))
            {
                role = GooglePermission.role.commenter.toString();
            }

            if (GooglePermission.role.owner.toString().equals(
                role) && user.getEmailAddress().equals(scope))
            {
                if (log.isDebugEnabled())
                {
                    log.debug(
                        "Skipping permission for owner '" + scope + "' (" + type + ") as '" + role
                        + "' which is implicit");
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(
                        "Adding permission for '" + scope + "' (" + type + ") as '" + role + "'");
                }
                // Include the scope (authority identifier, e.g. email address), type (e.g. "user") and role (e.g. "owner")
                // Store the type lowercase for consistency with the Drive API v1.0 permission entity
                // (https://developers.google.com/drive/v2/reference/permissions)
                permissionsMap.add(new GooglePermission(scope, type, role));
            }
        }

        return permissionsMap;
    }

    /**
     * Look up information from Google that describes the current state of the document, and store this into the repository.
     * <p/>
     * <p>
     * It is intended that this should be called prior to deleting the document from Google, and allows the state to be re-applied
     * if the content is subsequently edited again in Google.
     * </p>
     * <p/>
     * <p>
     * At present this stores only information on which Google users were explicitly listed as collaborators on the document.
     * </p>
     *
     * @param nodeRef    Noderef identifying the file in the repository
     * @param resourceId Identifier for the file on Google
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     * @throws IOException
     */
    private void saveSharedInfo(Credential credential, NodeRef nodeRef, String resourceId) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        List<GooglePermission> permissionsMap = getFilePermissions(credential,
            resourceId.substring(resourceId.lastIndexOf(':') + 1));
        Serializable permissionsList = buildPermissionsPropertyValue(permissionsMap);
        Map<QName, Serializable> aspectProperties = new HashMap<>();
        aspectProperties.put(PROP_PERMISSIONS, permissionsList);
        log.debug("File permissions: " + permissionsList);

        behaviourFilter.disableBehaviour(nodeRef);
        try
        {
            if (nodeService.hasAspect(nodeRef, ASPECT_SHARED_IN_GOOGLE))
            {
                log.debug("Updating Shared Google Docs permissions on " + nodeRef);
                nodeService.setProperty(nodeRef, PROP_PERMISSIONS, permissionsList);
            }
            else
            {
                log.debug("Adding Shared Google Docs aspect to " + nodeRef);
                nodeService.addAspect(nodeRef, ASPECT_SHARED_IN_GOOGLE, aspectProperties);
            }
        }
        finally
        {
            behaviourFilter.enableBehaviour(nodeRef);
        }
    }

    public Serializable buildPermissionsPropertyValue(List<GooglePermission> permissions)
    {
        if (permissions == null)
        {
            return null;
        }

        return permissions
            .stream()
            .map(p -> p.getAuthorityType() + "|" + p.getAuthorityId() + "|" + p.getRoleName())
            .collect(toCollection(() -> new ArrayList<>(permissions.size())));
    }

    /**
     * List the saved Google permissions currently stored for this object.
     *
     * @param nodeRef Noderef identifying the file in the repository
     * @return A list of permissions objects stored for this node, which may be an empty list, or null if nothing is stored
     */
    public List<GooglePermission> getGooglePermissions(NodeRef nodeRef, QName qName)
    {
        if (log.isDebugEnabled())
        {
            log.error("Loading Google permissions for " + nodeRef);
        }
        final List<String> propVals = (List<String>) nodeService.getProperty(nodeRef, qName);
        if (propVals == null)
        {
            if (log.isDebugEnabled())
            {
                log.error("No Google permissions found for " + nodeRef);
            }
            return null;
        }

        final List<GooglePermission> permissions = new ArrayList<>(propVals.size());
        for (String val : propVals)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.error("Adding Google permission '" + val + "' for " + nodeRef);
                }
                permissions.add(GooglePermission.fromString(val));
            }
            catch (IllegalArgumentException e)
            {
                log.error("Skipping bad permission '" + val + "'");
            }
        }
        return permissions;
    }

    public void addRemotePermissions(Credential credential, File file,
        List<GooglePermission> permissions) throws GoogleDocsAuthenticationException,
        GoogleDocsServiceException, GoogleDocsRefreshTokenException, IOException
    {
        Drive drive = getDriveApiWithCredentialCheck(credential);

        if (log.isDebugEnabled())
        {
            log.debug("Adding permissions on item " + file.getId() + " in Google");
        }

        for (GooglePermission p : permissions)
        {
            String roleName = p.getRoleName(), authorityType = p.getAuthorityType();
            String role;
            String type;

            if (roleName.equals(GooglePermission.role.reader.toString()))
            {
                role = GooglePermission.role.reader.toString();
            }
            else if (roleName.equals(GooglePermission.role.writer.toString()))
            {
                role = GooglePermission.role.writer.toString();
            }
            else if (roleName.equals(GooglePermission.role.owner.toString()))
            {
                role = GooglePermission.role.owner.toString();
            }
            else if (roleName.equals(GooglePermission.role.commenter.toString()))
            {
                role = GooglePermission.role.commenter.toString();
            }
            else
            {
                throw new IllegalArgumentException("Bad permission role " + roleName);
            }

            if (authorityType.equals(GooglePermission.type.user.toString()))
            {
                type = GooglePermission.type.user.toString();
            }
            else if (authorityType.equals(GooglePermission.type.group.toString()))
            {
                type = GooglePermission.type.group.toString();
            }
            else if (authorityType.equals(GooglePermission.type.domain.toString()))
            {
                type = GooglePermission.type.domain.toString();
            }
            else if (authorityType.equals(GooglePermission.type.anyone.toString()))
            {
                type = GooglePermission.type.anyone.toString();
            }
            else
            {
                throw new IllegalArgumentException("Bad permission type " + authorityType);
            }
            if (log.isDebugEnabled())
            {
                log.debug(
                    "Adding permission " + role + " for " + type + " " + p.getAuthorityId() + "");
            }

            drive.permissions()
                 .create(file.getId(), new Permission().setRole(role).setType(type).setEmailAddress(
                     p.getAuthorityId()))
                 .setFields(ALL_PROPERTY_FIELDS)
                 .execute();
        }
    }

    private File createWorkingDirectory(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        File file = null;

        // Get or create the parent folder
        if (googleDriveFolderExists(credential, ROOT_FOLDER_ID, ALF_TEMP_FOLDER))
        {
            List<File> files = getFolder(credential, ROOT_FOLDER_ID, ALF_TEMP_FOLDER);

            // Look for our description if there is more than one file returned
            if (!files.isEmpty() && files.size() > 1)
            {
                for (File f : files)
                {
                    if (ALF_TEMP_FOLDER_DESC.equals(f.getDescription()))
                    {
                        file = f;
                        break;
                    }
                }

                if (file == null)
                {
                    file = createFolder(credential, ROOT_FOLDER_ID, ALF_TEMP_FOLDER,
                        ALF_TEMP_FOLDER_DESC);
                }
            }
            else if (files.size() == 1)
            {
                file = files.get(0);
            }
        }
        else
        {
            file = createFolder(credential, ROOT_FOLDER_ID, ALF_TEMP_FOLDER, ALF_TEMP_FOLDER_DESC);
        }

        // create working directory
        String folderName = null;
        String pathElement = getPathElement(nodeRef, 2);
        SiteInfo siteInfo;

        //Is the node located under a site?
        if (pathElement.equals(ALF_SITES_PATH_FQNS_ELEMENT))
        {
            siteInfo = filenameUtil.resolveSiteInfo(nodeRef);
            if (siteInfo != null)
            {
                folderName = siteInfo.getShortName();
            }
        }
        else
        {
            //is it in the shared folder path?
            if (pathElement.equals(ALF_SHARED_PATH_FQNS_ELEMENT))
            {
                folderName = ALF_SHARED_FILES_FOLDER;
            }
            else
            {
                //If it is not in a site or in Shared Files it is pulled My Files.
                folderName = ALF_MY_FILES_FOLDER;
            }
        }

        //If the folder name is not set (GOOGLEDOCS-301) place it directly the working directory
        if (folderName != null && file != null)
        {
            file = createFolder(credential, file.getId(), folderName, null);
        }

        return file;
    }

    private String getPathElement(NodeRef nodeRef, int position)
    {
        Path path = nodeService.getPath(nodeRef);
        Path.Element element = path.get(position);
        return element.toString();
    }

    private void deleteWorkingDirectory(Credential credential, NodeRef nodeRef) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        if (nodeService.getProperty(nodeRef, PROP_DRIVE_WORKING_FOLDER) != null &&
            isNotBlank(nodeService.getProperty(nodeRef, PROP_DRIVE_WORKING_FOLDER).toString()))
        {
            String id = nodeService.getProperty(nodeRef, PROP_DRIVE_WORKING_FOLDER).toString();
            deleteFolder(credential, id);
        }
    }

    /**
     * Does a folder with the name and in the parent folder exist. (Note: there may be more than one)
     *
     * @param parentId
     * @param folderName
     * @return
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     */
    private boolean googleDriveFolderExists(Credential credential, String parentId,
        String folderName) throws GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, GoogleDocsServiceException, IOException
    {
        List<File> files = getFolder(credential, parentId, folderName);
        return !files.isEmpty();
    }

    /**
     * Create new folder in Google Drive
     *
     * @param parentId
     * @param folderName
     * @return
     * @throws GoogleDocsServiceException
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     */
    private File createFolder(Credential credential, String parentId, String folderName,
        String description) throws GoogleDocsServiceException, GoogleDocsAuthenticationException,
        GoogleDocsRefreshTokenException, IOException
    {
        return createFileOnDrive(credential, parentId, folderName,
            description == null ? "" : description, FOLDER_MIMETYPE);
    }

    private File createFileOnDrive(Credential credential, String parentId, String fileName,
        String description, String mimeType) throws GoogleDocsServiceException, IOException,
        GoogleDocsRefreshTokenException, GoogleDocsAuthenticationException
    {
        Drive drive = getDriveApiWithCredentialCheck(credential);
        try
        {
            final List<String> parents = Collections.singletonList(parentId);
            File file = new File()
                .setName(fileName)
                .setDescription(description)
                .setMimeType(mimeType)
                .setParents(parents);

            return drive.files()
                        .create(file)
                        .setFields(ALL_PROPERTY_FIELDS)
                        .execute();
        }
        catch (GoogleJsonResponseException e)
        {
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }
    }

    private List<File> getFolder(Credential credential, String parentId, String folderName)
        throws GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        Drive drive = getDriveApiWithCredentialCheck(credential);
        List<File> files = new ArrayList<>();
        FileList fileList = null;

        String query = "name = '" + folderName + "' and mimeType = '" + FOLDER_MIMETYPE +
                       "' and '" + parentId + "' in parents";
        log.debug("Get folder query string: " + query);
        Drive.Files.List request = drive.files().list().setQ(query);
        try
        {
            do
            {
                if (fileList == null)
                {
                    fileList = request.execute();
                }
                else
                {
                    fileList = request.setPageToken(fileList.getNextPageToken()).execute();
                }

                List<File> childfolders = fileList.getFiles();
                if (childfolders != null && !childfolders.isEmpty())
                {
                    files.addAll(childfolders);
                }
            }
            while (fileList.getNextPageToken() != null && fileList.getNextPageToken().length() > 0);
        }
        catch (GoogleJsonResponseException e)
        {
            throw new GoogleDocsServiceException(e.getMessage(), e.getStatusCode(), e);
        }

        return files;
    }

    /**
     * Delete Google Drive Folder
     *
     * @param folderId
     * @throws GoogleDocsAuthenticationException
     * @throws GoogleDocsRefreshTokenException
     * @throws GoogleDocsServiceException
     */
    private void deleteFolder(Credential credential, String folderId) throws
        GoogleDocsAuthenticationException, GoogleDocsRefreshTokenException,
        GoogleDocsServiceException, IOException
    {
        Drive drive = getDriveApiWithCredentialCheck(credential);
        try
        {
            drive.files()
                 .delete(folderId)
                 .setFields(ALL_PROPERTY_FIELDS)
                 .execute();
        }
        catch (GoogleJsonResponseException e)
        {
            if (SC_NOT_FOUND == e.getStatusCode())
            {
                log.debug("Directory not found in Google Drive. This is not a fatal issue.");
            }
            else if (SC_FORBIDDEN == e.getStatusCode())
            {
                if (e.getMessage().equals(GOOGLE_ERROR_UNMUTABLE))
                {
                    log.debug("Unable to delete remote file. Google claims it is unmutable.");
                }
            }
            else
            {
                log.debug(
                    "Google has reported an issue deleting the folder.  This is not a fatal issue. " + e.getDetails());
            }
        }
    }

    /**
     * Validate that client secret is valid json
     *
     * @param clientSecret
     * @return
     */
    private boolean validateClientSecret(String clientSecret) throws GoogleDocsServiceException
    {
        try
        {
            if (isNotBlank(clientSecret))
            {
                //String escaped = JSONObject.quote(clientSecret);
                new JSONObject(clientSecret.trim());
                return true;
            }
        }
        catch (JSONException e)
        {
            log.debug("Client Secret is not valid json. " + e.getMessage());
            throw new GoogleDocsServiceException("Invalid Client Secret.");
        }

        return false;
    }

    private String getRedirectUri() throws GoogleDocsServiceException
    {
        if (!isNotBlank(clientSecret))
        {
            return REDIRECT_URI;
        }
        try
        {
            JSONObject json = new JSONObject(clientSecret);

            return String.valueOf(json.getJSONObject(CLIENT_SECRET_WEB).getJSONArray(
                CLIENT_SECRET_REDIRECT_URIS).get(0));
        }
        catch (JSONException e)
        {
            log.debug("Unable to parse the Client Secret. " + e.getMessage());
            throw new GoogleDocsServiceException(e);
        }
    }
}
