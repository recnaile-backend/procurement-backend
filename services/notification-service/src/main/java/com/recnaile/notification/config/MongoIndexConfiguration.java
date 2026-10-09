package com.recnaile.notification.config;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Creates required uniqueness and query indexes at startup; index creation failures fail startup. */
@Configuration
public class MongoIndexConfiguration {
    @Bean
    ApplicationRunner notificationIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            MongoCollection<Document> notifications = mongoTemplate.getCollection("notifications");
            notifications.createIndex(new Document("eventId", 1)
                            .append("recipientId", 1).append("channel", 1),
                    new com.mongodb.client.model.IndexOptions().unique(true).name("event_recipient_channel_unique"));
            notifications.createIndex(new Document("recipientId", 1).append("createdAt", -1),
                    new com.mongodb.client.model.IndexOptions().name("recipient_created_at_idx"));
            mongoTemplate.getCollection("notification_event_processing").createIndex(
                    new Document("eventId", 1),
                    new com.mongodb.client.model.IndexOptions().unique(true).name("notification_event_id_unique"));
        };
    }
}
