package tech.buildrun.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tech.buildrun.service.dto.CheckBookingStateDto;

import java.io.UncheckedIOException;

@ApplicationScoped
public class BookingExpirationService {

    @ConfigProperty(name = "booking.expiration.check.seconds")
    private int expirationCheckSeconds;

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public BookingExpirationService(SqsClient sqsClient,
                                    @ConfigProperty(name = "queue.check-booking-pending-state.name") String queueName,
                                    ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = resolveQueueUrl(queueName);
        this.objectMapper = objectMapper;
    }

    private String resolveQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build())
            .queueUrl();
    }

    public void scheduleExpirationCheck(Long bookingId) {

        try {
            var dto = new CheckBookingStateDto(bookingId);
            var body = objectMapper.writeValueAsString(dto);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .delaySeconds(expirationCheckSeconds)
                    .messageBody(body)
                    .build());

        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
