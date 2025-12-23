package org.texttechnologylab.duui.api.controllers.events;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIContext;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIContext.*;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIEvent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIStatus;
import org.texttechnologylab.duui.api.controllers.documents.DUUIDocumentController;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;
import org.texttechnologylab.duui.api.storage.DUUIMongoDBStorage;
import org.texttechnologylab.duui.api.websocket.ProcessWebSocketRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Controller for database operations related to the events collection.
 *
 * @author Cedric Borkowski
 */
public class DUUIEventController {

    /**
     * Delete all events matching a given filter
     *
     * @param filter A {@link Bson} filter to delete only selected events
     */
    public static void deleteMany(Bson filter) {
        DUUIMongoDBStorage
            .Events()
            .deleteMany(filter);
    }

    /**
     * Find one or more events that reference a document by id.
     *
     * @param documentId The id of the document an event must reference.
     * @return A List of Documents.
     */
    public static List<Document> findManyByDocument(String documentId) {
        Document document = DUUIDocumentController.findOne(documentId);
        if (document == null) return new ArrayList<>();

        String processId = document.getString("process_id");
        String path = document.getString("path");
        AggregateIterable<Document> result = DUUIMongoDBStorage
            .Events()
            .aggregate(
                List.of(
                    Aggregates.match(
                        Filters.and(
                            Filters.eq("event.process_id", processId),
                            Filters.or(
                                Filters.eq("event.context.document", path),
                                new Document(
                                    "event.message",
                                    Pattern.compile(Pattern.quote(path), Pattern.CASE_INSENSITIVE))
                            )
                        )
                    ),
                    Aggregates.sort(Sorts.ascending("timestamp"))
                )
            );

        return result.into(new ArrayList<>());
    }

    public static List<Document> findManyByProcess(String process_id) {
        FindIterable<Document> timeline = DUUIMongoDBStorage
            .Events()
            .find(Filters.eq("event.process_id", process_id));

        List<Document> events = timeline.into(new ArrayList<>());
        events.forEach(DUUIMongoDBStorage::convertObjectIdToString);
        events.forEach(event -> DUUIMongoDBStorage.convertDateToTimestamp(event, "timestamp"));
        return events;
    }

    /**
     * Insert one or more events that reference a process.
     *
     * @param processId The id of the process an event must reference
     * @param events    The list of events to insert.
     */
    public static void insertMany(String processId, List<DUUIEvent> events) {

        if (events.isEmpty()) return;

        Function<DUUIEvent, Document> serializeEvent = (DUUIEvent event) -> {
            Document eventDoc = new Document("_id", processId + "_" + getEventId(event))
            
                .append("process_id", processId)
                .append("sender", event.getSender() != null ? event.getSender().name() : null)
                .append("message", event.getMessage())
                .append("level", event.getDebugLevel().name())
                .append("context", merge(
                    serialize(event.getContext()),
                        serialize(event.getContext().payloadRecord()))
                            .append("kind", event.getContext().getClass().getSimpleName())
                            .append("event_id", processId + "_" + getEventId(event))
                );
            
            return new Document()
                .append("timestamp", event.getTimestamp())
                .append("event", eventDoc);
        }; 

        List<DUUIEvent> inserts = new ArrayList<>(events);

        // Prepare MongoDB documents

        // Insert into MongoDB
        DUUIMongoDBStorage
            .Events()
            .insertMany(
                events
                    .stream()
                    .map(serializeEvent)
                    .collect(Collectors.toList())
        );

        // Broadcast to WebSocket subscribers as DUUIEvent-shaped JSON
        ProcessWebSocketRegistry registry = ProcessWebSocketRegistry.getInstance();
        inserts.stream().map(serializeEvent).forEach(e -> registry.broadcast(processId, e.toJson()));

        events.removeAll(inserts);
    }

    private static String getEventId(DUUIEvent event) {
        if (event == null) return null;
        return event.getClass().getName() + "@" + System.identityHashCode(event);
    }

    private static Document serialize(Payload payload) {
        return switch (payload) {
            case Payload(DUUIStatus status, String content, PayloadKind kind, String thread) 
                when kind != PayloadKind.NONE -> 
                    new Document("payload", 
                            new Document("content", content)
                                .append("type", kind.name()))
                        .append("status", status.value())
                        .append("thread", thread);
            case Payload(DUUIStatus status, String content, PayloadKind kind, String thread) -> 
                    new Document()
                        .append("status", status.value())
                        .append("thread", thread);
            default -> new Document(); 
        };
    }

    private static Document serialize(DUUIContext context) {
        return switch (context) {
            case ComposerContext(var runKey, var pipelineStatus, var progress, var total, var payload) ->
                new Document()
                    .append("runKey", runKey)
                    .append("pipeline_status", 
                    pipelineStatus.entrySet().stream()
                        .collect(Collectors.toMap(
                            java.util.Map.Entry::getKey, 
                            e -> DUUIRequestHelper.toTitleCase(e.getValue().name())))
                        )
                    .append("progress", progress.get())
                    .append("total", total);
            case DriverContext(var driver, var payload) ->
                new Document("driver", driver);
            case WorkerContext(var composer, var name, var activeWorkers, var payload) ->
                new Document("composer", composer)
                .append("name", name)
                .append("activeWorkers", activeWorkers.get());
            case DocumentContext(var document, var payloadRecord) ->
                new Document()
                .append("path", document.getPath())
                .append("name", document.getName())
                .append("size", document.getSize())
                .append("progress", document.getProgess().get())
                .append("error", document.getError())
                .append("is_finished", document.isFinished())
                .append("duration_decode", document.getDurationDecode())
                .append("duration_deserialize", document.getDurationDeserialize())
                .append("duration_wait", document.getDurationWait())
                .append("duration_process", document.getDurationProcess())
                .append("started_at", document.getStartedAt())
                .append("finished_at", document.getFinishedAt());
            case DocumentProcessContext(var document, var composer, var payloadRecord) ->
                new Document("document", document)
                    .append("composer", composer);
            case ComponentContext(var uuid, var name, var driver, var instanceIds, var payload) ->
                new Document()
                    .append("component", uuid)
                    .append("name", name)
                    .append("driver", driver)
                    .append("instance_ids", instanceIds);
            case InstantiatedComponentContext(var component, var instanceId, var endpoint, var payload) ->
                new Document("component",component)
                    .append("instance_id", instanceId)
                    .append("endpoint", endpoint);
            case DocumentComponentProcessContext(var document, var component, var payload) ->
                new Document("document", document)
                    .append("instantiated_component", component);
            case ReaderContext(var reader, var payload) ->
                new Document()
                    .append("total", reader.getInitial())
                    .append("skipped", reader.getSkipped())
                    .append("read", reader.getDone())
                    .append("remaining", reader.getSize())
                    .append("used_bytes", reader.getCurrentMemorySize())
                    .append("total_bytes", reader.getMaximumMemory())
                ;
            default ->
                new Document();
        };
    }

    private static Document merge(Document... docs) {
        Document out = new Document();
        for (Document d : docs) out.putAll(d);
        return out;
    }
}
