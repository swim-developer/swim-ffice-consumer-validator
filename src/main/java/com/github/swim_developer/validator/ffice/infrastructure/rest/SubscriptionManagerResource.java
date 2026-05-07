package com.github.swim_developer.validator.ffice.infrastructure.rest;

import com.github.swim_developer.validator.consumer.domain.model.CreateSubscriptionCommand;
import com.github.swim_developer.validator.consumer.domain.port.in.ManageSubscriptionPort;
import com.github.swim_developer.validator.core.domain.model.QualityOfService;
import com.github.swim_developer.validator.core.domain.model.SubscriptionResponse;
import com.github.swim_developer.validator.core.domain.model.SubscriptionStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Path("/swim/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscriptionManagerResource {

    @Inject
    ManageSubscriptionPort subscriptionPort;

    @Inject
    ValidatorTopicConfig topicConfig;

    @POST
    @Path("/subscriptions")
    public Response createSubscription(Map<String, Object> request) {
        String topic = (String) request.getOrDefault("topic", "ffice.v1");
        String description = (String) request.getOrDefault("description", "");

        String rawQueue = (String) request.get("queueName");
        String queueName = (rawQueue != null && !rawQueue.isBlank()) ? rawQueue : null;
        var command = new CreateSubscriptionCommand(
                topic, queueName, QualityOfService.AT_LEAST_ONCE, true,
                List.of(), List.of(), List.of(), null, null, description, null);
        SubscriptionResponse sub = subscriptionPort.createSubscription(command);

        log.info("Subscription created: id={}, queue={}", sub.subscriptionId(), sub.queue());
        return Response.status(201).entity(toSubscriptionDto(sub)).build();
    }

    @GET
    @Path("/subscriptions")
    public Response listSubscriptions(
            @QueryParam("queueName") String queueName,
            @QueryParam("subscriptionStatus") String status) {
        SubscriptionStatus subStatus = status != null ? SubscriptionStatus.valueOf(status) : null;
        List<SubscriptionResponse> subs = subscriptionPort.listSubscriptions(queueName, subStatus);
        return Response.ok(subs.stream().map(this::toSubscriptionDto).toList()).build();
    }

    @GET
    @Path("/subscriptions/{subscriptionId}")
    public Response getSubscription(@PathParam("subscriptionId") String subscriptionId) {
        return subscriptionPort.getSubscriptionDetails(subscriptionId)
                .map(sub -> Response.ok(toSubscriptionDto(sub)).build())
                .orElse(Response.status(404).build());
    }

    @PUT
    @Path("/subscriptions/{subscriptionId}")
    public Response updateSubscriptionStatus(
            @PathParam("subscriptionId") String subscriptionId,
            Map<String, String> body) {
        String newStatus = body.getOrDefault("subscription_status",
                body.getOrDefault("subscriptionStatus", "ACTIVE"));
        SubscriptionStatus status = SubscriptionStatus.valueOf(newStatus);
        SubscriptionResponse sub = subscriptionPort.updateSubscriptionStatus(subscriptionId, status);
        return Response.ok(toSubscriptionDto(sub)).build();
    }

    @DELETE
    @Path("/subscriptions/{subscriptionId}")
    public Response deleteSubscription(@PathParam("subscriptionId") String subscriptionId) {
        subscriptionPort.deleteSubscription(subscriptionId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/subscriptions/{subscriptionId}/renew")
    public Response renewSubscription(@PathParam("subscriptionId") String subscriptionId) {
        return subscriptionPort.renewSubscription(subscriptionId)
                .map(sub -> Response.ok(toSubscriptionDto(sub)).build())
                .orElse(Response.status(404).build());
    }

    @GET
    @Path("/topics")
    public Response listTopics() {
        var topics = topicConfig.topicSummaries().stream()
                .map(t -> Map.of(
                        "topicId", t.id(),
                        "title", t.name(),
                        "description", t.description()))
                .toList();
        return Response.ok(Map.of("topics", topics)).build();
    }

    @GET
    @Path("/topics/{topicId}")
    public Response getTopicDetails(@PathParam("topicId") String topicId) {
        return topicConfig.topicSummaries().stream()
                .filter(t -> t.id().equals(topicId))
                .findFirst()
                .map(t -> Response.ok(Map.of(
                        "topicId", t.id(),
                        "topicName", t.name(),
                        "description", t.description(),
                        "publisherState", "ACTIVE")).build())
                .orElse(Response.status(404).build());
    }

    @GET
    @Path("/features")
    @Produces(MediaType.APPLICATION_XML)
    public Response getFeatures(
            @QueryParam("typeName") String typeName,
            @QueryParam("filter") String filter,
            @QueryParam("validTime") String validTime) {
        return Response.ok("<FeatureCollection/>").build();
    }

    private Map<String, Object> toSubscriptionDto(SubscriptionResponse sub) {
        return Map.of(
                "subscriptionId", sub.subscriptionId().toString(),
                "subscriptionStatus", sub.subscriptionStatus().name(),
                "queueName", sub.queue() != null ? sub.queue() : "",
                "subscriptionEnd", sub.subscriptionEnd() != null
                        ? sub.subscriptionEnd().toString()
                        : Instant.now().plusSeconds(86400).toString());
    }
}
