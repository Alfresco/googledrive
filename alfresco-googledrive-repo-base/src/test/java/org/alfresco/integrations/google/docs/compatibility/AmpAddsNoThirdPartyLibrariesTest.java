/*
 * Copyright (C) 2026 Alfresco Software Limited
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
package org.alfresco.integrations.google.docs.compatibility;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Guards the single guarantee the Google Drive AMPs must uphold: they add NO third-party libraries to the
 * ACS runtime classpath. This repo-base module is where every third-party library the integration needs -
 * the Google Drive/OAuth2 SDK, google-http-client, jackson, httpclient and their transitives - enters the
 * reactor. Each is declared {@code provided} and {@code optional}, so the content-services WARs (built in
 * {@code alfresco-community-repo} / {@code alfresco-enterprise-repo}) ship them and neither the community
 * nor the enterprise AMP bundles any of them.
 *
 * <p>Binary compatibility between that bundled SDK and the platform libraries (jackson, guava,
 * httpclient, ...) is owned by {@code alfresco-community-repo}, which manages every version there; it is
 * verified by {@code DriveSdkPlatformCompatibilityTest} alongside the WAR, not here.</p>
 *
 * <p>{@code maven-dependency-plugin} stages the jars this module would contribute to an AMP (its
 * runtime-scoped dependencies) under {@code target/mmt-analysis/amp-libs} in the
 * {@code process-test-resources} phase. With every third-party dependency {@code provided} that set is
 * empty - the directory may not even be created.</p>
 */
public class AmpAddsNoThirdPartyLibrariesTest
{
    @Test
    public void ampBundlesNoThirdPartyLibraries() throws IOException
    {
        List<String> bundled = listBundledJars();
        System.out.println("[AMP] repo-base contributes " + bundled.size() + " third-party library jar(s): " + bundled);

        assertTrue("The Google Drive AMP must not add third-party libraries to the ACS classpath, but these "
                + "jars would be bundled: " + bundled + ". Every third-party dependency must be 'provided' "
                + "(shipped by the content-services WAR).", bundled.isEmpty());
    }

    private static List<String> listBundledJars() throws IOException
    {
        Path ampLibs = Paths.get("target", "mmt-analysis", "amp-libs");
        List<String> jars = new ArrayList<>();
        if (Files.isDirectory(ampLibs))
        {
            try (DirectoryStream<Path> jarFiles = Files.newDirectoryStream(ampLibs, "*.jar"))
            {
                jarFiles.forEach(jar -> jars.add(jar.getFileName().toString()));
            }
        }
        return jars;
    }
}
