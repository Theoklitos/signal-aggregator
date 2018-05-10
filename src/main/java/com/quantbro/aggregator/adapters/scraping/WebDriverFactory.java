package com.quantbro.aggregator.adapters.scraping;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.services.JobService;

/**
 * Used to set properties to a driver before creating it
 */
public final class WebDriverFactory {

	// see here: https://techblog.willshouse.com/2012/01/03/most-common-user-agents/
	private final static List<String> USER_AGENTS = Lists.newArrayList(
			"Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X; en-us) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53",
			"Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19",
			"Mozilla/5.0 (Windows Phone 10.0; Android 4.2.1; Xbox; Xbox One) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Mobile Safari/537.36 Edge/13.10586",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");

	public static ChromeDriver createDriver() {
		System.setProperty("webdriver.chrome.driver", JobService.PHANTOMJS_DRIVER_LOCATION_PROPERTY);
		System.setProperty("webdriver.chrome.args", "--disable-logging");
		System.setProperty("webdriver.chrome.silentOutput", "true");
		final ChromeOptions options = new ChromeOptions();
		options.addArguments("--log-level=3");
		options.addArguments("--user-agent=" + getRandomUserAgent());
		final ChromeDriver driver = new ChromeDriver(options);
		return driver;
	}

	public static PhantomJSDriver createPhantomJsDriver() {
		final DesiredCapabilities caps = new DesiredCapabilities();
		final String[] phantomArgs = new String[] { "--webdriver-loglevel=NONE" };
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, phantomArgs);
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", getRandomUserAgent());
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, System.getProperty(JobService.PHANTOMJS_DRIVER_LOCATION_PROPERTY));
		final PhantomJSDriver driver = new PhantomJSDriver(caps);
		driver.setLogLevel(Level.OFF);
		return driver;
	}

	private static String getRandomUserAgent() {
		final int numberOfPossibleAgents = USER_AGENTS.size();
		return USER_AGENTS.get(new Random().nextInt(numberOfPossibleAgents));
	}

}
