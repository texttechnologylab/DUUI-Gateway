package org.texttechnologylab.duui.analysis.process;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.bson.Document;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.document_handler.DUUIDropboxDocumentHandler;
import org.texttechnologylab.DockerUnifiedUIMAInterface.document_handler.DUUILocalDrivesDocumentHandler;
import org.texttechnologylab.DockerUnifiedUIMAInterface.document_handler.IDUUIDocumentHandler;
import org.texttechnologylab.DockerUnifiedUIMAInterface.document_handler.IDUUIFolderPickerApi;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIDocumentReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIEvent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIProcess;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIStatus;
import org.texttechnologylab.duui.analysis.document.DUUIDocumentProvider;
import org.texttechnologylab.duui.analysis.document.Provider;
import org.texttechnologylab.duui.api.Main;
import org.texttechnologylab.duui.api.controllers.documents.DUUIDocumentController;
import org.texttechnologylab.duui.api.controllers.events.DUUIEventController;
import org.texttechnologylab.duui.api.controllers.pipelines.DUUIPipelineController;
import org.texttechnologylab.duui.api.controllers.processes.DUUIProcessController;
import org.texttechnologylab.duui.api.controllers.users.DUUIUserController;
import org.texttechnologylab.duui.api.metrics.providers.DUUIProcessMetrics;
import org.texttechnologylab.duui.api.websocket.ProcessWebSocketRegistry;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.WriteMode;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

/**
 * The default ProcessHandler implementing the {@link IDUUIProcessHandler} interface.
 * A process runs immediately after creating an instance of this class.
 *
 * @author Cedric Borkowski
 */
public class DUUISimpleProcessHandler extends Thread implements IDUUIProcessHandler {

    /**
     * The composer for handling the process.
     */
    private final DUUIComposer composer;

    /**
     * The document containing process relevant information.
     */
    private final Document process;

    /**
     * The document containing information about the pipeline to be executed by the process.
     */
    private final Document pipeline;

    /**
     * The document containing process specific settings that alter its behavior.
     */
    private final Document settings;

    /**
     * The current status of the process.
     */
    private DUUIStatus status = DUUIStatus.SETUP;

    /**
     * The input document provider.
     */
    private DUUIDocumentProvider input;

    /**
     * The output document provider.
     */
    private DUUIDocumentProvider output;

    /**
     * The handler for the input document.
     */
    private IDUUIDocumentHandler inputHandler;

    /**
     * The handler for the output document.
     */
    private IDUUIDocumentHandler outputHandler;

    /**
     * The document reader for the collection.
     */
    private DUUIDocumentReader collectionReader;

    /**
     * Flag indicating whether to delete the input directory after processing.
     */
    private boolean deleteInputDirectory = false;

    /**
     * The number of threads currently in use.
     */
    private int threadCount = 0;

    /**
     * The maximum number of worker threads allowed.
     */
    private int maximumWorkerCount = 1;

    /**
     * Indicates whether to shut down on exit.
     */
    private final boolean shutdownOnExit;

    /**
     * Run a process using the specified settings and pipeline. The pipeline is instantiated specifically
     * for this process.
     *
     * @param process  A {@link Document} containing process relevant information.
     * @param pipeline A {@link Document} containing information about the pipeline to be executed by the process.
     * @param settings A {@link Document} containing process specific settings that alter its behavior.
     * @throws URISyntaxException Thrown when the minimal TypeSystem can not be loaded.
     * @throws IOException        Thrown when the Lua Json Library can not be loaded.
     */
    public DUUISimpleProcessHandler(Document process, Document pipeline, Document settings) throws URISyntaxException, IOException {
        this.pipeline = pipeline;
        this.process = process;
        this.settings = settings;

        boolean ignoreErrors = settings.getBoolean("ignore_errors", true);

        composer = new DUUIComposer()
            .withSkipVerification(true)
            .withDebugLevel(DUUIComposer.DebugLevel.NONE)
            .withIgnoreErrors(ignoreErrors)
//            TODO versions of DUUI and API are incompatible
//            .withStorageBackend(
//                new DUUIMongoDBStorageBackend(
//                    DUUIMongoDBStorage.getConnectionURI()))
            .withLuaContext(new DUUILuaContext().withJsonLibrary());

        composer.addProcessObserver(this::onProcessUpdates);

        shutdownOnExit = true;
        start();
    }

    /**
     * Run a process using the specified settings and pipeline. The pipeline is not instantiated but an
     * instantiated pipeline must be passed in the constructor.
     *
     * @param process              A {@link Document} containing process relevant information.
     * @param pipeline             A {@link Document} containing information about the pipeline to be executed by the process.
     * @param settings             A {@link Document} containing process specific settings that alter its behavior.
     * @param instantiatedPipeline A {@link Vector} holding the instantiated pipeline.
     * @throws URISyntaxException Thrown when the minimal TypeSystem can not be loaded.
     * @throws IOException        Thrown when the Lua Json Library can not be loaded.
     */
    public DUUISimpleProcessHandler(
        Document pipeline,
        Document process,
        Document settings,
        Vector<DUUIComposer.PipelinePart> instantiatedPipeline) throws URISyntaxException, IOException {
        this.pipeline = pipeline;

        composer = new DUUIComposer()
            .withInstantiatedPipeline(instantiatedPipeline)
            .withSkipVerification(true)
            .withDebugLevel(DUUIComposer.DebugLevel.NONE)
            .asService(true)
            .withLuaContext(new DUUILuaContext().withJsonLibrary());

        this.process = process;
        this.settings = settings;

        input = new DUUIDocumentProvider(process.get("input", Document.class));
        output = new DUUIDocumentProvider(process.get("output", Document.class));

        composer.addProcessObserver(this::onProcessUpdates);

        shutdownOnExit = false;
        start();
    }


    /**
     * Dispatches the process configuration to the composer which starts it.
     */
    @Override
    public void startInput() {
        DUUIProcessController.setStatus(getProcessID(), DUUIStatus.INPUT);
        status = DUUIStatus.INPUT;

        input = new DUUIDocumentProvider(process.get("input", Document.class));
        output = new DUUIDocumentProvider(process.get("output", Document.class));

        try {
            inputHandler = DUUIProcessController.getHandler(input.getProvider(), input.getProviderId(), getUserID());
        } catch (IllegalArgumentException | DbxException | GeneralSecurityException | IOException  exception) {
            onException(exception);
        }

        try {
            outputHandler = Objects.equals(input, output) && Objects.equals(output.getProviderId(), input.getProviderId())
                ? inputHandler
                : DUUIProcessController.getHandler(output.getProvider(), output.getProviderId(), getUserID());
            if (outputHandler != null && output.getProvider().equals(Provider.DROPBOX)) {
                DUUIDropboxDocumentHandler dropboxDataReader = (DUUIDropboxDocumentHandler) outputHandler;
                dropboxDataReader
                    .setWriteMode(
                        settings.getBoolean("overwrite", false)
                            ? WriteMode.OVERWRITE : WriteMode.ADD);
            }
        } catch (Exception exception) {
            onException(exception);
        }


        if (input.isText()) {
            deleteInputDirectory = true;

            String rawName = Optional.ofNullable(pipeline.getString("name")).orElse("pipeline");
            String safeName = rawName
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

            if (safeName.isEmpty()) {
                safeName = "pipeline";
            }

            String tempFileName = String.format(
                "%s_%s.txt",
                safeName,
                System.currentTimeMillis()
            );

            if (!output.getProvider().equals(Provider.LOCAL_DRIVE)) {
                inputHandler = new DUUILocalDrivesDocumentHandler(Main.config.getLocalDriveRoot());
            } else {
                inputHandler = outputHandler;
            }

            // Create a temporary file to hold the text content
            String uuid = UUID.randomUUID().toString();
            Path root = Paths.get(Main.config.getLocalDriveRoot(), "." + uuid);
            try {
                java.nio.file.Files.createDirectories(root);
                Path tempFile = root.resolve(tempFileName);
                java.nio.file.Files.write(
                    tempFile,
                    input.getContent().getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException e) {
                onException(new Exception("Error creating directory: " + e.getMessage(), e));
                return;
            }

            input = new DUUIDocumentProvider(
                Provider.LOCAL_DRIVE,
                input.getProviderId(),
                root.toString(),
                null,
                input.getFileExtension()
            );

        }

        try {
            DUUIDocumentReader.Builder builder = new DUUIDocumentReader
                    .Builder(composer);

            if (inputHandler instanceof  IDUUIFolderPickerApi) {
                builder.withInputPaths(List.of(input.getPath().split(",")));
            } else {
                builder.withInputPath(input.getPath());
            }

            collectionReader = builder
                .withInputFileExtension(input.getFileExtension())
                .withInputHandler(inputHandler)
                .withOutputPath(output.getPath())
                .withOutputFileExtension(output.getFileExtension())
                .withOutputHandler(outputHandler)
                .withAddMetadata(true)
                .withLanguage(DUUIProcessController.getLanguageCode(settings.getString("language")))
                .withMinimumDocumentSize(Math.max(0, Math.min(Integer.MAX_VALUE, settings.getInteger("minimum_size"))))
                .withSortBySize(settings.getBoolean("sort_by_size", false))
                .withCheckTarget(settings.getBoolean("check_target", false))
                .withRecursive(settings.getBoolean("recursive", false))
                .build();

            DUUIProcessController.setDocumentPaths(getProcessID(), composer.getDocumentPaths());

            if (composer.getDocuments().isEmpty()) {
                onCompletion();
                exit();
            } else {
                DUUIProcessController.updateOne(getProcessID(), "initial", collectionReader.getInitial());
                DUUIProcessController.updateOne(getProcessID(), "skipped", collectionReader.getSkipped());
            }

            maximumWorkerCount = composer.getDocuments().size();

        } catch (Exception exception) {
            onException(exception);
        }
    }

    /**
     * Processes the text input.
    */
    @Deprecated
    @Override
    public void processText() {
        try {
            JCas cas = JCasFactory.createText(input.getContent());

            String processIdentifier = String.format(
                "%s_%s",
                pipeline.getString("name"),
                process.getLong("started_at")
            );

            if (JCasUtil.select(cas, DocumentMetaData.class).isEmpty()) {
                DocumentMetaData dmd = DocumentMetaData.create(cas);
                dmd.setDocumentId(pipeline.getString("name"));
                dmd.setDocumentTitle(pipeline.getString("name"));
                dmd.setDocumentUri(processIdentifier);
                dmd.addToIndexes();
            }

            cas.setDocumentLanguage(DUUIProcessController.getLanguageCode(settings.getString("language")));

            composer.addEvent(DUUIEvent.Sender.READER, "Starting Pipeline");
            DUUIProcessController.setStatus(getProcessID(), DUUIStatus.ACTIVE);
            status = DUUIStatus.ACTIVE;

            composer.run(cas, processIdentifier);

        } catch (InterruptedException ignored) {
            status = DUUIStatus.CANCELLED;
        } catch (Exception exception) {
            onException(exception);
        }
    }

    /**
     * Processes the collection.
     */
    @Override
    public void process() {
        String processIdentifier = String.format(
            "%s_%s",
            pipeline.getString("name"),
            process.getLong("started_at")
        );
        try {
            composer.run(collectionReader, processIdentifier);
        } catch (InterruptedException ignored) {
            status = DUUIStatus.CANCELLED;
        } catch (Exception exception) {
            onException(exception);
        }
    }


    /**
     * Updates the process status.
     */
    @Override
    public void update() {
        // Live state is persisted via DUUIProcess observer (onProcessUpdates).
    }

    /**
     * Handles exceptions that occur during the process.
     *
     * @param exception The exception that occurred.
     */
    @Override
    public void onException(Exception exception) {
        DUUIProcessController.setStatus(getProcessID(), DUUIStatus.FAILED);
        DUUIProcessController.setError(
            getProcessID(),
            String.format("%s - %s", exception.getClass().getCanonicalName(), exception.getMessage()));

        DUUIProcessController.setFinishedAt(getProcessID());
        DUUIProcessController.setFinished(getProcessID(), true);


        composer.getDocuments().stream().filter(document ->
            !document.isFinished() || DUUIDocumentController.isActive(document)).forEach(document -> {
                document.setStatus(DUUIStatus.FAILED);
                document.setFinished(true);
                document.setFinishedAt();
            }
        );

        DUUIProcessMetrics.incrementErrorCount(1);
        DUUIProcessMetrics.incrementFailedProcesses();
        exit();
    }

    /**
     * Handles the completion of the process.
     */
    @Override
    public void onCompletion() {
        if (status.equals(DUUIStatus.CANCELLED)) return;

        status = DUUIStatus.COMPLETED;
        DUUIProcessController.setStatus(getProcessID(), DUUIStatus.COMPLETED);
        DUUIProcessMetrics.incrementCompletedProcesses();

        DUUIProcessController.setFinished(getProcessID(), true);
        DUUIProcessController.setFinishedAt(getProcessID());

        composer
            .getDocuments()
            .stream()
            .filter(document -> !DUUIStatus.oneOf(document.getStatus(), DUUIStatus.FAILED, DUUIStatus.CANCELLED))
            .forEach(document -> document.setStatus(DUUIStatus.COMPLETED));

    }

    /**
     * Cancels the process.
     */
    @Override
    public void cancel() {
        status = DUUIStatus.CANCELLED;
        DUUIProcessMetrics.incrementCancelledProcesses();

        DUUIProcessController.setStatus(getProcessID(), DUUIStatus.SHUTDOWN);
        composer.interrupt();

        DUUIProcessController.setStatus(getProcessID(), DUUIStatus.CANCELLED);
        DUUIProcessController.setFinishedAt(getProcessID());
        DUUIProcessController.setFinished(getProcessID(), true);

        composer.setFinished(true);
        composer.getDocuments().stream().filter(document ->
            !document.isFinished() || DUUIDocumentController.isActive(document)).forEach(document -> {
                document.setStatus(DUUIStatus.CANCELLED);
                document.setFinished(true);
                document.setFinishedAt();
            }
        );

        exit();
    }

    /**
     * Shuts down the process.
     *
     * Active connections to cloud services are closed.
     */
    @Override
    public void shutdown() {
        inputHandler.shutdown();
        if (outputHandler != inputHandler) outputHandler.shutdown();
    }

    /**
     * Exits the process.
     */
    @Override
    public void exit() {
        DUUIProcessMetrics.decrementActiveProcesses();

        deleteTemporaryInputDirectory();

        if (input.getProvider().equals(Provider.FILE)) {
            try {
                if (DUUIProcessController.deleteTempOutputDirectory(
                    new File(Paths.get(input.getPath()).toString())
                )) {
                    composer.addEvent(DUUIEvent.Sender.SYSTEM, "Clean up complete");
                }
            } catch (Exception ignored) {
            }
        }

        if (composer != null) {
            DUUIUserController.addToWorkerCount(getUserID(), threadCount);
            DUUIProcessMetrics.decrementThreads(threadCount);

            try {
                composer.asService(!shutdownOnExit).shutdown();
            } catch (UnknownHostException | NullPointerException ignored) {
            }

            DUUIProcessController.removeProcess(getProcessID());

            // TODO: Add a method to the DUUIComposer to remove the installed shutdown hook...
        }

        try {
            Optional<String> emailContent = DUUIProcessController.getProcessSummaryForEmail(getProcessID());
            if (emailContent.isPresent()) {
                // TODO: Send E-Mail
            }
        } catch (IOException ignored) {

        }
        
        threadCount = 0;
        interrupt();
    }

    private void onProcessUpdates(DUUIEvent event, List<DUUIProcess.Update> updates) {
        if (event == null || updates == null || updates.isEmpty()) return;

        String processId = getProcessID();
        if (processId == null || processId.isBlank()) return;

        Document eventMessage = DUUIEventController.insert(processId, event);
        if (eventMessage != null) {
            ProcessWebSocketRegistry.getInstance().broadcast(processId, eventMessage.toJson());
        }

        applyUpdatesToMongo(processId, updates);

        Document updateMessage = serializeUpdateMessage(processId, updates);
        ProcessWebSocketRegistry.getInstance().broadcast(processId, updateMessage.toJson());
    }

    private void applyUpdatesToMongo(String processId, List<DUUIProcess.Update> updates) {
        for (DUUIProcess.Update u : updates) {
            if (u == null) continue;
            switch (u) {
                case DUUIProcess.Update.ProcessUpdate(var meta, var processDoc) -> {
                    DUUIProcessController.updateFields(processId, processDoc);
                }
                case DUUIProcess.Update.WorkerUpsert(var meta, var workerName, var workerDoc) -> {
                    DUUIProcessController.upsertPath(
                        processId,
                        "process_state.workers." + escapeMongoKey(workerName),
                        workerDoc
                    );
                }
                case DUUIProcess.Update.DriverUpsert(var meta, var driverName, var driverDoc) -> {
                    DUUIProcessController.upsertPath(
                        processId,
                        "process_state.drivers." + escapeMongoKey(driverName),
                        driverDoc
                    );
                }
                case DUUIProcess.Update.ComponentUpsert(var meta, var componentId, var componentDoc) -> {
                    DUUIProcessController.upsertPath(
                        processId,
                        "process_state.components." + escapeMongoKey(componentId),
                        componentDoc
                    );
                }
                case DUUIProcess.Update.InstanceUpsert(var meta, var componentId, var instanceId, var instanceDoc) -> {
                    DUUIProcessController.upsertPath(
                        processId,
                        "process_state.components." + escapeMongoKey(componentId) + ".instances." + escapeMongoKey(instanceId),
                        instanceDoc
                    );
                }
                case DUUIProcess.Update.DocumentUpsert(var meta, var documentKey, var documentDoc) -> {
                    DUUIDocumentController.upsertState(processId, documentKey, documentDoc);
                }
                default -> {
                }
            }
        }
    }

    private static String escapeMongoKey(String raw) {
        if (raw == null) return "_";
        return raw
            .replace(".", "\uFF0E")
            .replace("$", "\uFF04");
    }

    private Document serializeUpdateMessage(String processId, List<DUUIProcess.Update> updates) {
        return new Document("kind", "update")
            .append("process_id", processId)
            .append("updates", updates.stream().map(this::serializeUpdate).toList());
    }

    private Document serializeUpdate(DUUIProcess.Update u) {
        return switch (u) {
            case DUUIProcess.Update.ProcessUpdate(var meta, var processDoc) ->
                new Document("kind", "ProcessUpdate")
                    .append("meta", serializeMeta(meta))
                    .append("process", processDoc);
            case DUUIProcess.Update.WorkerUpsert(var meta, var workerName, var workerDoc) ->
                new Document("kind", "WorkerUpsert")
                    .append("meta", serializeMeta(meta))
                    .append("workerName", workerName)
                    .append("worker", workerDoc);
            case DUUIProcess.Update.DriverUpsert(var meta, var driverName, var driverDoc) ->
                new Document("kind", "DriverUpsert")
                    .append("meta", serializeMeta(meta))
                    .append("driverName", driverName)
                    .append("driver", driverDoc);
            case DUUIProcess.Update.ComponentUpsert(var meta, var componentId, var componentDoc) ->
                new Document("kind", "ComponentUpsert")
                    .append("meta", serializeMeta(meta))
                    .append("componentId", componentId)
                    .append("component", componentDoc);
            case DUUIProcess.Update.InstanceUpsert(var meta, var componentId, var instanceId, var instanceDoc) ->
                new Document("kind", "InstanceUpsert")
                    .append("meta", serializeMeta(meta))
                    .append("componentId", componentId)
                    .append("instanceId", instanceId)
                    .append("instance", instanceDoc);
            case DUUIProcess.Update.DocumentUpsert(var meta, var documentKey, var documentDoc) ->
                new Document("kind", "DocumentUpsert")
                    .append("meta", serializeMeta(meta))
                    .append("documentKey", documentKey)
                    .append("document", documentDoc);
            default ->
                new Document("kind", "Unknown");
        };
    }

    private Document serializeMeta(DUUIProcess.Update.Meta meta) {
        return new Document("runKey", meta.runKey())
            .append("eventId", meta.eventId())
            .append("timestamp", meta.timestamp());
    }
    
    /**
     * Deletes the temporary input directory if the flag is set.
     */
    private void deleteTemporaryInputDirectory() {
        if (deleteInputDirectory) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(Path.of(input.getPath()).toFile());
            } catch (IOException e) {
                System.err.println("Error deleting temporary input directory: " + e.getMessage());
            }
        }
    }

    /**
     * Handles the server stopping.
     */
    @Override
    public void onServerStopped() {
        cancel();
    }

    /**
     * Returns the process ID.
     *
     * @return The process ID.
     */
    @Override
    public String getProcessID() {
        return process.getString("oid");
    }

    /**
     * Returns the pipeline ID.
     *
     * @return The pipeline ID.
     */
    @Override
    public String getPipelineID() {
        return pipeline.getString("oid");
    }

    /**
     * Returns the user ID.
     *
     * @return The user ID.
     */
    @Override
    public String getUserID() {
        return pipeline.getString("user_id");
    }

    /**
     * Returns the status of the process.
     *
     * @return The status of the process.
     */
    @Override
    public DUUIStatus getStatus() {
        return status;
    }

    /**
     * Returns the composer.
     *
     * @return The composer.
     */
    @Override
    public void run() {
        DUUIProcessMetrics.incrementActiveProcesses();

        startInput();
        if (status.equals(DUUIStatus.COMPLETED)) return;

        if (shutdownOnExit) {
            DUUIProcessController.setStatus(getProcessID(), DUUIStatus.SETUP);
            try {
                DUUIPipelineController.setupDrivers(composer, pipeline);
                DUUIPipelineController.setupComponents(composer, pipeline);
            } catch (Exception exception) {
                onException(exception);
            }
        }

        int requestedWorkers = settings.getInteger("worker_count");
        int availableWorkers = DUUIUserController
            .getUserById(getUserID(), List.of("worker_count"))
            .getInteger("worker_count");

        if (availableWorkers == 0) {
            onException(new IllegalStateException(
                "This Account is out of workers for now. Wait until your other processes have finished."));
        }

        threadCount = Math.max(1, Math.min(input.isText() ? 1 : requestedWorkers, availableWorkers));
        threadCount = Math.min(threadCount, maximumWorkerCount);
        DUUIUserController.addToWorkerCount(getUserID(), -threadCount);
        composer.withWorkers(threadCount);
        DUUIProcessMetrics.incrementThreads(threadCount);

        if (status.equals(DUUIStatus.CANCELLED)) {
            exit();
            return;
        }

        DUUIProcessController.setStatus(getProcessID(), DUUIStatus.ACTIVE);

        // Start processing
        process();

        DUUIProcessController.setInstantiationDuration(getProcessID(), composer.getInstantiationDuration());

        if (status.equals(DUUIStatus.CANCELLED)) {
            exit();
            return;
        }

        onCompletion();

        exit();
    }
}
