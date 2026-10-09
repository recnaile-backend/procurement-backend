package com.recnaile.notification.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Migrates legacy recipientId records/indexes to the canonical empId field, then creates indexes. */
@Configuration
public class MongoIndexConfiguration {
    private static final String NOTIFICATIONS = "notifications";
    private static final String LEGACY_RECIPIENT_INDEX = "recipient_created_at_idx";
    private static final String LEGACY_UNIQUE_INDEX = "event_recipient_channel_unique";

    @Bean
    ApplicationRunner notificationIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            MongoCollection<Document> notifications = mongoTemplate.getCollection(NOTIFICATIONS);

            // Do not silently overwrite conflicting identities in old documents.
            long conflicting = notifications.countDocuments(Filters.and(
                    Filters.exists("recipientId", true), Filters.exists("empId", true),
                    Filters.expr(new Document("$ne", java.util.List.of("$recipientId", "$empId")))));
            if (conflicting > 0) {
                throw new IllegalStateException("Cannot migrate notifications: " + conflicting
                        + " documents contain different recipientId and empId values. Resolve these records before startup.");
            }

            // Preserve old recipient values before removing the legacy field.
            // Use an aggregation update pipeline so the old field's value is copied, not stored literally.
            notifications.updateMany(
                    Filters.and(Filters.exists("empId", false), Filters.exists("recipientId", true)),
                    java.util.List.of(new Document("$set", new Document("empId", "$recipientId"))));
            notifications.updateMany(Filters.exists("recipientId", true), Updates.unset("recipientId"));

            // Drop only the known legacy indexes after migrating their indexed field.
            dropIndexIfPresent(notifications, LEGACY_RECIPIENT_INDEX);
            dropIndexIfPresent(notifications, LEGACY_UNIQUE_INDEX);

            notifications.createIndex(Indexes.ascending("eventId", "empId", "channel"),
                    new IndexOptions().unique(true).name("event_empId_channel_unique"));
            notifications.createIndex(Indexes.compoundIndex(Indexes.ascending("empId"), Indexes.descending("createdAt")),
                    new IndexOptions().name("empId_created_at_idx"));
            mongoTemplate.getCollection("notification_event_processing").createIndex(
                    Indexes.ascending("eventId"),
                    new IndexOptions().unique(true).name("notification_event_id_unique"));
        };
    }

    private static void dropIndexIfPresent(MongoCollection<Document> collection, String name) {
        for (Document index : collection.listIndexes()) {
            if (name.equals(index.getString("name"))) {
                collection.dropIndex(name);
                return;
            }
        }
    }
}
