package tech.buildrun;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.buildrun.entity.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class CreateBookingIT {

    public Long eventId;
    public Long seatId;
    public static String USERNAME = "brunorana";
    public static String PASSWORD = "123456";

    @BeforeEach
    @Transactional
    public void setup() {
        setupAdmin();
        var accessToken = login("admin", "admin");
        createEvent(accessToken);
        listEventSeats(accessToken);

        createUser(USERNAME, PASSWORD);
    }

    @Test
    void shouldCreateBooking() {
        var accessToken = login(USERNAME, PASSWORD);

        given()
                .when()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .body(String.format("""
                        {
                          "eventId": %s,
                          "seats": [
                            {
                              "seatId": %s
                            }
                          ]
                        }
                        """, eventId, seatId))
                .post("/bookings")
                .then()
                .statusCode(200)
                .body("bookingId", Matchers.notNullValue());
    }

    private void setupAdmin() {
        given()
                .when()
                .header("Content-Type", "application/json")
                .body("""
                        {
                          "username": "admin",
                          "password": "admin",
                          "email": "admin@admin.com"
                        }
                        """)
                .post("/setup-admin")
                .then()
                .statusCode(200);
    }

    private void createEvent(String accessToken) {
        var response = given()
                .when()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .body("""
                        {
                          "name": "Final do Mundial",
                          "description": "FIFA",
                          "settings": {
                            "numberOfSeats": 1
                          }
                        }
                        """)
                .post("/events")
                .thenReturn();

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create event: HTTP error code : " + response.statusCode());
        }

        String location = response.header("Location").replaceAll(".*/events/", "");

        eventId = location.isEmpty() ?
                null :
                Long.parseLong(location);
    }

    private String login(String username, String password) {
        var response = given()
                .when()
                .header("Content-Type", "application/json")
                .body(String.format("""
                    {
                      "grantType": "password",
                      "username": "%s",
                      "password": "%s"
                    }
                    """, username, password))
                .post("/auth/token")
                .thenReturn();

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
        }

        return response.getBody().jsonPath().getString("accessToken");
    }

    private void listEventSeats(String accessToken) {
        var response = given()
                .when()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .get(String.format("/events/%s/seats", eventId))
                .thenReturn();

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to list seats: HTTP error code : " + response.statusCode());
        }

        seatId = response.getBody().jsonPath().getLong("data[0].seatId");
    }

    private static void createUser(String username, String password) {
        given()
                .when()
                .header("Content-Type", "application/json")
                .body(String.format("""
                        {
                          "username": "%s",
                          "email": "%s",
                          "password": "%s"
                        }
                        """, username, "test@example.com", password))
                .post("/users")
                .then()
                .statusCode(201);
    }
}
