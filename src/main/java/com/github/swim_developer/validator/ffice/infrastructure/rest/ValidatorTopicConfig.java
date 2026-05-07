package com.github.swim_developer.validator.ffice.infrastructure.rest;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class ValidatorTopicConfig {

    private final List<TopicEntry> topics;

    public ValidatorTopicConfig(
            @ConfigProperty(name = "validator.topic.id", defaultValue = "ffice-v1") String topicId,
            @ConfigProperty(name = "validator.topic.name", defaultValue = "ffice.v1") String topicName,
            @ConfigProperty(name = "validator.topic.description", defaultValue = "FF-ICE Flight Information messages (FIXM 4.3)") String topicDescription) {
        this.topics = List.of(new TopicEntry(topicId, topicName, topicDescription));
    }

    public List<TopicEntry> topicSummaries() {
        return topics;
    }

    public record TopicEntry(String id, String name, String description) {}
}
