package cloud.fogbow.fs.core.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.fs.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;

public class BuildNumberHolder {
    private static final String BUILD_FILE = "./build";
    private Logger LOGGER = Logger.getLogger(BuildNumberHolder.class);

    private Properties properties;
    private static BuildNumberHolder instance;

    private BuildNumberHolder() {
        this.properties = readBuildFile(BUILD_FILE);
    }

    public static synchronized BuildNumberHolder getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new BuildNumberHolder();
        }
        return instance;
    }

    public String getBuildNumber() {
        return this.properties.getProperty(ConfigurationPropertyKeys.BUILD_NUMBER_KEY,
                ConfigurationPropertyDefaults.DEFAULT_BUILD_NUMBER);
    }

    private Properties readBuildFile(String fileName) {
        Properties prop = new Properties();
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(fileName);
            prop.load(fileInputStream);
        } catch (IOException e) {
            LOGGER.info(String.format(Messages.Log.PROPERTY_FILE_S_NOT_FOUND, fileName));
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    LOGGER.error(String.format(Messages.Log.UNABLE_TO_CLOSE_FILE_S, fileName), e);
                }
            }
        }
        return prop;
    }
}