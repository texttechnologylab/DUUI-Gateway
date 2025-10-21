package org.texttechnologylab.duui.api.storage;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.texttechnologylab.duui.analysis.document.Provider;
import org.texttechnologylab.duui.api.controllers.users.Role;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DataModel {

    public record MongoComponent(
            ObjectId id,
            String name,
            String description,
            String driver,
            String target,
            String pipeline_id,
            String user_id,
            Integer index,
            Long created_at,
            Long modified_at,
            String status,
            List<String> tags,
            Map<String, String> parameters,
            MongoComponentOptions options
    ) {}

    public record MongoComponentOptions(
            Boolean use_GPU,
            Boolean docker_image_fetching,
            Integer scale,
            Boolean keep_alive,
            String host,
            Boolean ignore_200_error,
            List<String> labels,
            List<String> constraints,
            MongoDockerRegistryAuth registry_auth
    ){}

    public record MongoDockerRegistryAuth(
            String username,
            String password
    ) {}

    public record MongoPipeline(
            ObjectId id,
            String name,
            String description,
            String user_id,
            String status,
            List<String> tags,
            Long created_at,
            Long modified_at,
            Long last_used,
            Integer times_uses,
            Map<String, String> settings
    ) {}

    public record MongoProcess(
            ObjectId id,
            String pipeline_id,
            String status,
            String error,
            Boolean is_finished,
            List<String> document_names,
            Long started_at,
            Long finished_at,
            Long size,
            Integer progress,
            MongoProcessSettings settings,
            MongoDocumentProvider input,
            MongoDocumentProvider output
    ) {}

    public record MongoDocumentProvider(
            Provider provider,
            String provider_id, // Connection ID
            String path,
            String content,
            String file_extension
    ) {}



    public record MongoProcessSettings(
            String language,
            Boolean notification,
            Boolean check_target,
            Boolean recursive,
            Boolean overwrite,
            Boolean sort_by_size,
            Boolean ignore_errors,
            Long minimum_size,
            Integer worker_count
    ) {}


    public record MongoDocument(
            ObjectId id,
            String path,
            String name,
            String process_id,
            Long duration_decode,
            Long duration_deserialize,
            Long duration_process,
            Long duration_wait,
            Long finished_at,
            Long progress,
            Long progress_download,
            Long progress_upload,
            Long size,
            Long started_at,
            String status,
            Boolean is_finished,
            String error,
            Map<String, Integer> annotations
    ) {}

    public record MongoEvent(
            ObjectId id,
            Long timestamp,
            MongoEventEntry event
    ) {}

    public record MongoEventEntry(
            String process_id,
            String sender,
            String message
    ) {}

    public record MongoLabel(
            ObjectId id,
            String name,
            String driver,
            List<String> groups // Group IDs
    ) {}

    public record MongoSettings(
            String file_upload_directory,
            String allowed_origins,
            String dbx_app_key,
            String dbx_app_secret,
            String dbx_redirect_url,
            String google_client_id,
            String google_client_secret,
            String google_redirect_uri,
            String local_drive_root,
            Document local_folder_structure

    ) {}

    public record MongoGroup(
            ObjectId id,
            String name,
            List<String> members, // User IDs
            List<String> whitelist
    ) {}

    public record MongoRegistry(
            ObjectId id,
            String name,
            String endpoint,
            String scope,
            List<String> groups // Group IDs
    ) {}

    public record MongoUser(
            ObjectId id,
            String email,
            String password,
            Long created_at,
            Role role,
            Integer workers,
            String session,
            Boolean activated,
            String password_reset_token,
            Long reset_token_expiration,
            Map<String, MongoPreference> preferences,
            MongoConnections connections
    ) {}

    public record MongoConnections(
            String api_key,
            Integer worker_count,
            Map<String, MongoConnection.Dropbox> dropbox,
            Map<String, MongoConnection.MinIO> minio,
            Map<String, MongoConnection.Nextcloud> nextcloud,
            Map<String, MongoConnection.Google> google
    ) {}

    public sealed interface MongoConnection {
        public record Dropbox (
                String alias,
                String refresh_token,
                String access_token
        ) implements MongoConnection {}

        public record MinIO (
                String alias,
                String endpoint,
                String username,
                String password
        ) implements MongoConnection {}

        public record Nextcloud (
                String alias,
                String endpoint,
                String username,
                String password
        ) implements MongoConnection {}

        public record Google (
                String alias,
                String refresh_token,
                String access_token
        ) implements MongoConnection {}
    }

    public record MongoPreference (
            Boolean tutorial,
            String language,
            Boolean notifications
    ) {}

    public record MongoActivation(
            ObjectId id,
            String userId,
            String purpose,
            String codeHash,
            Instant expiresAt,
            Instant createdAt,
            boolean used
    ) {}

    public record MongoMigration(
            String id,
            String author,
            Long ts
    ) {}

}
