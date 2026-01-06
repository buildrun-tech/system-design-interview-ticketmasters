package tech.buildrun;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import tech.buildrun.entity.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class CreateBookingIT {

    @Test
    @Transactional
    void shouldCreateBooking() {
        var username = "brunorana";
        var password = "123456";
        createUser(username, password);
        var event = createEvent();

        // TODO - implement the login (JWT)
        // TODO - Call create booking API

//        var eventId = 1;
//        var seatId = 1;
//        var jwt = "";
//
//        given()
//                .when()
//                .header("Content-Type", "application/json")
//                .header("Authorization", "Bearer " + jwt)
//                .body(String.format("""
//                        {
//                          "eventId": %s,
//                          "seats": [
//                            {
//                              "seatId": %s
//                            }
//                          ]
//                        }
//                        """, eventId, seatId))
//                .post("/users")
//                .then()
//                .statusCode(201)
//                .header("Location", containsString("/users/1"));
    }

    private EventEntity createEvent() {
        Set<SeatEntity> seats = new HashSet<>();

        EventEntity event = new EventEntity();
        event.name = "Concert";
        event.description = "Live concert event";

        seats.add(new SeatEntity(event, "A1", SeatStatus.AVAILABLE));

        event.seats = seats;
        event.persist();

        return event;
    }

    private static UserEntity createUser(String username, String password) {
        var user = new UserEntity();
        user.email = "bruno@buildrun.com.br";
        user.role = userRole();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);

        user.persist();

        return user;
    }

    private static RoleEntity userRole() {
        var role = new RoleEntity();
        role.id = 1L;
        return role;
    }
}
