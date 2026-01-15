package org.texttechnologylab.duui.api.controllers.events;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIContext;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIContext.Payload;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIContext.PayloadKind;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIEvent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIStatus;
import org.texttechnologylab.duui.api.controllers.documents.DUUIDocumentController;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;
import org.texttechnologylab.duui.api.storage.DUUIMongoDBStorage;
import org.texttechnologylab.duui.api.websocket.ProcessWebSocketRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    public static Document insert(String processId, DUUIEvent event) {
        if (processId == null || processId.isBlank() || event == null) return null;
        Document stored = serialize(processId, event);
        
        try {
            DUUIMongoDBStorage
                .Events()
                .updateOne(
                    Filters.eq("event.event_id", stored.get("event", Document.class).getString("event_id")),
                    new Document("$setOnInsert", stored),
                    new com.mongodb.client.model.UpdateOptions().upsert(true)
                );
        } catch (Exception ignored) {
        }

        return new Document("kind", "event")
            .append("process_id", processId)
            .append("timestamp", stored.getLong("timestamp"))
            .append("event", stored.get("event", Document.class));
    }

    /**
     * Insert one or more events that reference a process.
     *
     * @param processId The id of the process an event must reference
     * @param events    The list of events to insert.
     */
    public static void insertMany(String processId, List<DUUIEvent> events) {
        if (events == null || events.isEmpty() || processId == null || processId.isBlank()) return;

        List<DUUIEvent> inserts = new ArrayList<>(events);
        ProcessWebSocketRegistry registry = ProcessWebSocketRegistry.getInstance();
        for (DUUIEvent e : inserts) {
            Document msg = insert(processId, e);
            if (msg != null) {
                registry.broadcast(processId, msg.toJson());
            }
        }
        events.removeAll(inserts);
    }

    private static String eventId(String processId, DUUIEvent event) {
        if (processId == null || processId.isBlank() || event == null) return null;
        return processId + "_" + event.getId();
    }

    private static Document serialize(Payload payload) {
        if (payload == null) return new Document();
        Document out = new Document()
            .append("status", payload.status() == null ? DUUIStatus.UNKNOWN.value() : payload.status().value())
            .append("thread", payload.thread());
        if (payload.kind() != null && payload.kind() != PayloadKind.NONE) {
            out.append("payload",
                new Document("content", payload.content())
                    .append("type", payload.kind().name()));
        };
        return out;
    }

    private static Document serialize(DUUIContext context, String eventId) {
        if (context == null) return new Document();

        Document base = serialize(context.payloadRecord())
            .append("event_id", eventId)
            .append("kind", context.getClass().getSimpleName());

        return switch (context) {
            case DUUIContext.DefaultContext _ctx ->
                base;
            case DUUIContext.ComposerContext ctx ->
                base
                    .append("runKey", ctx.runKey())
                    .append("pipeline_status",
                        ctx.pipelineStatus().entrySet().stream()
                            .collect(Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                e -> e.getValue() == null ? DUUIStatus.UNKNOWN.value() : DUUIRequestHelper.toTitleCase(e.getValue().name())
                            )))
                    .append("documentCount", ctx.documentCount());
            case DUUIContext.WorkerContext ctx ->
                base
                    .append("name", ctx.name())
                    .append("activeWorkers", ctx.activeWorkers().get())
                    .append("composer", serialize(ctx.composer(), eventId));
            case DUUIContext.DriverContext ctx ->
                base.append("driver", ctx.driverName());
            case DUUIContext.DocumentContext ctx -> {
                var doc = ctx.document();
                yield base
                    .append("path", doc.getPath())
                    .append("name", doc.getName())
                    .append("size", doc.getSize())
                    .append("progress", doc.getProgess().get())
                    .append("error", doc.getError())
                    .append("is_finished", doc.isFinished())
                    .append("duration_decode", doc.getDurationDecode())
                    .append("duration_deserialize", doc.getDurationDeserialize())
                    .append("duration_wait", doc.getDurationWait())
                    .append("duration_process", doc.getDurationProcess())
                    .append("started_at", doc.getStartedAt())
                    .append("finished_at", doc.getFinishedAt());
            }
            case DUUIContext.DocumentProcessContext ctx ->
                base
                    .append("document", serialize(ctx.document(), eventId))
                    .append("composer", serialize(ctx.composer(), eventId));
            case DUUIContext.ComponentContext ctx ->
                base
                    .append("component", ctx.componentId())
                    .append("name", ctx.componentName())
                    .append("driver", ctx.driverName())
                    .append("instance_ids", ctx.instanceIds());
            case DUUIContext.InstantiatedComponentContext ctx ->
                base
                    .append("component", serialize(ctx.component(), eventId))
                    .append("instance_id", ctx.instanceId())
                    .append("endpoint", ctx.endpoint());
            case DUUIContext.DocumentComponentProcessContext ctx ->
                base
                    .append("document", serialize(ctx.document(), eventId))
                    .append("instantiated_component", serialize(ctx.component(), eventId));
            case DUUIContext.ReaderContext ctx ->
                base
                    .append("total", ctx.reader().getInitial())
                    .append("skipped", ctx.reader().getSkipped())
                    .append("read", ctx.reader().getDone())
                    .append("remaining", ctx.reader().getSize())
                    .append("used_bytes", ctx.reader().getCurrentMemorySize())
                    .append("total_bytes", ctx.reader().getMaximumMemory());
            case DUUIContext.ReaderDocumentContext ctx ->
                base
                    .append("reader", serialize(ctx.reader(), eventId))
                    .append("document", serialize(ctx.document(), eventId));
        };
    }

    private static Document serialize(String processId, DUUIEvent event) {
        String id = eventId(processId, event);
        List<String> scopeKeys = deriveScopeKeys(processId, event.getContext());

        Document eventDoc = new Document("event_id", id)
            .append("process_id", processId)
            .append("scope_keys", scopeKeys)
            .append("sender", event.getSender() != null ? event.getSender().name() : null)
            .append("message", event.getMessage())
            .append("level", event.getDebugLevel().name())
            .append("context", serialize(event.getContext(), id));

        return new Document()
            .append("timestamp", event.getTimestamp())
            .append("event", eventDoc);
    }

    private static List<String> deriveScopeKeys(String processId, DUUIContext ctx) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (processId != null && !processId.isBlank()) keys.add("process:" + processId);
        addContextKeys(keys, ctx);
        return List.copyOf(keys);
    }

    private static void addContextKeys(Set<String> out, DUUIContext ctx) {
        if (ctx == null) return;
        switch (ctx) {
            case DUUIContext.ComposerContext c -> {
                if (c.runKey() != null && !c.runKey().isBlank()) out.add("run:" + c.runKey());
            }
            case DUUIContext.WorkerContext w -> {
                if (w.name() != null && !w.name().isBlank()) out.add("worker:" + w.name());
                addContextKeys(out, w.composer());
            }
            case DUUIContext.DriverContext d -> {
                if (d.driverName() != null && !d.driverName().isBlank()) out.add("driver:" + d.driverName());
            }
            case DUUIContext.ComponentContext c -> {
                if (c.componentId() != null && !c.componentId().isBlank()) out.add("component:" + c.componentId());
                if (c.driverName() != null && !c.driverName().isBlank()) out.add("driver:" + c.driverName());
            }
            case DUUIContext.InstantiatedComponentContext i -> {
                if (i.instanceId() != null && !i.instanceId().isBlank()) out.add("instance:" + i.instanceId());
                addContextKeys(out, i.component());
            }
            case DUUIContext.DocumentContext d -> {
                String path = d.document() != null ? d.document().getPath() : null;
                if (path != null && !path.isBlank()) out.add("document:" + path);
            }
            case DUUIContext.DocumentProcessContext dp -> {
                addContextKeys(out, dp.document());
                addContextKeys(out, dp.composer());
            }
            case DUUIContext.DocumentComponentProcessContext dcp -> {
                addContextKeys(out, dcp.document());
                addContextKeys(out, dcp.component());
            }
            case DUUIContext.ReaderDocumentContext rd -> {
                addContextKeys(out, rd.reader());
                addContextKeys(out, rd.document());
            }
            default -> {
            }
        }
    }

    private static Document merge(Document... docs) {
        Document out = new Document();
        for (Document d : docs) out.putAll(d);
        return out;
    }
}
