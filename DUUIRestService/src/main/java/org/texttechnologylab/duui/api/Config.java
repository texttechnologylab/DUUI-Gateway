package org.texttechnologylab.duui.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.texttechnologylab.duui.api.storage.DUUIMongoDBStorage;

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
            String value; 
            if(System.getenv().containsKey(sKey)){
                value = System.getenv(sKey);
            }
            else{
                value = defaultValue;
            }

            return value;
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

    public String getSmtpHost() {
        return getValue("SMTP_HOST", "localhost");
    }

    public String getSmtpPort() { return getValue("SMTP_PORT", "587"); }

    public String getSmtpUser() {
        return getValue("SMTP_USERNAME", null);
    }

    public String getSmtpPassword() {
        return getValue("SMTP_PASSWORD", null);
    }

    public String getSmtpFromEmail() {
        return getValue("MAIL_FROM_EMAIL", null);
    }

    public boolean getUseSmptpDebug() {
        return Boolean.parseBoolean(getValue("USE_SMTP_DEBUG", "true"));
    }

    public String getDropboxKey() {
        
        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("dbx_app_key")) {
            value = DUUIMongoDBStorage.Settings().getString("dbx_app_key");
        } else {
            value = getValue("DBX_APP_KEY", null);
            DUUIMongoDBStorage.updateSettings("dbx_app_key", value);
        }
        return value;
    }

    public String getDropboxSecret() {
        
        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("dbx_app_secret")) {
            value = DUUIMongoDBStorage.Settings().getString("dbx_app_secret");
        } else {
            value = getValue("DBX_APP_SECRET", null);
            DUUIMongoDBStorage.updateSettings("dbx_app_secret", value);
        }
        return value;
    }

    public String getDropboxRedirectUrl() {
        
        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("dbx_redirect_url")) {
            value = DUUIMongoDBStorage.Settings().getString("dbx_redirect_url");
        } else {
            value = getValue("DBX_REDIRECT_URL", null);
            DUUIMongoDBStorage.updateSettings("dbx_redirect_url", value);
        }
        return value;
    }

    public String getGoogleClientId() {  
        
        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("google_client_id")) {
            value = DUUIMongoDBStorage.Settings().getString("google_client_id");
        } else {
            value = getValue("GOOGLE_CLIENT_ID", null);
            DUUIMongoDBStorage.updateSettings("google_client_id", value);
        }
        return value;
    }

    public String getGoogleClientSecret() { 
        
        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("google_client_secret")) {
            value = DUUIMongoDBStorage.Settings().getString("google_client_secret");
        } else {
            value = getValue("GOOGLE_CLIENT_SECRET", null);
            DUUIMongoDBStorage.updateSettings("google_client_secret", value);
        }
        return value;
    }

    public String getGoogleRedirectUri() { 

        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("google_redirect_uri")) {
            value = DUUIMongoDBStorage.Settings().getString("google_redirect_uri");
        } else {
            value = getValue("GOOGLE_REDIRECT_URI", null);
            DUUIMongoDBStorage.updateSettings("google_redirect_uri", value);
        }
        return value;
    }

    public List<String> getAllowedOrigins() {
        
        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("allowed_origins")) {
            value = DUUIMongoDBStorage.Settings().getString("allowed_origins");
        } else {
            value = getValue("ALLOWED_ORIGINS", "");
            DUUIMongoDBStorage.updateSettings("allowed_origins", value);
        }
        return List.of(value.split(";"));
    }

    public int getPort() {
        return Integer.parseInt(getValue("PORT", null));
    }

    public String getHost() {
        return getValue("HOST", "localhost");
    }

    public String getLocalDriveRoot() {
        return getValue("LOCAL_DRIVE_ROOT", null);
    }

    public String getFileUploadPath() {
        
        String value; 

        if (DUUIMongoDBStorage.Settings().containsKey("file_upload_directory")) {
            value = DUUIMongoDBStorage.Settings().get("file_upload_directory", "files/upload");
        } else {
            value = getValue("FILE_UPLOAD_DIRECTORY", "files/upload");
            DUUIMongoDBStorage.updateSettings("file_upload_directory", value);
        }
        return value;
    }

}
