package org.testingchief;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;

/**
 * Selenium Manager to download the required
 * browser drivers for Selenium Test runs
 */
public class SeleniumManager {
    static final Logger logger = LogManager.getLogger(SeleniumManager.class);
    static final String DRIVERS_DIR = "drivers/";

    private SeleniumManager() {
        //private constructor
    }

    /**
     * Selenium manager main function
     *
     * @param args ""
     */
    public static void main(String[] args) {
        String browserVersion = System.getProperty("browserVersion");
        String[] browserArray = {"chrome", "edge"};
        String[] proxyArray = getProxyList();

        logger.info("Clearing {} directory", DRIVERS_DIR);
        clearResourcesDir("");
        clearResourcesDir("chromedriver");
        clearResourcesDir("msedgedriver");
        logger.info("Completed deleting files in {}", DRIVERS_DIR);

        logger.info("Started copying selenium-manager to {}", DRIVERS_DIR);
        getSeleniumManager();
        logger.info("Completed copying selenium-manager to {}", DRIVERS_DIR);

        for (String browserName : browserArray) {
            logger.info("Started downloading {} browser driver version {} to {}", browserName, browserVersion, DRIVERS_DIR);
            for (String proxyName : proxyArray) {
                downloadDriver(browserName, browserVersion, proxyName);
                if (checkDriverExists(browserName, browserVersion)) {
                    copyDriverFile(locateDriver(DRIVERS_DIR, getDriverName(browserName), browserVersion), browserName);
                }
            }
        }
        clearResourcesDir("chromedriver");
        clearResourcesDir("msedgedriver");
    }

    /**
     * Get Driver Name
     *
     * @param browserName "chrome or edge"
     * @return String driverName
     */
    private static String getDriverName(String browserName) {
        String driverName = "";
        switch (browserName) {
            case "chrome":
                driverName = "chromedriver.exe";
                break;
            case "edge":
                driverName = "msedgedriver.exe";
                break;
            default:
                //do nothing
                break;
        }
        return driverName;
    }

    /**
     * Copy driver file (.exe) to Resources directory
     *
     * @param filePath    "file path"
     * @param browserName "chrome, edge"
     */
    private static void copyDriverFile(String filePath, String browserName) {
        String driverName = getDriverName(browserName);
        File driverFile = new File(filePath + "\\" + driverName);
        try (FileOutputStream output = new FileOutputStream(DRIVERS_DIR + driverName)) {
            try (InputStream inputStream = new FileInputStream(driverFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead = inputStream.read(buffer);
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead);
                    bytesRead = inputStream.read(buffer);
                }
            }
            logger.info("Copied driver file to {}", DRIVERS_DIR);
        } catch (Exception e) {
            logger.error("Error copying driver file: ", e);
        }
    }

    /**
     * Clear Driver directories
     *
     * @param folderName "Directory"
     */
    private static void clearResourcesDir(String folderName) {
        try {
            Files.createDirectories(Paths.get(DRIVERS_DIR));
            FileUtils.cleanDirectory(new File(DRIVERS_DIR + folderName));
            logger.info("Cleaned {}{} directory", DRIVERS_DIR, folderName);
        } catch (Exception e) {
            logger.error("Unable to clear Driver directory {}", e.getMessage());
        }
    }

    /**
     * Copying selenium-manager from JAR to resources directory
     */
    private static void getSeleniumManager() {
        try {
            URL url = getClassLoader().getResource("org/openqa/selenium/manager/windows/selenium-manager.exe");
            try (FileOutputStream output = new FileOutputStream(DRIVERS_DIR + "selenium-manager.exe")) {
                if (url != null) {
                    try (InputStream input = url.openStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead = input.read(buffer);
                        while (bytesRead != -1) {
                            output.write(buffer, 0, bytesRead);
                            bytesRead = input.read(buffer);
                        }
                    }
                }
            }
            logger.info("Copying selenium-manager to {}", DRIVERS_DIR);
        } catch (Exception e) {
            logger.error("Unable to copy selenium-manager to {}", DRIVERS_DIR);
        }
    }

    /**
     * Download required driver
     *
     * @param browserName    "chrome, edge"
     * @param browserVersion "browser version"
     * @param proxyName      "proxy"
     */
    private static void downloadDriver(String browserName, String browserVersion, String proxyName) {
        System.setProperty("SE_CACHE_PATH", DRIVERS_DIR + browserVersion);
        Process downloader;
        try {
            if (proxyName.equals("")) {
                logger.info("Processing with NO proxy");
                downloader = Runtime.getRuntime().exec(DRIVERS_DIR + "selenium-manager" + " --debug " + " --avoid-browser-download " + " --browser " + browserName + " --browser-version " + browserVersion + " --cache-path " + DRIVERS_DIR);
            } else {
                logger.info("Processing with proxy: {}", proxyName);
                downloader = Runtime.getRuntime().exec(DRIVERS_DIR + "selenium-manager" + " --debug " + " --avoid-browser-download " + " --browser " + browserName + " --browser-version " + browserVersion + " --cache-path " + DRIVERS_DIR + " --proxy " + proxyName);
            }
            downloader.waitFor();
        } catch (Exception e) {
            logger.error("Unable to download {} driver version {} : {}", browserName, browserVersion, e);
        }
    }

    /**
     * Check if driver exe already exists
     *
     * @param browserName    "chrome, edge"
     * @param browserVersion "browser version"
     * @return True or False
     */
    private static boolean checkDriverExists(String browserName, String browserVersion) {
        boolean driverExists = false;
        String driverName = getDriverName(browserName);
        try {
            String dir = locateDriver(DRIVERS_DIR, driverName, browserVersion);
            if (dir.contains("\\" + browserVersion)) {
                driverExists = true;
                logger.info("{} driver found in {}", browserName, dir);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return driverExists;
    }

    /**
     * Locate driver file
     *
     * @param filePath       "file path"
     * @param driverName     "required driver file"
     * @param browserVersion "browser version"
     * @return String file path
     */
    private static String locateDriver(String filePath, String driverName, String browserVersion) {
        String fileLocation = "";
        List<Path> filesList = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(filePath))) {
            filesList = paths.filter(p -> p.toString().endsWith(driverName) && p.toString().contains(browserVersion)).map(Path::getParent).distinct().collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Unable to find driver exe: ", e);
        }

        for (Path file : filesList) {
            fileLocation = file.toString();
            break;
        }
        return fileLocation;
    }

    /**
     * Get proxy list
     *
     * @return "proxy"
     */
    private static String[] getProxyList() {
        return new String[]{""};
    }
}
