Alfresco Google Docs Module
===========================

Description
-----------

This extension adds the ability to edit supported content items in Google Docs&trade; to the Alfresco repository and Share interface.

When building from source you must include your own Google OAuth client secret json file. Instructions for creating it can be found at [Google Drive REST API - Authorizing requests with OAuth 2.0](https://developers.google.com/drive/v3/web/about-auth).

The content of the generated file should be added to the appropriate `client_secret-{community,enterprise}.json` file found in `Google Docs Repository/src/main/oauth-client-resources/`.

Building
--------

When building the amps (`mvn clean package`) you should also include a combination of the following profiles (`-P`). If not include, the defaults are applied.

**Platform**

`community` Build amps for Alfresco Community (default)

`enterprise` Build amps for Alfresco Enterprise

**Version**

`6` Build amps for version 6.x of Alfresco

Example: `mvn clean package -Penterprise`
	
***Note:** Amps built from this source are not supported by Alfresco. They are built and used at your own risk. Supported releases of the enterprise amps can be found at the Alfresco Customer Portal.*

Contributing
------------
Thanks for your interest in contributing to this project!

The following is a set of guidelines for contributing to this module. Most of them will make the life of the reviewer easier and therefore decrease the time required for the patch be included in a future version.

Ways to contribute would be by submitting pull requests, reporting issues and creating suggestions. In the case of a defect please provide steps to reproduce the issue, as well as the expected result and the actual one.

You can report an issue in the [ALF](https://issues.alfresco.com/jira/projects/ALF/issues) jira project of the Alfresco issue tracker. Read instructions for a [good issue report](https://community.alfresco.com/docs/DOC-6263-reporting-an-issue). Please also set the component as "GoogleDocs".

If you'd like a hand at trying to implement features yourself, please validate your changes by running the tests. Also pull requests should contain tests whenever possible. Please follow the [coding standards](https://community.alfresco.com/docs/DOC-4658-coding-standards).

As a contributor you must sign a contribution agreement, but please keep in mind that the contribution process includes a recorded acceptance step.

* [Submitting Contributions](https://community.alfresco.com/docs/DOC-6269-submitting-contributions)
* [Accepting the Contribution Agreement](https://community.alfresco.com/docs/DOC-7070-alfresco-contribution-agreement)

