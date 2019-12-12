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

package org.alfresco.integrations.web.evaluator;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.evaluator.BaseEvaluator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

public class SizeLimitEvaluator extends BaseEvaluator
{
    private static final Log log = LogFactory.getLog(SizeLimitEvaluator.class);

    private static final String DOCUMENT_TYPE     = "document";
    private static final String PRESENTATION_TYPE = "presentation";
    private static final String SPREADSHEET_TYPE  = "spreadsheet";

    private String accessor;

    private JSONObject importFormats;

    private long maxDocumentSize;
    private long maxSpreadsheetSize;
    private long maxPresentationSize;

    public void setImportFormats(String accessor)
    {
        this.accessor = accessor;
    }

    public void setMaxDocumentSize(long size)
    {
        this.maxDocumentSize = size;
    }

    public void setMaxSpreadsheetSize(long size)
    {
        this.maxSpreadsheetSize = size;
    }

    public void setMaxPresentationSize(long size)
    {
        this.maxPresentationSize = size;
    }

    @Override
    public boolean evaluate(JSONObject jsonObject)
    {
        importFormats = (JSONObject) getJSONValue(getMetadata(), accessor);

        try
        {
            JSONObject node = (JSONObject) jsonObject.get("node");
            if (node == null)
            {
                return false;
            }
            long size = ((Number) node.get("size")).longValue();
            final String contentType = getContentType(node.get("mimetype").toString());
            if (contentType == null)
            {
                log.debug("NodeRef: " + node.get("nodeRef") + " - missing content type.");
                return false;
            }

            if (log.isDebugEnabled())
            {
                log.debug("NodeRef: " + node.get("nodeRef") + "ContentType: " + contentType +
                          "; Max file Size: " + getMaxFileSize(contentType) +
                          "; Actual File Size: " + size);
            }

            if (size > getMaxFileSize(contentType))
            {
                log.debug("NodeRef: " + node.get("nodeRef") + " exceeds Max file size.");
                return false;
            }

            return true;
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException("Failed to run action evaluator: " + e.getMessage());
        }
    }

    private String getContentType(String mimetype)
    {
        if (importFormats.containsKey(mimetype))
        {
            return importFormats.get(mimetype).toString();
        }
        return null;
    }

    private long getMaxFileSize(String contentType)
    {
        switch (contentType)
        {
        case DOCUMENT_TYPE:
            return maxDocumentSize;
        case SPREADSHEET_TYPE:
            return maxSpreadsheetSize;
        case PRESENTATION_TYPE:
            return maxPresentationSize;
        default:
            return Long.MAX_VALUE;
        }
    }
}
