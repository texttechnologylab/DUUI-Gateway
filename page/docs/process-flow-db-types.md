# Process Flow DB DTO Sketch
One file for snippet replacement targets + DTO shape sketch.

## DTO sketches (`org.texttechnologylab.duui.api.dto.db`, suffix `Db`)
```java
// NOTE: no org.bson.Document fields in DTOs.

record ProcessSettingsDb(
  // DUUIProcessController.getDefaultSettings() keys (+ profiler_path when enabled)
  Optional<Boolean> profile,
  Optional<String> profilerPath,        // "profiler_path"
  Optional<Boolean> notify,
  Optional<Boolean> checkTarget,        // "check_target"
  Optional<Boolean> recursive,
  Optional<Boolean> overwrite,
  Optional<Boolean> sortBySize,         // "sort_by_size"
  Optional<Integer> minimumSize,        // "minimum_size"
  Optional<Integer> workerCount,        // "worker_count"
  Optional<Boolean> ignoreErrors,       // "ignore_errors"
  Optional<String> language
) {}

record ProcessDb(
  String oid,
  Optional<String> pipelineId,
  Optional<String> userId,
  Optional<String> status,
  Optional<Double> progress,
  Optional<Integer> documentsTotal,
  Optional<Integer> size,                               // pipeline component count stored on process
  Optional<Long> startedAt,
  Optional<Long> finishedAt,
  Optional<Boolean> isFinished,
  Optional<String> profilerPath,                        // some code also stores top-level "profiler_path"
  Optional<Map<String, String>> pipelineStatus,         // stored as object today
  Optional<Set<String>> documentNames,                  // stored as set today
  Optional<DocumentProviderDb> input,
  Optional<DocumentProviderDb> output,
  Optional<ProcessSettingsDb> settings,
  Optional<String> error,
) {}

record DocumentProviderDb(
  Optional<String> provider,
  Optional<String> providerId,          // "provider_id"
  Optional<String> path,
  Optional<String> content,
  Optional<String> fileExtension        // "file_extension"
) {}

record DocumentDb(
  String oid,
  Optional<String> processId,
  Optional<String> path,
  Optional<String> name,
  Optional<Long> size,
  Optional<Double> progress,
  Optional<String> status,
  Optional<String> error,
  Optional<Boolean> isFinished,
  Optional<Long> durationDecode,
  Optional<Long> durationDeserialize,
  Optional<Long> durationWait,
  Optional<Long> durationProcess,
  Optional<Double> progressUpload,
  Optional<Double> progressDownload,
  Optional<Long> startedAt,
  Optional<Long> finishedAt
) {}

sealed interface ContextDb permits
  ComposerContextDb, DriverContextDb, WorkerContextDb,
  DocumentContextDb, DocumentProcessContextDb,
  ComponentContextDb, InstantiatedComponentContextDb, DocumentComponentProcessContextDb,
  ReaderContextDb {}

record ComposerContextDb(
  String runKey, String status, String thread, double progress, long total
) implements ContextDb {}

record DriverContextDb(String driver) implements ContextDb {}

record WorkerContextDb(
  ComposerContextDb composer, String name, int activeWorkers
) implements ContextDb {}

record DocumentSnapshotDb(
  String path, String name, long size, double progress, String error, boolean isFinished,
  long durationDecode, long durationDeserialize, long durationWait,
  long startedAt, long finishedAt
) {}

record DocumentContextDb(DocumentSnapshotDb document) implements ContextDb {}

record DocumentProcessContextDb(
  DocumentContextDb document, ComposerContextDb composer
) implements ContextDb {}

record ComponentContextDb(
  String component, String name, String driver, List<String> instanceIds
) implements ContextDb {}

record InstantiatedComponentContextDb(
  ComponentContextDb component, String instanceId, String endpoint
) implements ContextDb {}

record DocumentComponentProcessContextDb(
  DocumentContextDb document, ComponentContextDb component
) implements ContextDb {}

record ReaderSnapshotDb(
  long total, long skipped, long read, long remaining, long usedBytes, long totalBytes
) {}

record ReaderContextDb(ReaderSnapshotDb reader) implements ContextDb {}

// PayloadDb matches the nested "payload" object only: { content, type }
record PayloadDb(String content, String type) {}

record EventDb(String id, Long timestamp, EventBodyDb event) {
  // current insertMany: eventDoc.append("context", serialize(context)); then eventDoc.putAll(serialize(payloadRecord));
  // => payload fields live on event level (same object as process_id/sender/message/level/context)
  record EventBodyDb(
    String oid,
    String processId,
    String sender,
    String message,
    String level,
    ContextDb context,
    String status,
    String thread,
    PayloadDb payload
  ) {}
  final class Fields {
    static final String TIMESTAMP = "timestamp";
    static final String EVENT_PROCESS_ID = "event.process_id";
    static final String EVENT_CONTEXT_DOCUMENT = "event.context.document";
    static final String EVENT_MESSAGE = "event.message";
  }
}

record PipelineDb(String id, String userId, String name) {}
record PipelineDb(
  String oid,
  Optional<String> userId,
  Optional<String> name,
  Optional<String> description,
  Optional<String> status,
  Optional<List<String>> tags,
  Optional<Long> createdAt,
  Optional<Long> modifiedAt,
  Optional<Integer> timesUsed,
  Optional<Long> lastUsed,
  Optional<Map<String, Object>> settings              // stored as object today
) {}

record ComponentDb(
  String oid,
  Optional<String> pipelineId,
  Optional<String> userId,
  Optional<Integer> index,
  Optional<String> name,
  Optional<List<String>> tags,
  Optional<String> description,
  Optional<String> status,
  Optional<String> driver,
  Optional<String> target,
  Optional<List<String>> inputs,
  Optional<List<String>> outputs,
  Optional<ComponentOptionsDb> options,
  Optional<Map<String, Object>> parameters,
  Optional<Long> createdAt,
  Optional<Long> modifiedAt
) {}

record ComponentOptionsDb(
  Optional<Integer> scale,
  Optional<Boolean> useGPU,               // "use_GPU"
  Optional<Boolean> dockerImageFetching,  // "docker_image_fetching"
  Optional<Boolean> keepAlive,
  Optional<List<String>> constraints,
  Optional<List<String>> labels,
  Optional<String> host,
  Optional<Boolean> ignore200Error,       // "ignore_200_error"
  Optional<RegistryAuthDb> registryAuth
) {}

record RegistryAuthDb(Optional<String> username, Optional<String> password) {}

record UserDb(
  String oid,
  Optional<String> email,
  Optional<String> password,
  Optional<Long> createdAt,
  Optional<String> role,
  Optional<Integer> workerCount,
  Optional<PreferencesDb> preferences,
  Optional<String> session,
  Optional<String> passwordResetToken,
  Optional<Long> resetTokenExpiration,
  Optional<UserConnectionsDb> connections,
  Optional<Boolean> activated
) {}

record PreferencesDb(
  Optional<Boolean> tutorial,
  Optional<String> language,
  Optional<Boolean> notifications
) {}

record UserConnectionsDb(
  Optional<String> key,
  Optional<Integer> workerCount,
  Optional<Map<String, DropboxConnectionDb>> dropbox,
  Optional<Map<String, MinioConnectionDb>> minio,
  Optional<Map<String, NextcloudConnectionDb>> nextcloud,
  Optional<Map<String, GoogleConnectionDb>> google,
  Optional<MongoDbConnectionDb> mongoDb        // "connections.mongoDB"
) {}

record DropboxConnectionDb(
  Optional<String> accessToken,     // "access_token"
  Optional<String> refreshToken,    // "refresh_token"
  Optional<String> alias,
  Optional<FolderNodeDb> folderStructure
) implements ServiceConnection {}

record GoogleConnectionDb(
  Optional<String> accessToken,
  Optional<String> refreshToken,
  Optional<String> alias,
  Optional<FolderNodeDb> folderStructure
) implements ServiceConnection {}

record MinioConnectionDb(
  Optional<String> endpoint,
  Optional<String> accessKey,
  Optional<String> secretKey,
  Optional<String> alias,
  Optional<FolderNodeDb> folderStructure
) implements ServiceConnection {}

record NextcloudConnectionDb(
  Optional<String> uri,
  Optional<String> username,
  Optional<String> password,
  Optional<String> alias,
  Optional<FolderNodeDb> folderStructure
) implements ServiceConnection {}

record MongoDbConnectionDb(Optional<String> uri) {}

interface ServiceConnection {
  Optional<String> alias();
}

// Globals refactor targets (each becomes its own collection; old globals doc remains readable for migration)
record GroupDb(
  String oid,
  Optional<String> name,
  Optional<List<String>> members,
  Optional<List<String>> labels,
  Optional<List<String>> whitelist
) {}

record RegistryEndpointDb(
  String oid,
  Optional<String> name,
  Optional<RegistryScope> scope,
  Optional<List<String>> groups,   // used when scope == "GROUP"
  Optional<String> endpoint        // observed in frontend as "endpoint"
) {}

enum RegistryScope { USER, GROUP }

record LabelDb(
  String oid,
  Optional<String> label,
  Optional<String> driver,         // driver name or "Any" (observed)
  Optional<List<String>> groups    // empty => public label (observed)
) {}

record GlobalSettingsDb(
  Optional<List<String>> allowedOrigins,          // stored as ";" string today
  Optional<OAuthAppDb> dropbox,                   // present in Main.config today
  Optional<OAuthAppDb> google,                    // present in Main.config today
  Optional<String> fileUploadDirectory,           // Main.config.getFileUploadPath()
  Optional<String> localDriveRoot,                // Main.config.getLocalDriveRoot()
  Optional<FolderNodeDb> localFolderStructure     // cached folder structure doc today (record, not raw)
) {}

record OAuthAppDb(String key, String secret, String url) {}

// Folder structure (cached under settings.local_folder_structure)
record FolderNodeDb(
  String id,
  String name,
  String path,
  boolean folder,
  long size,
  List<FolderNodeDb> children
) {}
```

## DTO optionality (partial body requests)
Rule: DTOs used as partial-update bodies use `Optional<T>` for patchable fields; `Optional.empty()` means "leave unchanged".

## DTO hierarchy + BSON serialization pattern
Standard pattern: request -> `ReqDto` -> validate -> controller uses only DTOs -> persist -> response DTO -> JSON.

```java
package org.texttechnologylab.duui.api.dto;

import org.bson.Document;

public interface BsonDto<T> {
  Document toBson();
}

public interface BsonReadable<T> {
  // tolerant read: supports old DB documents
  T fromBson(Document d);
}
```

```java
// Standard helpers: ObjectId <-> oid and failure responses
final class DtoIds {
  static String requireOid(String oid) { /* null/empty check + throw */ }
  static ObjectId toObjectId(String oid) { /* new ObjectId(oid) with error */ }
  static String toOid(ObjectId id) { return id.toHexString(); }
}

final class Req {
  static Document body(Request req) { return Document.parse(req.body()); }
}

final class Resp {
  static String badRequest(Response res, String msg) { res.status(400); return new Document("error", msg).toJson(); }
  static String notFound(Response res, String msg) { res.status(404); return new Document("error", msg).toJson(); }
  static String ok(Response res, Object payload) { res.status(200); return payload instanceof Document d ? d.toJson() : new Document("data", payload).toJson(); }
}
```

## Globals controllers (start simple; no new collections yet)
We create dedicated controllers but still persist inside the existing `globals` document for now.

```java
// package org.texttechnologylab.duui.api.controllers.globals;
public final class GroupController {
  // read/write under globals.groups.{oid}
  static String upsert(String oid, GroupCreateReq req) { /* validate -> write -> return */ }
  static String patch(String oid, GroupPatchReq req) { /* validate -> partial update */ }
  static boolean delete(String oid) { /* delete under globals */ }
  static List<GroupDb> list() { /* read globals.groups map */ }
}

public final class RegistryController { /* globals.registries.{oid} */ }
public final class LabelController { /* globals.labels.{oid} */ }
public final class GlobalSettingsController { /* globals.settings */ }
```

```java
// Validation sketch (common)
final class V {
  static void requireNonEmpty(String v, String field) { /*...*/ }
  static void requireList(List<?> v, String field) { /*...*/ }
  static void requireEnum(String v, Class<? extends Enum<?>> e, String field) { /*...*/ }
}
```

## OID naming standard (DTO + frontend)
```java
// Rule: DTO field name is always "oid" for ids, never "_id" or "id".
// Storage: convert oid <-> ObjectId at the DB boundary only.
// Frontend: always sends/receives "oid".
```

## User DTOs still missing (sketch)
```java
// UserDb is incomplete today; needs at least:
record UserDb(
  String oid,
  String email,
  String role,
  Integer workerCount,
  String session,
  PreferencesDb preferences,
  Map<String, ConnectionDb> connections
) {}
```

```java
// Sketch mapping targets (BSON keys) for ContextDb -> Mongo event.context
// - ComposerContextDb: runKey, status, thread, progress, total
// - DriverContextDb: driver
// - WorkerContextDb: (merge composer) + name + activeWorkers
// - DocumentContextDb: path, name, size, progress, error, is_finished, duration_decode, duration_deserialize, duration_wait, started_at, finished_at
// - DocumentProcessContextDb: merge(document, composer)
// - ComponentContextDb: component, name, driver, instance_ids
// - InstantiatedComponentContextDb: merge(component) + instance_id + endpoint
// - DocumentComponentProcessContextDb: merge(document, component)
// - ReaderContextDb: total, skipped, read, remaining, used_bytes, total_bytes

// Sketch mapping targets (BSON keys) for payload fields -> Mongo event.{status,thread,payload}
// - EventBodyDb.status -> "status"
// - EventBodyDb.thread -> "thread"
// - EventBodyDb.payload -> "payload" { content, type }
```

```java
final class MongoCoerce {
  static String id(Document d) { /* _id/oid/string */ }
  static String stringOrNull(Document d, String k) { }
  static Long longOrNull(Document d, String k) { }
  static Double doubleOrNull(Document d, String k) { }
  static Boolean boolOrNull(Document d, String k) { }
}
```

## Replace: `DUUIEventController.insertMany` event document build
```java
Function<DUUIEvent, Document> serializeEvent = (DUUIEvent event) -> {
    Document eventDoc = new Document("_id", processId + "_" + getEventId(event))
        .append("process_id", processId)
        .append("sender", event.getSender() != null ? event.getSender().name() : null)
        .append("message", event.getMessage())
        .append("level", event.getDebugLevel().name())
        .append("context", serialize(event.getContext()));

    eventDoc.putAll(serialize(event.getContext().payloadRecord()));

    return new Document()
        .append("timestamp", event.getTimestamp())
        .append("event", eventDoc);
};
```
```java
EventDb dbEvent = EventDb.from(processId, event);
Document mongoDoc = dbEvent.toDocument();
```

## Replace: `DUUIEventController.serialize(DUUIContext)`
```java
private static Document serialize(DUUIContext context) {
    return switch (context) {
        case ComposerContext(...) -> new Document() /* ... */;
        case WorkerContext(...) -> merge(serialize(composer)) /* ... */;
        default -> new Document();
    };
}
```
```java
Document contextDoc = ContextDb.from(context).toDocument();
```

## Replace: hardcoded `events` dot-paths
```java
Filters.eq("event.process_id", processId);
Filters.eq("event.context.document", path);
new Document("event.message", Pattern.compile(...));
```
```java
Filters.eq(EventDb.Fields.EVENT_PROCESS_ID, processId);
Filters.eq(EventDb.Fields.EVENT_CONTEXT_DOCUMENT, path);
new Document(EventDb.Fields.EVENT_MESSAGE, Pattern.compile(...));
```

## Replace: `DUUIDocumentController` embedding events
```java
List<Document> events = DUUIEventController.findManyByDocument(document.getString("oid"));
events.forEach(DUUIMongoDBStorage::convertObjectIdToString);
events.forEach(event -> DUUIMongoDBStorage.convertDateToTimestamp(event, "timestamp"));
document.append("events", events).toJson();
```
```java
List<EventDb> events = DUUIEventController.findManyByDocumentDb(documentOid);
document.append("events", events.stream().map(EventDb::toApiDocument).toList());
```

## Replace: `ProcessEventWebSocketHandler` backlog send
```java
List<Document> events = DUUIEventController.findManyByProcess(processId);
for (Document event : events) session.getRemote().sendStringByFuture(event.toJson());
```
```java
List<EventDb> events = DUUIEventController.findManyByProcessDb(processId);
for (EventDb event : events) session.getRemote().sendStringByFuture(event.toApiDocument().toJson());
```

## Questions
- Keep `EventDb.event.context` as raw `Document` until context/payload refactor, or define temporary `ContextDb` now?
- For legacy IDs in API responses: standardize on `oid`, `_id`, or both?
- Globals migration: during transition, should reads check new collections first and fall back to `globals.{groups|registries|labels|settings}`?

## Wrapper sketch (typed field + builder-ish ops)
One-file sketch of wrappers around Mongo driver operations using DTO `Fields` (no raw strings at call sites).

```java
// Typed field path
final class F<T> {
  final String path;
  private F(String path) { this.path = path; }
  static <T> F<T> of(String path) { return new F<>(path); }
}

// Collection registry derived from DTO class (single switch).
final class DtoRegistry {
  static MongoCollection<Document> collection(Class<?> dtoClass) {
    return switch (dtoClass.getSimpleName()) {
      case "EventDb" -> DUUIMongoDBStorage.Events();
      case "ProcessDb" -> DUUIMongoDBStorage.Processes();
      case "DocumentDb" -> DUUIMongoDBStorage.Documents();
      case "PipelineDb" -> DUUIMongoDBStorage.Pipelines();
      case "ComponentDb" -> DUUIMongoDBStorage.Components();
      case "UserDb" -> DUUIMongoDBStorage.Users();
      // globals refactor targets (new collections)
      case "GroupDb" -> DUUIMongoDBStorage.Groups();
      case "RegistryEndpointDb" -> DUUIMongoDBStorage.Registries();
      case "LabelDb" -> DUUIMongoDBStorage.Labels();
      case "GlobalSettingsDb" -> DUUIMongoDBStorage.SettingsCollection();
      default -> throw new IllegalArgumentException("No collection for " + dtoClass);
    };
  }
}

// DTO interface to expose its Fields (fields are CONSTANTS, not record accessors).
// Fields are METHODS so call sites can use method refs (Fields::EVENT_PROCESS_ID).
interface HasFields<FIELDS> {
  FIELDS fields();
}

// Filter builder (wraps com.mongodb.client.model.Filters)
final class Q {
  static <T> Bson eq(F<T> f, T v) { return Filters.eq(f.path, v); }
  static <T> Bson ne(F<T> f, T v) { return Filters.ne(f.path, v); }
  static Bson exists(F<?> f) { return Filters.exists(f.path, true); }
  static Bson exists(F<?> f, boolean on) { return Filters.exists(f.path, on); }
  static Bson regex(F<String> f, Pattern p) { return Filters.regex(f.path, p); }
  static Bson and(Bson... xs) { return Filters.and(xs); }
  static Bson or(Bson... xs) { return Filters.or(xs); }
}

// Update builder (wraps com.mongodb.client.model.Updates)
final class U {
  static <T> Bson set(F<T> f, T v) { return Updates.set(f.path, v); }
  static Bson unset(F<?> f) { return Updates.unset(f.path); }
  static Bson inc(F<? extends Number> f, Number n) { return Updates.inc(f.path, n); }
  static <T> Bson push(F<List<T>> f, T v) { return Updates.push(f.path, v); }
  static <T> Bson pull(F<List<T>> f, T v) { return Updates.pull(f.path, v); }
  static <T> Bson addToSet(F<List<T>> f, T v) { return Updates.addToSet(f.path, v); }
  static Bson combine(Bson... xs) { return Updates.combine(xs); }
}

// Sort builder (wraps com.mongodb.client.model.Sorts)
enum Dir { ASC, DESC }
final class S {
  static Bson by(F<?> f, Dir d) { return d == Dir.ASC ? Sorts.ascending(f.path) : Sorts.descending(f.path); }
}

// Projection builder (wraps com.mongodb.client.model.Projections)
final class P {
  static Bson include(F<?>... fs) { return Projections.include(Arrays.stream(fs).map(x -> x.path).toList()); }
  static Bson exclude(F<?>... fs) { return Projections.exclude(Arrays.stream(fs).map(x -> x.path).toList()); }
}
```

```java
// Aggregate stage wrappers (wrap com.mongodb.client.model.Aggregates)
final class A {
  static Bson match(Bson f) { return Aggregates.match(f); }
  static Bson sort(Bson s) { return Aggregates.sort(s); }
  static Bson skip(int n) { return Aggregates.skip(n); }
  static Bson limit(int n) { return Aggregates.limit(n); }
  static Bson project(Bson p) { return Aggregates.project(p); }
  static Bson addFields(Field<?>... fs) { return Aggregates.addFields(fs); }
  static Bson group(String idExpr, Bson... acc) { return Aggregates.group(idExpr, acc); }
  static Bson facet(Facet... facets) { return Aggregates.facet(facets); }
  static Bson count(String outField) { return Aggregates.count(outField); }
}
```

```java
// Repository-style operation builders (thin “builder pattern” around MongoCollection<Document>)
final class Repo<DTO extends HasFields<FIELDS>, FIELDS> {
  private final MongoCollection<Document> col;
  private final FIELDS f;

  private Repo(MongoCollection<Document> col, FIELDS f) {
    this.col = col;
    this.f = f;
  }

  // Collection derived from DTO class; Fields provided via DTO interface (no extra registry).
  static <DTO extends HasFields<FIELDS>, FIELDS> Repo<DTO, FIELDS> of(Class<DTO> dtoClass, DTO dto) {
    return new Repo<>(DtoRegistry.collection(dtoClass), dto.fields());
  }

  FindOp<DTO, FIELDS> find() { return new FindOp<>(col, f); }
  AggOp<DTO, FIELDS> aggregate() { return new AggOp<>(col, f); }
  InsertOp<DTO, FIELDS> insert() { return new InsertOp<>(col); }
  UpdateOp<DTO, FIELDS> update() { return new UpdateOp<>(col, f); }
  DeleteOp<DTO, FIELDS> delete() { return new DeleteOp<>(col); }
  CountOp<DTO, FIELDS> count() { return new CountOp<>(col); }
  FindOneAndOp<DTO, FIELDS> findOneAnd() { return new FindOneAndOp<>(col); }

  static final class FindOp<DTO, FIELDS> {
    private final MongoCollection<Document> col;
    private final FIELDS f;
    private Bson filter = new Document();
    private Bson projection;
    private Bson sort;
    private int skip = 0;
    private int limit = 0;
    FindOp(MongoCollection<Document> col, FIELDS f) { this.col = col; this.f = f; }
    FindOp where(Bson f) { this.filter = f; return this; }
    <T> FindOp where(java.util.function.Function<FIELDS, F<T>> pick, T value) {
      this.filter = Q.eq(pick.apply(f), value);
      return this;
    }
    FindOp project(Bson p) { this.projection = p; return this; }
    FindOp sort(Bson s) { this.sort = s; return this; }
    FindOp sort(java.util.function.Function<FIELDS, F<?>> pick, Dir d) {
      this.sort = S.by(pick.apply(f), d);
      return this;
    }
    FindOp skip(int n) { this.skip = n; return this; }
    FindOp limit(int n) { this.limit = n; return this; }
    FindIterable<Document> exec() {
      FindIterable<Document> it = col.find(filter);
      if (projection != null) it = it.projection(projection);
      if (sort != null) it = it.sort(sort);
      if (skip > 0) it = it.skip(skip);
      if (limit > 0) it = it.limit(limit);
      return it;
    }
  }

  static final class AggOp<DTO, FIELDS> {
    private final MongoCollection<Document> col;
    private final FIELDS f;
    private final List<Bson> pipeline = new ArrayList<>();
    AggOp(MongoCollection<Document> col, FIELDS f) { this.col = col; this.f = f; }
    AggOp stage(Bson s) { pipeline.add(s); return this; }
    AggregateIterable<Document> exec() { return col.aggregate(pipeline); }
  }

  static final class InsertOp<DTO, FIELDS> {
    private final MongoCollection<Document> col;
    InsertOp(MongoCollection<Document> col) { this.col = col; }
    void one(Document d) { col.insertOne(d); }
    void many(List<Document> ds) { col.insertMany(ds); }
  }

  static final class UpdateOp<DTO, FIELDS> {
    private final MongoCollection<Document> col;
    private final FIELDS f;
    private Bson filter = new Document();
    private Bson update;
    private UpdateOptions opts;
    UpdateOp(MongoCollection<Document> col, FIELDS f) { this.col = col; this.f = f; }
    UpdateOp where(Bson f) { this.filter = f; return this; }
    <T> UpdateOp where(java.util.function.Function<FIELDS, F<T>> pick, T value) {
      this.filter = Q.eq(pick.apply(f), value);
      return this;
    }
    UpdateOp set(Bson u) { this.update = u; return this; }
    <T> UpdateOp set(java.util.function.Function<FIELDS, F<T>> pick, T value) {
      this.update = U.set(pick.apply(f), value);
      return this;
    }
    UpdateOp options(UpdateOptions o) { this.opts = o; return this; }
    void one() { if (opts != null) col.updateOne(filter, update, opts); else col.updateOne(filter, update); }
  }

  static final class DeleteOp<DTO, FIELDS> {
    private final MongoCollection<Document> col;
    DeleteOp(MongoCollection<Document> col) { this.col = col; }
    void one(Bson f) { col.deleteOne(f); }
    void many(Bson f) { col.deleteMany(f); }
  }

  static final class CountOp<DTO, FIELDS> {
    private final MongoCollection<Document> col;
    CountOp(MongoCollection<Document> col) { this.col = col; }
    long all() { return col.countDocuments(); }
    long where(Bson f) { return col.countDocuments(f); }
  }

  static final class FindOneAndOp<DTO, FIELDS> {
    private final MongoCollection<Document> col;
    FindOneAndOp(MongoCollection<Document> col) { this.col = col; }
    Document update(Bson filter, Bson update, FindOneAndUpdateOptions opts) {
      return col.findOneAndUpdate(filter, update, opts);
    }
    Document replace(Bson filter, Document replacement, FindOneAndReplaceOptions opts) {
      return col.findOneAndReplace(filter, replacement, opts);
    }
  }
}
```

```java
// Example usage (no raw strings)
// EventDb implements HasFields<EventDb.Fields> returning Fields.INSTANCE.
var repo = Repo.of(EventDb.class, EventDb.INSTANCE);
var it = repo.find()
  .where(EventDb.Fields::EVENT_PROCESS_ID, processId)
  .sort(EventDb.Fields::TIMESTAMP, Dir.DESC)
  .limit(100)
  .exec();
```

## Requests → Spark → Mongo DTOs (partial-field map)
Frontend requests (SvelteKit `DUUIWeb/src/routes/api/**`) mapped to Spark handlers and the Mongo collection DTO impacted; request fields listed are the actual JSON/query/header fields used.

### `processes` (`ProcessDb`)
- `DUUIWeb/src/routes/api/processes/+server.ts` `POST` → `POST ${API_URL}/processes` → Spark `DUUIProcessRequestHandler.start`
  - Body: `pipeline_id`, `input` (object), `output` (object), `settings` (object, partial OK)
- `DUUIWeb/src/routes/api/processes/+server.ts` `PUT` → `PUT ${API_URL}/processes/${oid}` → Spark `DUUIProcessRequestHandler.updateOne`
  - Body: includes `oid` (used for URL), plus arbitrary update fields (partial patch)
- `DUUIWeb/src/routes/api/processes/+server.ts` `DELETE` → `DELETE ${API_URL}/processes/${oid}` → Spark `DUUIProcessRequestHandler.deleteOne`
  - Body: includes `oid` (used for URL), body not otherwise used
- `DUUIWeb/src/routes/api/processes/batch/+server.ts` `GET` → `GET ${API_URL}/processes?...` → Spark `DUUIProcessRequestHandler.findMany`
  - Query: `pipeline_id`, `limit`, `skip`, `sort`, `order`, `status`, `input`, `output`

### `documents` (`DocumentDb`)
- `DUUIWeb/src/routes/api/processes/documents/+server.ts` `GET` → `GET ${API_URL}/processes/${processId}/documents?...` → Spark `DUUIProcessRequestHandler.findDocuments`
  - Query: `process_id`, `limit`, `skip`, `sort`, `order`, `search`, `status`

### `events` (`EventDb`)
- `DUUIWeb/src/lib/ws/processEvents.ts` `GET /api/processes/events?process_id=...` → `DUUIWeb/src/routes/api/processes/events/+server.ts` (build WS URL only)
  - Query: `process_id`
- Timeline: `GET ${API_URL}/processes/:id/events` → Spark `DUUIProcessRequestHandler.findEvents`
  - Path: `:id`

### `pipelines` (`PipelineDb`)
- `DUUIWeb/src/routes/api/pipelines/+server.ts` `POST` → `POST ${API_URL}/pipelines?template=${bool}` → Spark `DUUIPipelineRequestHandler.insertOne`
  - Body: `name`, `components` (list; full objects)
  - Query: `template`
- `DUUIWeb/src/routes/api/pipelines/+server.ts` `PUT` → `PUT ${API_URL}/pipelines/${oid}` → Spark `DUUIPipelineRequestHandler.updateOne`
  - Body: includes `oid` (used for URL), plus arbitrary update fields (partial patch)
- `DUUIWeb/src/routes/api/pipelines/+server.ts` `DELETE` → `DELETE ${API_URL}/{pipelines|components}/${oid}` → Spark `DUUIPipelineRequestHandler.deleteOne` (or component delete)
  - Body: `oid`, `component` (boolean switches endpoint)
- `DUUIWeb/src/routes/api/pipelines/batch/+server.ts` `GET` → `GET ${API_URL}/pipelines?...` → Spark `DUUIPipelineRequestHandler.findMany`
  - Query: `limit`, `skip`, `sort`, `order`
- `DUUIWeb/src/routes/api/pipelines/services/+server.ts` `POST` → `POST ${API_URL}/pipelines/${oid}/start` → Spark `DUUIPipelineRequestHandler.start`
  - Body: `oid`
- `DUUIWeb/src/routes/api/pipelines/services/+server.ts` `PUT` → `PUT ${API_URL}/pipelines/${oid}/stop` → Spark `DUUIPipelineRequestHandler.stop`
  - Body: `oid`

### `components` (`ComponentDb`)
- `DUUIWeb/src/routes/api/components/+server.ts` `POST` → `POST ${API_URL}/components?template=${bool}` → Spark `DUUIComponentRequestHandler.insertOne`
  - Body: `name`, `target`, `driver`, `index` (required unless template), optional: `tags`, `description`, `inputs`, `outputs`, `options`, `parameters`, `pipeline_id`
  - Query: `template`
- `DUUIWeb/src/routes/api/components/+server.ts` `PUT` → `PUT ${API_URL}/components/${oid}` → Spark `DUUIComponentRequestHandler.updateOne`
  - Body: includes `oid` (used for URL), plus arbitrary update fields (partial patch)
- `DUUIWeb/src/routes/api/components/+server.ts` `DELETE` → `DELETE ${API_URL}/components/${oid}` → Spark `DUUIComponentRequestHandler.deleteOne`
  - Body: `oid`

### `users` (`UserDb`)
- `DUUIWeb/src/routes/api/users/+server.ts` `PUT` → `PUT ${API_URL}/users/${user.oid}` → controller `DUUIUserController.updateOne`
  - Body: arbitrary patch fields (partial), `connections.*` updates allowed
- `DUUIWeb/src/routes/api/users/connections/+server.ts` `PUT` → `PUT ${API_URL}/users/connections/${user.oid}/${provider}` → controller `DUUIUserController.insertNewConnection`
  - Query: `provider`
  - Body: expects key like `connections.${provider}.${providerId}` → connection object

### `globals` (current) → refactor targets (`GroupDb`, `RegistryEndpointDb`, `LabelDb`, `GlobalSettingsDb`)
- `DUUIWeb/src/routes/api/users/groups/+server.ts` `PUT` → `PUT ${API_URL}/users/groups/${groupId}` → controller `DUUIUserController.upsertGroup`
  - Body: group object (validated for `name`, `members`, `labels`)
- `DUUIWeb/src/routes/api/users/groups/+server.ts` `DELETE` → `DELETE ${API_URL}/users/groups/${groupId}` → controller `DUUIUserController.deleteGroup`
  - Body: `{}` (unused)
- `DUUIWeb/src/routes/api/users/registry/+server.ts` `PUT` → `PUT ${API_URL}/users/registries/${registryId}` → controller `DUUIUserController.upsertRegistryEndpoint`
  - Body: registry object (validated for `scope`, `name`, `endpoint`; `groups` used for GROUP scope)
- `DUUIWeb/src/routes/api/users/registry/+server.ts` `DELETE` → `DELETE ${API_URL}/users/registries/${registryId}` → controller `DUUIUserController.deleteRegistryEndpoint`
  - Body: `{}` (unused)
- `DUUIWeb/src/routes/api/users/labels/+server.ts` `PUT` → `PUT ${API_URL}/users/labels/${labelId}` → controller `DUUIUserController.upsertLabel`
  - Body: label object (validated for `driver`, `label`, `groups`)
- `DUUIWeb/src/routes/api/users/labels/+server.ts` `DELETE` → `DELETE ${API_URL}/users/labels/${labelId}` → controller `DUUIUserController.deleteLabel`
  - Body: `{}` (unused)
- `DUUIWeb/src/routes/api/settings/+server.ts` `PUT` → `PUT ${API_URL}/settings/` → Spark `Main.updateSettings`
  - Body: settings object (partial); `allowed_origins` array → stored as `;` string
- `DUUIWeb/src/routes/api/settings/local-folder-structure/+server.ts` `GET` → `GET ${API_URL}/files/local-folder-structure/{reset|cached}` → Spark `Main.getLocalFolderStructure`
  - Query: `reset` (frontend), server uses `{reset|cached}` path segment
- `DUUIWeb/src/routes/api/settings/filtered-folder-structure/+server.ts` `GET` → `GET ${API_URL}/files/filtered-folder-structure` → Spark `Main.getFilteredFolderStructure`

## Questions
- Keep `EventDb.event.context` as raw `Document` until context/payload refactor, or define temporary `ContextDb` now?
- For legacy IDs in API responses: standardize on `oid`, `_id`, or both?
- Globals migration: during transition, should reads check new collections first and fall back to `globals.{groups|registries|labels|settings}`?

## Spark request pattern (auth → parse → DTO → DB)
Controller-local helpers: `parseParams` + `parseBody`, then merge, then DTO.

```java
// Controller pattern (per handler method)
authenticate(request);               // existing DUUIRequestHelper.authenticate / getUserId, etc.
Document params = parseParams(request);
Document body = parseBody(request);
Document merged = Merge.noDup(params, body);   // throw if duplicate keys
MyDto dto = MyDto.fromBson(merged);
DtoValidator.validate(dto);                    // standard validator step (fail fast)
// DB ops using dto only (no raw Document after this point)
```

```java
// Per-controller: only whitelist keys that controller cares about
static Document parseParams(Request req, Set<String> keys) {
  Document out = new Document();
  for (String k : keys) {
    String v = req.queryParams(k);
    if (v != null) out.put(k, v);
  }
  return out;
}

static Document parseBody(Request req) {
  String raw = req.body();
  if (raw == null || raw.isBlank()) return new Document();
  return Document.parse(raw);
}
```

```java
final class Merge {
  static Document noDup(Document a, Document b) {
    Document out = new Document(a);
    for (Map.Entry<String, Object> e : b.entrySet()) {
      if (out.containsKey(e.getKey())) {
        throw new IllegalArgumentException("Duplicate key: " + e.getKey());
      }
      out.put(e.getKey(), e.getValue());
    }
    return out;
  }
}
```

```java
// Standard DTO validator hook
interface DtoValidator<T> {
  void validate(T dto) throws IllegalArgumentException;
}

final class Validators {
  static final DtoValidator<GroupDb> GROUP = dto -> {
    if (dto.oid() == null || dto.oid().isBlank()) throw new IllegalArgumentException("Missing oid");
    dto.name().ifPresent(v -> { if (v.isBlank()) throw new IllegalArgumentException("name empty"); });
    dto.members().ifPresent(v -> { if (v.isEmpty()) throw new IllegalArgumentException("members empty"); });
  };
}
```
