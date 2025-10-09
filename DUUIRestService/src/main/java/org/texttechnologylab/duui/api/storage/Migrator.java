package org.texttechnologylab.duui.api.storage;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.*;
import org.bson.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

public final class Migrator {
    /** All change sets live here */
    public interface ChangeSet {
        String id();
        String author();
        void run(MongoDatabase db) throws Exception;

        /** --- Example implementers --- */
        final class InitIndexes implements ChangeSet {
            public String id() { return "001-init-indexes"; }
            public String author() { return "you"; }
            public void run(MongoDatabase db) {
                var users = db.getCollection("users");
                users.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
                users.createIndex(Indexes.compoundIndex(Indexes.ascending("age"), Indexes.descending("name")));
            }
        }

        final class SeedAdmin implements ChangeSet {
            public String id() { return "002-seed-admin"; }
            public String author() { return "you"; }
            public void run(MongoDatabase db) {
                var users = db.getCollection("users");
                var exists = users.countDocuments(Filters.eq("name", "admin")) > 0;
                if (!exists) {
                    users.insertOne(new Document()
                            .append("name", "admin")
                            .append("roles", List.of("ADMIN"))
                            .append("active", true));
                }
            }
        }
    }

    private final MongoDatabase db;
    public Migrator(MongoDatabase db) { this.db = db; }

    public void run(List<ChangeSet> changes) {
        var coll = db.getCollection("migrations");
        coll.createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));

        var applied = coll.find()
                .projection(Projections.include("id"))
                .map(d -> d.getString("id"))
                .into(new HashSet<>());

        for (var cs : changes) {
            if (applied.contains(cs.id())) continue;
            try {
                cs.run(db);
                coll.insertOne(new Document()
                        .append("id", cs.id())
                        .append("author", cs.author())
                        .append("ts", Instant.now()));
            } catch (Exception e) {
                throw new RuntimeException("Migration failed: " + cs.id(), e);
            }
        }
    }

    // minimal boot example
    public static void main(String[] args) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            var db = client.getDatabase("app");
            new Migrator(db).run(List.of(
                    new ChangeSet.InitIndexes(),
                    new ChangeSet.SeedAdmin()
            ));
        }
    }
}

