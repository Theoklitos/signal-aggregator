package com.quantbro.aggregator.adapters.scraping;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Various small methods to help with scraping
 */
public final class ScrapingUtils {

	private static final int DEFAULT_SECONDS_TO_WAIT_FOR_ELEMENTS = 15;

	public static void sleep(final int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void takeScreenshotLocally(final RemoteWebDriver driver, final String location) {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(location));
			out.write(driver.getScreenshotAs(OutputType.BYTES));
		} catch (final Exception e1) {
			e1.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

	}

	/**
	 * will wait for the default amount of time
	 */
	public static WebElement waitForPresenceOfElementWithClass(final WebDriver driver, final String className) {
		final WebDriverWait wait = new WebDriverWait(driver, DEFAULT_SECONDS_TO_WAIT_FOR_ELEMENTS);
		return wait.until(ExpectedConditions.presenceOfElementLocated(By.className(className)));
	}

	/**
	 * will wait for the default amount of time
	 */
	public static void waitForUrlToContain(final WebDriver driver, final String path) {
		final WebDriverWait wait = new WebDriverWait(driver, DEFAULT_SECONDS_TO_WAIT_FOR_ELEMENTS);
		wait.until(ExpectedConditions.urlContains(path));
	}

	/**
	 * will wait for the default amount of time
	 */
	public static WebElement waitForVisibilityOfElementWithId(final WebDriver driver, final String id) {
		final WebDriverWait wait = new WebDriverWait(driver, DEFAULT_SECONDS_TO_WAIT_FOR_ELEMENTS);
		return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
	}

}
