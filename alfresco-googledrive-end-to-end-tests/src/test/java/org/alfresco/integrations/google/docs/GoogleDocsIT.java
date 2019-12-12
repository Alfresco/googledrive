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

package org.alfresco.integrations.google.docs;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class GoogleDocsIT
{
    private WebDriver driver;
    private WebDriverWait wait;

    @Before
    public void setup() throws Exception
    {
        driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), new ChromeOptions());
        wait = new WebDriverWait(driver, 30);
    }

    @Test
    public void testShareLogin()
    {
        try
        {
            driver.get("http://share:8080/share/page");
            driver.findElement(By.xpath(
                "//*[@id=\"page_x002e_components_x002e_slingshot-login_x0023_default-username\"]"))
                  .sendKeys("admin");
            driver.findElement(By.xpath(
                "//*[@id=\"page_x002e_components_x002e_slingshot-login_x0023_default-password\"]"))
                  .sendKeys("admin");
            driver.findElement(By.xpath(
                "//*[@id=\"page_x002e_components_x002e_slingshot-login_x0023_default-submit-button\"]"))
                  .click();

            WebElement titleElement = wait.until(presenceOfElementLocated(By.id("HEADER_TITLE")));
            Assert.assertEquals("Administrator Dashboard", titleElement.getText());
        }
        finally
        {
            driver.quit();
        }
    }
}
