package tech.buildrun;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import tech.buildrun.util.DynamicKeyCounter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class DoubleBookingTest {

    @Test
    void miniTest() {
        var username = RandomStringUtils.secure().nextAlphanumeric(6) + "_doublebooking";
        createUser(username, username + "@email.com", "123456");
        var token = authenticate(username, "123456");

        var statusCode = bookEvent(1, 2, token);
    }

    @Test
    void success() throws InterruptedException {

        int N = 1500;
        int eventId = 1;
        int seatId = 3;
        var dynamicKeyCounter = new DynamicKeyCounter();
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(N);

        Runnable userFlow = buildUserFlow(startSignal, eventId, seatId, dynamicKeyCounter, doneSignal);

        dispatchVirtualThreads(N, userFlow, startSignal, doneSignal);

        System.out.println("\nTodas as tarefas finalizaram.");

        System.out.println(dynamicKeyCounter.getCounts());

        assertEquals(1, dynamicKeyCounter.get("200"));
        assertEquals(N - 1, dynamicKeyCounter.get("422"));
    }

    private Runnable buildUserFlow(CountDownLatch startSignal,
                                   int eventId,
                                   int seatId, DynamicKeyCounter dynamicKeyCounter, CountDownLatch doneSignal) {
        Runnable task = () -> {
            String name = Thread.currentThread().toString();
            try {
                var username = RandomStringUtils.secure().nextAlphanumeric(6) + "_doublebooking";

                createUser(username, username + "@email.com", "123456");
                var token = authenticate(username, "123456");

                startSignal.await();

                var startTime = Instant.now().toEpochMilli();

                var statusCode = bookEvent(eventId, seatId, token);

                var endTime = Instant.now().toEpochMilli();

                System.out.println(LocalTime.now() + " " + name + " terminou em " +
                        (endTime - startTime) + " ms com status " + statusCode);

                dynamicKeyCounter.increment(String.valueOf(statusCode));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(LocalTime.now() + " " + name + " interrompida.");

            } finally {
                doneSignal.countDown();
            }
        };
        return task;
    }

    private static void dispatchVirtualThreads(int N,
                                               Runnable task,
                                               CountDownLatch startSignal,
                                               CountDownLatch doneSignal) throws InterruptedException {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int i = 0; i < N; i++) {
                executor.submit(task);
            }

            // Só para garantir que várias já estejam esperando antes de liberar
            Thread.sleep(1500);

            System.out.println("\n=== LIBERANDO TODAS AO MESMO TEMPO! ===\n");
            startSignal.countDown();

            doneSignal.await();
        }
    }

    private void createUser(String username, String email, String password) {
        given()
                .when()
                .baseUri("http://localhost:8080/")
                .body(String.format("""
                    {
                        "username": "%s",
                        "email": "%s",
                        "password": "%s"
                    }        
                """, username, email, password))
                .headers(Map.of("Content-Type", "application/json"))
                .post("/users")
                .then()
                .statusCode(201);
    }

    private String authenticate(String username, String password) {
        return given()
                .when()
                .baseUri("http://localhost:8080/")
                .body(String.format("""
                    {
                        "grantType": "password",
                        "username": "%s",
                        "password": "%s"
                    }        
                """, username, password))
                .headers(Map.of("Content-Type", "application/json"))
                .post("/auth/token")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    private int bookEvent(int eventId, int seatId, String token) {
        var response = given()
                .when()
                .baseUri("http://localhost:8080/")
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
                .headers(Map.of(
                        "Content-Type", "application/json",
                        "Authorization", "Bearer " + token
                ))
                .post("/bookings");
        return response.getStatusCode();
    }
}
