package org.texttechnologylab.duui.analysis.document;

import org.texttechnologylab.duui.analysis.process.IDUUIProcessHandler;
import org.bson.Document;

import java.util.Objects;

/**
 * This class is a utility to have easier access to input and output settings of an
 * {@link IDUUIProcessHandler}. Providers include Dropbox, Minio and Text but is
 * designed to support any future cloud or database providers.
 *
 * @author Cedric Borkowski
 */
public class DUUIDocumentProvider {

    /**
     * The name of the data provider ({@link Provider}) e.g. dropbox
     */
    private final String provider;

    /**
     * The id of the data provider connection
     */
    private final String providerId;

    /**
     * Optional. The path from where to read data if the provider is not {@link Provider}
     */
    private final String path;

    /**
     * Optional. If the provider is plain text, the content holds the text to be analyzed.
     */
    private final String content;

    /**
     * Optional. A filter to only select matching file extensions.
     */
    private final String fileExtension;

    /**
     * Constructs a DUUIDocumentProvider from a BSON Document.
     *
     * @param document the BSON Document containing the provider information
     */
    public DUUIDocumentProvider(Document document) {
        provider = document.getString("provider");
        providerId = document.getString("provider_id");
        path = document.getString("path");
        content = document.getString("content");
        fileExtension = document.getString("file_extension");
    }

    /**
     * Constructs a DUUIDocumentProvider with the specified parameters.
     *
     * @param provider the name of the data provider
     * @param providerId the ID of the data provider connection
     * @param path the path from where to read data
     * @param content the content to be analyzed if the provider is plain text
     * @param fileExtension the file extension filter
     */
    public DUUIDocumentProvider(String provider, String providerId, String path, String content, String fileExtension) {
        this.provider = provider;
        this.providerId = providerId;
        this.path = path;
        this.content = content;
        this.fileExtension = fileExtension;
    }

    /**
     * Returns the name of the data provider.
     *
     * @return the name of the data provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the path from where to read data if the provider is not {@link Provider}.
     *
     * @return the path from where to read data
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the content to be analyzed if the provider is plain text.
     *
     * @return the content to be analyzed
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the file extension filter to only select matching file extensions.
     *
     * @return the file extension filter
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Converts this DUUIDocumentProvider to a BSON Document.
     *
     * @return a BSON Document representing this DUUIDocumentProvider
     */
    public Document toDocument() {
        return new Document()
            .append("provider", provider)
            .append("provider_id", providerId)
            .append("path", path)
            .append("content", content)
            .append("file_extension", fileExtension);
    }

    /**
     * Checks if this DUUIDocumentProvider is equal to another object.
     *
     * @param o the object to compare with
     * @return true if this DUUIDocumentProvider is equal to the specified object, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DUUIDocumentProvider that = (DUUIDocumentProvider) o;
        return Objects.equals(provider, that.provider);
    }

    /**
     * Returns the hash code value for this DUUIDocumentProvider.
     *
     * @return the hash code value for this DUUIDocumentProvider
     */
    @Override
    public int hashCode() {
        return Objects.hash(provider);
    }

    /**
     * Checks if this DUUIDocumentProvider has no output.
     *
     * @return true if this DUUIDocumentProvider has no output, false otherwise
     */
    public boolean hasNoOutput() {
        return provider.equals(Provider.TEXT) || provider.equals(Provider.NONE);
    }

    /**
     * Checks if this DUUIDocumentProvider is a text provider.
     *
     * @return true if this DUUIDocumentProvider is a text provider, false otherwise
     */
    public boolean isText() {
        return provider.equals(Provider.TEXT);
    }

    /**
     * Checks if this DUUIDocumentProvider is a cloud provider.
     *
     * @return true if this DUUIDocumentProvider is a cloud provider, false otherwise
     */
    public boolean isCloudProvider() {
        return provider.equalsIgnoreCase(Provider.DROPBOX)
            || provider.equalsIgnoreCase(Provider.MINIO)
            || provider.equalsIgnoreCase(Provider.ONEDRIVE)
            || provider.equalsIgnoreCase(Provider.NEXTCLOUD)
            || provider.equalsIgnoreCase(Provider.GOOGLE);
    }

    **
     * Checks if this DUUIDocumentProvider is a database provider.
     *
     * @return true if this DUUIDocumentProvider is a database provider, false otherwise
     */
    public boolean isDatabaseProvider() {
        return provider.equalsIgnoreCase(Provider.MONGODB);
    }

    /**
     * Returns the ID of the data provider connection.
     *
     * @return the ID of the data provider connection
     */
    public String getProviderId() {
        return providerId;
    }
}
