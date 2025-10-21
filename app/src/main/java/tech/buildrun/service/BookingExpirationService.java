package tech.buildrun.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tech.buildrun.service.dto.CheckBookingStateDto;

import java.io.UncheckedIOException;

@ApplicationScoped
public class BookingExpirationService {

    private static final int expirationCheckSeconds = 10;

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public BookingExpirationService(SqsClient sqsClient,
                                    @ConfigProperty(name = "queue.check-booking-pending-state.url") String queueUrl,
                                    ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
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
