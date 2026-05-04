package tech.buildrun.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tech.buildrun.service.dto.CheckBookingStateDto;

import java.io.UncheckedIOException;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class BookingExpirationService {

    private static final Logger logger = getLogger(BookingExpirationService.class);

    @ConfigProperty(name = "booking.expiration.check.seconds")
    private int expirationCheckSeconds;

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public BookingExpirationService(SqsClient sqsClient,
                                    @ConfigProperty(name = "mp.messaging.incoming.check-booking.queue.url") String queueUrl,
                                    ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
    }

    public void scheduleExpirationCheck(Long bookingId) {

        logger.atInfo()
                .addKeyValue("bookingId", bookingId)
                .log("[Start] scheduleExpirationCheck");

        try {
            var dto = new CheckBookingStateDto(bookingId);
            var body = objectMapper.writeValueAsString(dto);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .delaySeconds(expirationCheckSeconds)
                    .messageBody(body)
                    .build());

            logger.atInfo()
                    .addKeyValue("bookingId", bookingId)
                    .addKeyValue("delaySeconds", expirationCheckSeconds)
                    .log("[End] scheduleExpirationCheck");

        } catch (JsonProcessingException e) {
            logger.atError()
                    .addKeyValue("bookingId", bookingId)
                    .setCause(e)
                    .log("[Error] scheduleExpirationCheck");
            throw new UncheckedIOException(e);
        }
    }
}
