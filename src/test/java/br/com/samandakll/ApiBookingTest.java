package br.com.samandakll;

import br.com.samandakll.entities.Booking;
import br.com.samandakll.entities.BookingDates;
import br.com.samandakll.entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public class ApiBookingTest {

    public static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;

    @BeforeAll
    public static void contextoInicial(){
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();

        user = new User(faker.name().username(), faker.name().firstName(), faker.name().lastName(),
                faker.internet().safeEmailAddress(), faker.internet().password(8,10), faker.phoneNumber().toString());

        bookingDates = new BookingDates("2023-01-02", "2023-01-04");

        booking = new Booking(user.getFirstName(), user.getLastName(),
                (float) faker.number().randomDouble(2, 50, 10000), true,
                bookingDates, "");

        RestAssured.filters(new RequestLoggingFilter(), new RequestLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest(){
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123");
    }

    @Test
    public void getAllBookings_returnOk(){
        Response response = request
                .when().get("/booking")
                .then().extract()
                .response();


        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void getAllBookingsById_returnOk(){
        Response response = request
                .when().get("/booking/1")
                .then().extract()
                .response();


        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void getAllBookingsByUserFirstName_BookingExists_returnOk(){
        request
                .when()
                .queryParam("firstName", "Samanda")
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0))).assertThat();
    }

    @Test
    public void createBooking_WithValidData_returnOk(){
        request
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .body(matchesJsonSchemaInClasspath("schemaBookingRequest.json"))
                .and()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON).and().time(lessThan(2000L));

    }

    @Test
    public void createBooking_WithInvalidData_returnError(){
        request
                .when()
                .body(bookingDates)
                .post("/booking")
                .then()
                .assertThat()
                .statusCode(500)
                .and().time(lessThan(2000L));

    }

    @Test
    public void getHealthCheck_returnOk(){
        Response response = request
                .when()
                .body(booking)
                .get("/ping")
                .then().extract().response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(201, response.statusCode());
    }

    @Test
    public void getHealthCheck_returnNotFound(){
        Response response = request
                .when()
                .body(booking)
                .get("/check")
                .then().extract().response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(404, response.statusCode());
    }
}
