package tech.buildrun.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import tech.buildrun.service.ExpireBookingService;
import tech.buildrun.service.dto.CheckBookingStateDto;

import java.util.concurrent.CompletionStage;

import static org.slf4j.LoggerFactory.*;

@ApplicationScoped
public class CheckBookingSqsListener {

    private static final Logger logger = getLogger(CheckBookingSqsListener.class);
    private final ExpireBookingService expireBookingService;
    private final ObjectMapper objectMapper;

    public CheckBookingSqsListener(ExpireBookingService expireBookingService,
                                   ObjectMapper objectMapper) {
        this.expireBookingService = expireBookingService;
        this.objectMapper = objectMapper;
    }

    @Incoming("check-booking")
    @Blocking
    @RunOnVirtualThread
    public CompletionStage<Void> receive(Message<String> message) {

        try {
            var dto = objectMapper.readValue(message.getPayload(), CheckBookingStateDto.class);

            logger.atInfo()
                    .addKeyValue("bookingId", dto.bookingId())
                    .log("[Start] receive");

            expireBookingService.expireBookings(dto.bookingId());

            logger.atInfo()
                    .addKeyValue("bookingId", dto.bookingId())
                    .log("[End] receive");

            return message.ack();

        } catch (Exception e) {
            logger.atError()
                    .setCause(e)
                    .log("[Error] receive");
            return message.nack(e);
        }
    }
}
