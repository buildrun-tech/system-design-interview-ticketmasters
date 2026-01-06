package tech.buildrun;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class CreateUserIT {

    @Test
    void createUserIT() {
        var username = "bruno";
        var email = "bruno@buildrun.com.br";
        var password = "1234";

        given()
          .when()
                .header("Content-Type", "application/json")
                .body(String.format("""
                        {
                          "username": "%s",
                          "email": "%s",
                          "password": "%s"
                        }
                        """, username, email, password))
                .post("/users")
          .then()
             .statusCode(201)
             .header("Location", containsString("/users/1"));
    }

}