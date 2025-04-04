package org.texttechnologylab.duui.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * A class holding configuration data for the application and allowing for easy access to the configuration.
 *
 * @author Cedric Borkowski
 */
public class Config {

    private final Properties properties;

    public Config(String configPath) throws IOException {
        properties = new Properties();
        try (InputStream input = new FileInputStream(Paths.get(configPath).toAbsolutePath().toString())) {
            properties.load(input);
        }
    }

    public Config() {
        properties = null;
    }

    private String getValue(String sKey, String defaultValue) {
        if(properties!=null){
            return properties.getProperty(sKey, defaultValue);
        }
        else{
            if(System.getenv().containsKey(sKey)){
                return System.getenv(sKey);
            }
            else{
                return defaultValue;
            }
        }
    }

    public String getMongoDBConnectionString() {
        return getValue("MONGO_DB_CONNECTION_STRING", null);
    }

    public String getMongoHost() {
        return getValue("MONGO_HOST", null);
    }

    public int getMongoPort() {
        return Integer.parseInt(getValue("MONGO_PORT", "27017"));
    }

    public String getMongoUser() {
        return getValue("MONGO_USER", null);
    }

    public String getMongoDatabase() {
        return getValue("MONGO_DB", null);
    }

    public String getMongoPassword() {
        return getValue("MONGO_PASSWORD", null);
    }

    public String getDropboxKey() {
        return getValue("DBX_APP_KEY", null);
    }

    public String getDropboxSecret() {
        return getValue("DBX_APP_SECRET", null);
    }

    public String getDropboxRedirectUrl() {
        return getValue("DBX_REDIRECT_URL", null);
    }

    public String getGoogleClientId() { return getValue("GOOGLE_CLIENT_ID", null); }

    public String getGoogleClientSecret() { return getValue("GOOGLE_CLIENT_SECRET", null); }

    public String getGoogleRedirectUri() { return getValue("GOOGLE_REDIRECT_URI", null); }

    public List<String> getAllowedOrigins() {
        return List.of(getValue("ALLOWED_ORIGINS", "").split(";"));
    }

    public int getPort() {
        return Integer.parseInt(getValue("PORT", null));
    }

    public String getHost() {
        return getValue("HOST", "localhost");
    }

    public String getFileUploadPath() {
        return getValue("FILE_UPLOAD_DIRECTORY", "files/upload");
    }

}
