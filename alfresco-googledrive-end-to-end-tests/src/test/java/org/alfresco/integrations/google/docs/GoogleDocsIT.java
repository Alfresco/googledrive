/*
 * Copyright 2015-2018 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
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
        try {
            driver.get("http://share:8080/share/page");
            driver.findElement(By.xpath("//*[@id=\"page_x002e_components_x002e_slingshot-login_x0023_default-username\"]")).sendKeys("admin");
            driver.findElement(By.xpath("//*[@id=\"page_x002e_components_x002e_slingshot-login_x0023_default-password\"]")).sendKeys("admin");
            driver.findElement(By.xpath("//*[@id=\"page_x002e_components_x002e_slingshot-login_x0023_default-submit-button\"]")).click();

            WebElement titleElement = wait.until(presenceOfElementLocated(By.id("HEADER_TITLE")));
            Assert.assertEquals("Administrator Dashboard", titleElement.getText());
        } finally {
            driver.quit();
        }
    }

}
