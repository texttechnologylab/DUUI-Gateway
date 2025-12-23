package org.texttechnologylab.duui.api.routes.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.texttechnologylab.DockerUnifiedUIMAInterface.monitoring.DUUIStatus;
import org.texttechnologylab.duui.api.controllers.components.DUUIComponentController;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;
import org.texttechnologylab.duui.api.storage.DUUIMongoDBStorage;
import org.texttechnologylab.duui.api.storage.MongoDBFilters;

import com.mongodb.client.model.Filters;

import spark.Request;
import spark.Response;


/**
 * A class that is responsible for handling incoming requests to the /components path group.
 *
 * @author Cedric Borkowksi
 */
public class DUUIComponentRequestHandler {

    static Logger log = LoggerFactory.getLogger(DUUIComponentRequestHandler.class);

    /**
     * A set of all available drivers.
     */
    public static final Set<String> DRIVERS = Set.of(
        "DUUIDockerDriver",
        "DUUISwarmDriver",
        "DUUIRemoteDriver",
        "DUUIUIMADriver",
        "DUUIKubernetesDriver",
        "DUUIPodmanDriver"
    );


    /**
     * Inserts a new Component into the Database. If no user_id or no pipeline_id is specified, the component is
     * inserted as a template.
     * See {@link DUUIComponentController#insertOne(Document)}
     *
     * @param request  Request object
     * @param response Response object
     * @return Response message
     */
    public static String insertOne(Request request, Response response) {
        String userId = DUUIRequestHelper.getUserId(request);
        Document data = Document.parse(request.body());

        String name = data.getString("name");
        if (DUUIRequestHelper.isNullOrEmpty(name)) {
            log.warn("Missing field: name");
            return DUUIRequestHelper.missingField(response, "name");
        }

        if (DUUIRequestHelper.isNullOrEmpty(data.getString("target"))) {
            log.warn("Missing field: target");
            return DUUIRequestHelper.missingField(response, "target");
        }

        String driver = data.getString("driver");
        if (DUUIRequestHelper.isNullOrEmpty(driver)) {
            log.warn("Missing field: driver");
            return DUUIRequestHelper.missingField(response, "driver");
        }

        if (!DRIVERS.contains(driver)) {
            log.warn("Invalid driver: {}", driver);
            return DUUIRequestHelper.badRequest(response, "Driver must be one of " + String.join(", ", DRIVERS));
        }

        boolean isTemplate = request.queryParamOrDefault("template", "false").equals("true");

        if (DUUIRequestHelper.isNullOrEmpty(String.valueOf(data.getInteger("index"))) && !isTemplate) {
            log.warn("Missing field: index");
            return DUUIRequestHelper.missingField(response, "index");
        }

        Document options = data.get("options", Document.class);
        Document parameters = data.get("parameters", Document.class);

        if (DUUIRequestHelper.isNullOrEmpty(parameters)) {
            log.debug("Parameters are null or empty, initializing with an empty Document");
            parameters = new Document();
        }

        log.debug("Creating component Document");

        // Ensure annotation type lists are never null
        List<String> inputs = data.getList("inputs", String.class);
        if (inputs == null) {
            inputs = new ArrayList<>();
        }
        List<String> outputs = data.getList("outputs", String.class);
        if (outputs == null) {
            outputs = new ArrayList<>();
        }

        Document component = DUUIComponentController
            .insertOne(new Document()
                .append("name", name)
                .append("tags", data.getList("tags", String.class))
                .append("description", data.getString("description"))
                .append("status", DUUIRequestHelper.toTitleCase(DUUIStatus.INACTIVE.name()))
                .append("driver", data.getString("driver"))
                .append("target", data.getString("target"))
                .append("inputs", inputs)
                .append("outputs", outputs)
                .append("options", options)
                .append("parameters", parameters)
                .append("pipeline_id", isTemplate ? null : data.getString("pipeline_id"))
                .append("user_id", isTemplate ? null : userId)
                .append("index", data.getInteger("index")));

        log.info("Component successfully created: {}", component);
        response.status(200);
        return component.toJson();
    }

    /**
     * Inserts a spacy template into the database. The template is read from a file and inserted as a new component.
     * See {@link DUUIComponentController#insertOne(Document)}
     *
     */
    public static void insertSpacyTemplate() {
        System.out.println("Inserting spacy template...");
        try (InputStream is = DUUIComponentRequestHandler.class.getResourceAsStream("/spacy_template.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            if (is == null) {
                System.err.println("Spacy template file not found.");
                return;
            }
            
            String json = reader.lines().collect(Collectors.joining("\n"));
            Document spacyTemplate = Document.parse(json);
            
            // Directly insert the spacy template into MongoDB.
            
            String templateName = spacyTemplate.getString("name");
            Document existingTemplate = DUUIMongoDBStorage.Components()
                .find(new Document("name", templateName)
                    .append("pipeline_id", null)
                    .append("user_id", null) // Ensure it's a template
            ).first();
            
            if (existingTemplate != null) {
                System.out.println("Template with name '" + templateName + "' already exists. Skipping insertion.");
            } else {
                Document inserted = DUUIComponentController.insertOne(spacyTemplate);
                System.out.println("Spacy template inserted: " + templateName);
            } 

        } catch (IOException e) {
            System.err.println("Error reading spacy template file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inserting spacy template: " + e.getMessage());
        }
    }

    /**
     * Finds one component by its id extracted from the request url.
     * See {@link DUUIComponentController#findOneById(String)}
     *
     * @return A Json String containing the component or a not found message.
     */
    public static String findOne(Request request, Response response) {
        String id = request.params(":id");


        Document component = DUUIComponentController.findOneById(id);
        if (DUUIRequestHelper.isNullOrEmpty(component)) return DUUIRequestHelper.notFound(response);

        response.status(200);
        return DUUIMongoDBStorage.convertObjectIdToString(component).toJson();

    }

    /**
     * Finds many components by its userID extracted from the request and components that are marked as
     * templates by not having a user_id field set.
     * See {@link DUUIComponentController#findMany(MongoDBFilters)}
     *
     * @return A Json String containing the components or a not found message.
     */
    public static String findMany(Request request, Response response) {
        String userId = DUUIRequestHelper.getUserId(request);
        String userRole = DUUIRequestHelper.getUserProps(request, Set.of("role")).getString("role");
        if (userRole == null) return DUUIRequestHelper.unauthorized(response);

        int limit = DUUIRequestHelper.getLimit(request);
        int skip = DUUIRequestHelper.getSkip(request);
        String sort = DUUIRequestHelper.getSort(request, "name");
        int order = DUUIRequestHelper.getOrder(request, 1);

        String search = request.queryParamOrDefault("search", null);
        String pipelineId = request.queryParamOrDefault("pipeline_id", "");

        MongoDBFilters filters = new MongoDBFilters();
        filters
            .limit(limit)
            .skip(skip)
            .search(search)
            .sort(sort)
            .order(order);

        if (pipelineId.isEmpty()) {
            filters.addFilter(Filters.and(
                Filters.eq("pipeline_id", null),
                Filters.eq("user_id", null)
            ));
        } else {
            filters.addFilter(Filters.and(
                Filters.eq("pipeline_id", pipelineId),
                Filters.eq("user_id", userId)
            ));
        }

        List<Document> components = DUUIComponentController.findMany(filters);
        components.forEach(DUUIMongoDBStorage::convertObjectIdToString);

        return new Document("components", components).toJson();
    }

    /**
     * Updates one component by its id extracted from the url. The updates to be performed are provided
     * in the request body in the Json format.
     * See {@link DUUIComponentController#updateOne(String, Document)}
     *
     * @return A Json String containing the updated component or a not found message.
     */
    public static String updateOne(Request request, Response response) {
        String id = request.params(":id");
        Document component = DUUIComponentController
            .updateOne(id, Document.parse(request.body()));
        if (component == null) return DUUIRequestHelper.notFound(response);

        response.status(200);
        return component.toJson();
    }

    /**
     * Attemps to delete a component given its id.
     * See {@link DUUIComponentController#deleteOne(String)}
     *
     * @return Confirmation of deletion or a not found message.
     */
    public static String deleteOne(Request request, Response response) {
        String id = request.params(":id");

        if (DUUIComponentController.deleteOne(id)) {
            response.status(200);
            return "Deleted";
        } else {
            return DUUIRequestHelper.notFound(response);
        }
    }
}
