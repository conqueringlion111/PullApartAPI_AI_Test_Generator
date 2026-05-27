package com.pullapart.api.tests;

import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

public class SampleTest extends TestBase {

    @Test()
    public void makePostCall() {

        String payload = "{}";

        given()
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .body(payload)
                .when()
                .post("the base url/path/path")
                .then()
                .statusCode(200);

    }

    @Test()
    public void quickGetCall() {
        given()
                .header("accept", "application/json")
                .when()
                .get("url plus path")
                .then()
                .statusCode(200)
                .body("status", is("approved"));
    }

}
