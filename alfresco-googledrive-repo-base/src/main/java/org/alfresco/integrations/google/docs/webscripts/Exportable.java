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

import static org.apache.commons.httpclient.HttpStatus.SC_NOT_ACCEPTABLE;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.integrations.google.docs.exceptions.MustDowngradeFormatException;
import org.alfresco.integrations.google.docs.exceptions.MustUpgradeFormatException;
import org.alfresco.integrations.google.docs.service.GoogleDocsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author Jared Ottley <jared.ottley@alfresco.com>
 */
public class Exportable extends GoogleDocsWebScripts
{
    private static final Log log = LogFactory.getLog(Exportable.class);

    private GoogleDocsService googledocsService;

    private final static String PARAM_MIMETYPE     = "mimetype";
    private final static String MODEL_EXPORT_ACION = "export_action";

    private final static String ACTION_UPGRADE   = "upgrade";
    private final static String ACTION_DOWNGRADE = "downgrade";
    private final static String ACTION_DEFAULT   = "default";

    public void setGoogledocsService(GoogleDocsService googledocsService)
    {
        this.googledocsService = googledocsService;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        getGoogleDocsServiceSubsystem();

        Map<String, Object> model = new HashMap<>();

        try
        {
            if (googledocsService.isExportable(req.getParameter(PARAM_MIMETYPE)))
            {
                model.put(MODEL_EXPORT_ACION, ACTION_DEFAULT);
            }
            else
            {
                throw new WebScriptException(SC_NOT_ACCEPTABLE, "Content not exportable");
            }
        }
        catch (MustUpgradeFormatException e)
        {
            model.put(MODEL_EXPORT_ACION, ACTION_UPGRADE);
        }
        catch (MustDowngradeFormatException e)
        {
            model.put(MODEL_EXPORT_ACION, ACTION_DOWNGRADE);
        }

        log.debug("Mimetype: " + req.getParameter(PARAM_MIMETYPE) + "; export action: " +
                  model.get(MODEL_EXPORT_ACION));

        return model;
    }
}
