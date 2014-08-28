package com.captainbern.mserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyHandler {

    private final File file;

    private Properties properties = new Properties();

    public PropertyHandler(File propertyFile) {
        this.file = propertyFile;

        if (propertyFile.exists()) {
            FileInputStream inputStream = null;

            try {
                inputStream = new FileInputStream(propertyFile);
                this.properties.load(inputStream);
            } catch (IOException e) {
                MaintenanceServer.LOGGER.warn("Failed to load " + propertyFile, e);
                this.saveProperties();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Swallow
                    }
                }
            }
        } else {
            this.saveProperties();
        }
    }

    public Properties getProperties() {
        return this.properties;
    }

    public void saveProperties() {
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(this.file);
            this.properties.store(outputStream, "MaintenanceServer Properties");
        } catch (IOException e) {
            MaintenanceServer.LOGGER.warn("Failed to save " + this.getFile());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // Swallow
                }
            }
        }
    }

    public File getFile() {
        return this.file;
    }

    public String getString(String key, String defValue) {
        if (!this.properties.containsKey(key)) {
            this.properties.setProperty(key, defValue);
            this.saveProperties();
        }

        return this.properties.getProperty(key);
    }

    public int getInt(String key, int defValue) {
        try {
            return Integer.parseInt(getString(key, "" + defValue));
        } catch (Exception e) {
            this.properties.setProperty(key, "" + defValue);
            this.saveProperties();
            return defValue;
        }
    }

    public void setProperty(String key, Object value) {
        this.properties.setProperty(key, "" + value);
    }
}
