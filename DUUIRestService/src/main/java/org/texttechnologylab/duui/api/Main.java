package org.texttechnologylab.duui.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.texttechnologylab.DockerUnifiedUIMAInterface.document_handler.*;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIStatus;
import org.texttechnologylab.duui.analysis.process.IDUUIProcessHandler;
import org.texttechnologylab.duui.api.controllers.pipelines.DUUIPipelineController;
import org.texttechnologylab.duui.api.controllers.processes.DUUIProcessController;
import org.texttechnologylab.duui.api.metrics.DUUIMetricsManager;
import org.texttechnologylab.duui.api.metrics.providers.DUUIHTTPMetrics;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;
import org.texttechnologylab.duui.api.routes.components.DUUIComponentRequestHandler;
import org.texttechnologylab.duui.api.storage.DUUIMongoDBStorage;

import com.dropbox.core.DbxException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

import spark.Request;
import spark.Response;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Accumulators.*;
import static spark.Spark.port;

/*
 * To build follow these steps for now:
 * - Run 'maven clean package -DskipTests'
 * - Set the correct working directory (where to jar is located)
 * - run 'java -jar DUUIRestService.jar PATH/TO/config.properties
 */

/**
 * The entry point to the DUUIRestService application. Running the application requires the path to
 * a config file to be specified in the command line arguments.
 *
 * @author Cedric Borkowski
 */
public class Main {
    public static Config config;

    public static void main(String[] args) {
        if(args.length>0) {
            try {
                String configFilePath = args[0];
                config = new Config(configFilePath);
                System.out.println("Starting DUUI REST Service");
            } catch (ArrayIndexOutOfBoundsException | IOException exception) {
                System.err.println("Create a config (ini, properties, ...) file and pass the path to this file as the first application argument.");
                System.err.println("The config file should contain the following variables: ");
                System.err.println("\t> DBX_APP_KEY");
                System.err.println("\t> DBX_APP_SECRET");
                System.err.println("\t> DBX_REDIRECT_URL");
                System.err.println("\t> GOOGLE_CLIENT_ID");
                System.err.println("\t> GOOGLE_CLIENT_SECRET");
                System.err.println("\t> GOOGLE_REDIRECT_URI");
                System.err.println("\t> PORT");
                System.err.println("\t> HOST");
                System.err.println("\t> FILE_UPLOAD_DIRECTORY");
                System.err.println("\t> MONGO_HOST");
                System.err.println("\t> MONGO_PORT");
                System.err.println("\t> MONGO_DB");
                System.err.println("\t> MONGO_USER");
                System.err.println("\t> MONGO_PASSWORD");
                System.err.println("\t> MONGO_DB_CONNECTION_STRING");
                System.err.println("Dropbox related variables are found in the App Console at https://www.dropbox.com/developers/apps");
                System.exit(0);
            }
        }
        else{
            config = new Config();
        }
        
        DUUIMongoDBStorage.init(config);
        DUUIMetricsManager.init();


        try {
            port(config.getPort());
        } catch (NumberFormatException exception) {
            port(2605);
        }

        Methods.init();


        DUUIComponentRequestHandler.insertSpacyTemplate();

        File fileUploadDirectory = Paths.get(config.getFileUploadPath()).toFile();

        if (!fileUploadDirectory.exists()) {
            boolean ignored = fileUploadDirectory.mkdirs();
        }


        Runtime.getRuntime().addShutdownHook(
            new Thread(
                () -> {
                    DUUIPipelineController
                        .getReusablePipelines()
                        .keySet()
                        .forEach(DUUIPipelineController::shutdownPipeline);

                    DUUIProcessController
                        .getActiveProcesses()
                        .forEach(IDUUIProcessHandler::cancel);

                    DUUIMongoDBStorage.Pipelines().updateMany(
                        Filters.exists("status", true),
                        Updates.set("status", DUUIStatus.INACTIVE)
                    );
                }
            ));
    }


    /**
     * Upload one or multiple files to the specified UPLOAD_DIRECTORY path in the config file. Files are stored
     * under UPLOAD_DIRECTORY/uuid
     *
     * @return a JSON Document containing the path to parent folder (uuid).
     */
    public static String uploadFile(Request request, Response response) throws ServletException, IOException, DbxException, GeneralSecurityException {
        String authorization = request.headers("Authorization");
        DUUIRequestHelper.authenticate(authorization);

        Document user = DUUIRequestHelper.authenticate(authorization);
        if (DUUIRequestHelper.isNullOrEmpty(user)) return DUUIRequestHelper.unauthorized(response);


        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
        Collection<Part> parts = request.raw().getParts();
        if (parts.isEmpty()) return DUUIRequestHelper.notFound(response);
        String uuid = UUID.randomUUID().toString();
        Path root = Paths.get(Main.config.getFileUploadPath(), uuid);
        boolean ignored = root.toFile().mkdirs();

        for (Part part : parts) {
            if (!part.getName().equals("file")) continue;

            DUUIHTTPMetrics.incrementFilesUploaded(1);
            Path path = Paths.get(root.toString(), part.getSubmittedFileName());

            try (InputStream is = part.getInputStream()) {
                Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
                DUUIHTTPMetrics.incrementBytesUploaded((double) path.toFile().length());
            } catch (IOException exception) {
                response.status(500);
                return "Failed to upload file " + exception;
            }
        }

        boolean storeFiles = request.queryParamOrDefault("store", "false").equals("true");
        if (storeFiles) {
            String path = request.queryParamOrDefault("path", "");
            String provider = request.queryParamOrDefault("provider", "");
            String providerId = request.queryParamOrDefault("provider_id", "");

            IDUUIDocumentHandler handler = DUUIProcessController.getHandler(provider, providerId, DUUIRequestHelper.getUserId(request));
            if (handler != null) {
                DUUILocalDocumentHandler localHandler = new DUUILocalDocumentHandler();
                List<DUUIDocument> paths = localHandler.listDocuments(root.toString(), "", true);
                List<DUUIDocument> documents = localHandler.readDocuments(paths.stream().map(DUUIDocument::getPath).toList());
                handler.writeDocuments(documents, path);
            }
        }

        response.status(200);
        return new Document("path", root.toString()).toJson();
    }

    public static String preprocessCas(Request request, Response response) {
        String userId = DUUIRequestHelper.getUserId(request);
        String provider = request.queryParamOrDefault("provider", null);
        String providerId = request.queryParamOrDefault("provider_id", null);
        String path = request.queryParamOrDefault("path", null);
        String pipelineId = request.queryParamOrDefault("pipeline_id", null);

        if (DUUIRequestHelper.isNullOrEmpty(provider))
            return DUUIRequestHelper.badRequest(response, "Missing provider in query params.");
        if (DUUIRequestHelper.isNullOrEmpty(path))
            return DUUIRequestHelper.badRequest(response, "Missing path in query params.");
        if (DUUIRequestHelper.isNullOrEmpty(pipelineId))
            return DUUIRequestHelper.badRequest(response, "Missing pipeline.");
        if (!path.endsWith("xmi") && !path.endsWith("gz")) {
            response.status(400);
            return "Invalid file format.";
        }


        try {

            IDUUIDocumentHandler handler = DUUIProcessController.getHandler(provider, providerId, userId);
            if (handler == null) return DUUIRequestHelper.notFound(response);

            InputStream file = DUUIProcessController.downloadFile(handler, path);

            if (path.endsWith(CompressorStreamFactory.GZIP)) {
                file = new CompressorStreamFactory()
                        .createCompressorInputStream(
                                CompressorStreamFactory.GZIP,
                                file
                        );
            }

            JCas jcas = JCasFactory.createJCas();
            XmiCasDeserializer.deserialize(file,  jcas.getCas(), true);
            Set<String> annotationNames = new HashSet<>();

            List<Document> processed = JCasUtil.select(jcas, Annotation.class).stream()
                .sorted(Comparator.comparingInt(Annotation::getBegin))
                .peek(annotation -> annotationNames.add(annotation.getType().getName()))
                .map(annotation -> new Document()
                    .append("annotationType", annotation.getType().getName())
                    .append("begin", annotation.getBegin())
                    .append("end", annotation.getEnd())
                ).toList();


            response.status(200);

            return new Document("preprocessed", processed)
                    .append("text", jcas.getDocumentText())
                    .append("annotationNames", annotationNames)
                    .toJson();
        } catch (DbxException | IOException | GeneralSecurityException e) {
            response.status(500);
            return "The file could not be downloaded.";
        } catch (Exception e) {
            response.status(500);
            return "Document processing failed." + e.getClass() + ": " + e.getMessage();
        }

    }



    /**
     * Download a file given a cloud provider and a path.
     *
     * @return a response containing the file content as bytes.
     */
    public static String downloadFile(Request request, Response response) {
        String userId = DUUIRequestHelper.getUserId(request);
        String provider = request.queryParamOrDefault("provider", null);
        String providerId = request.queryParamOrDefault("provider_id", null);
        String path = request.queryParamOrDefault("path", null);

        if (DUUIRequestHelper.isNullOrEmpty(provider))
            return DUUIRequestHelper.badRequest(response, "Missing provider in query params.");
        if (DUUIRequestHelper.isNullOrEmpty(path))
            return DUUIRequestHelper.badRequest(response, "Missing path in query params.");

        try {
            IDUUIDocumentHandler handler = DUUIProcessController.getHandler(provider, providerId, userId);
            if (handler == null) return DUUIRequestHelper.notFound(response);

            InputStream file = DUUIProcessController.downloadFile(handler, path);
            response.type("application/octet-stream");
            response.raw().getOutputStream().write(file.readAllBytes());
            response.raw().getOutputStream().close();
            return "Download.";
        } catch (DbxException | IOException | GeneralSecurityException e) {
            response.status(500);
            return "The file could not be downloaded.";
        }
    }

    public static String getFilteredFolderStructure(Request request, Response response) {
        String rootPath = System.getProperty("user.home");
        String userId = DUUIRequestHelper.getUserId(request);

        List<Bson> pipeline = List.of(
                project(fields(
                        computed("groupsArr", new Document("$objectToArray", "$groups"))
                )),
                unwind("$groupsArr"),
                match(in("groupsArr.v.members", userId)),
                group(null, push("listOfLists", "$groupsArr.v.whitelist")),
                project(fields(
                        computed("combinedWhitelist",
                                new Document("$reduce", new Document("input", "$listOfLists")
                                        .append("initialValue", List.of())
                                        .append("in", new Document("$concatArrays", List.of("$$value", "$$this")))
                                )
                        )
                ))
        );

        Document result = DUUIMongoDBStorage.Globals().aggregate(pipeline).first();
        if (result != null) result = new Document("combinedWhitelist", List.of());
        List<Path> whitelist = result.getList("combinedWhitelist", String.class)
            .stream().map(Path::of).toList();

        try {
            IDUUIFolderPickerApi.DUUIFolder folder;
            DUUILocalDrivesDocumentHandler handler = new DUUILocalDrivesDocumentHandler(rootPath);
            folder = handler.getFolderStructure();
            folder = handler.filterTree(folder, whitelist);
            Document fs = new Document(folder.toJson());
            return fs.toJson();
        } catch (Exception e) {
            response.status(500);
            return "Failed to get folder structure: " + e.getMessage();
        }
    }

    public static String getLocalFolderStructure(Request request, Response response) {

        String rootPath = System.getProperty("user.home");
        boolean reset = request.queryParamOrDefault("reset", "false").equals("true");

        try {
            Document fs = getLFS(rootPath, reset);
            return fs.toJson();
        } catch (Exception e) {
            response.status(500);
            return "Failed to get folder structure: " + e.getMessage();
        }

    }

    private static Document getLFS(String rootPath, boolean reset) {

        Document fs = null;

        if (reset) {
            DUUIMongoDBStorage.Globals()
                    .findOneAndUpdate(
                            Filters.exists("settings"),
                            Updates.unset("settings.local_folder_structure")
                    );
        } else {
            Document result = DUUIMongoDBStorage.Globals().find(Filters.exists("settings")).first();
            if (result != null) {
                Document doc = result.getEmbedded(List.of("settings", "local_folder_structure"), Document.class);
                if (doc != null) fs = doc;
            }
        }

        if (fs == null) {
            IDUUIFolderPickerApi.DUUIFolder folder;
            try {
                DUUILocalDrivesDocumentHandler handler = new DUUILocalDrivesDocumentHandler(rootPath);
                folder = handler.getFolderStructure();

                fs = new Document(folder.toJson());
                DUUIMongoDBStorage.Globals()
                        .findOneAndUpdate(
                                Filters.exists("settings"),
                                Updates.set("settings.local_folder_structure", fs)
                        );
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        return fs;
    }

    public static String updateSettings(Request request, Response response) {
        
        Document settings = Document.parse(request.body());

        if (settings.containsKey("allowed_origins")) {
            List<String> allowedOrigins = settings.getList("allowed_origins", String.class);
            settings.put("allowed_origins", String.join(";", allowedOrigins));
        }

        settings = DUUIMongoDBStorage.Globals()
            .findOneAndUpdate(
                Filters.exists("settings"),
                Updates.set("settings", settings),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
            );
        
        if (settings == null) {
            throw new RuntimeException("Failed to create settings document");
        }
        
        if (settings.get("settings", Document.class) != null) {
            settings = settings.get("settings", Document.class);
        }

        return settings.toJson();
    }

    public static String getSettings(Request request, Response response) {
        String settings = DUUIMongoDBStorage.Settings().toJson();
        
        // System.out.println("Settings: " + settings);

        return settings;
    }
}
