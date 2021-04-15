Alfresco Google Docs Integration / Module
=========================================

Description
-----------

This extension adds the ability to edit supported content items in Google Docs&trade; to the Alfresco repository and Share interface. See also the [Alfresco Google Docs Integration documentation](https://docs.alfresco.com/google-drive/latest/).

When building from source you must include your own Google OAuth client secret json file. Instructions for creating it can be found at [Google Drive REST API - Authorizing requests with OAuth 2.0](https://developers.google.com/drive/v3/web/about-auth).

The content of the generated file should be added to the appropriate `client_secret-{community,enterprise}.json` file found in `Google Docs Repository/src/main/oauth-client-resources/`.

Building
--------
Windows users must ensure that the following value is set in Docker Engine > Edit JSON: “experimental: true”.

When building the amps (`mvn clean package`) you should also include a combination of the following profiles (`-P`). If not included, the defaults are applied.

**Docker images**

'local' Build amps for local dev/test only

'docker-end-to-end-setup' Build amps for future end-to-end tests

Example: `mvn clean package -Plocal`
	
***Note:** Amps built from this source are not supported by Alfresco. They are built and used at your own risk. Supported releases of the enterprise amps can be found at the Alfresco Customer Portal.*

Contributing
------------
Thanks for your interest in contributing to this project!

The following is a set of guidelines for contributing to this module. Most of them will make the life of the reviewer easier and therefore decrease the time required for the patch be included in a future version.

Ways to contribute would be by submitting pull requests, reporting issues and creating suggestions. In the case of a defect please provide steps to reproduce the issue, as well as the expected result and the actual one.

You can report an issue in the [ALF](https://alfresco.atlassian.net/projects/ALF/issues) jira project of the Alfresco issue tracker. Read instructions for a [good issue report](https://hub.alfresco.com/t5/alfresco-content-services-hub/reporting-an-issue/ba-p/289727). Please also set the component as "GoogleDocs".

If you'd like a hand at trying to implement features yourself, please validate your changes by running the tests. Also pull requests should contain tests whenever possible. Please follow the [coding standards](https://hub.alfresco.com/t5/alfresco-content-services-hub/coding-standards-for-alfresco-content-services/ba-p/290457).

As a contributor you must sign a contribution agreement, but please keep in mind that the contribution process includes a recorded acceptance step.

* [Submitting Contributions](https://hub.alfresco.com/t5/alfresco-content-services-hub/submitting-contributions/ba-p/293325)
* [Accepting the Contribution Agreement](https://hub.alfresco.com/t5/alfresco-content-services-hub/alfresco-contribution-agreement/ba-p/293276)
