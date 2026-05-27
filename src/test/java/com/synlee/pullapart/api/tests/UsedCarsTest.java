package com.pullapart.api.tests;

import com.pullapart.api.endpoint.UsedCars;
import com.pullapart.api.properties.AppConstants;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class UsedCarsTest extends TestBase {

    @Test(groups = {"used_cars"}, description = "test coverage for used cars end point")
    public void searchUsedCars() {
        // test data
        String zip = "zipcode";
        String zipCode = "30074";
        String target = "targetUrlAtlas";
        String usedCarLoc = "usedCarLocation";

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        Response searchObj = given()
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                .param(zip, zipCode)
                .param(target, usedCarLoc)
                .when().get(baseURLOrigin.concat(UsedCars.UMBRACO.path).concat(UsedCars.SURFACE.path).concat(UsedCars.USED_CARS.path)
                        .concat(UsedCars.GET_GEO_DATA.path))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().response();

        //get total count of returned search objects
        int objCount = searchObj.path("$.size()");
        for(int i = 0; i < objCount; i++) {
            Map<String, Object> tempData = searchObj.path("[" + i + "]");
            Assert.assertNotNull(tempData.get("locationID"));
            if(!tempData.get("locationName").toString().isEmpty()) {
                Assert.assertNotNull(tempData.get("locationName"));
            } else {
                Assert.fail("expected location name was NOT returned for " + tempData.get("cityName") + " location");
            }
            Assert.assertNotNull(tempData.get("locationName"));
            Assert.assertNotNull(tempData.get("href"));
            Assert.assertTrue(tempData.get("locationName").toString().contains("WELCOME TO PULL-A-PART"));
        }
    }

    @Test(groups = {"used_cars"}, dataProvider = "dataProvider", description = "test coverage for used cars end point - this test" +
            "is expced to fail as location value is missing for Birmingham location")
    public void searchUsedCarsDataProvider(String zip, String zipCode, String target, String usedCarLoc, int statusCode) {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        Response searchObj = given()
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                .param(zip, zipCode)
                .param(target, usedCarLoc)
                .when().get(baseURLOrigin.concat(UsedCars.UMBRACO.path).concat(UsedCars.SURFACE.path).concat(UsedCars.USED_CARS.path)
                        .concat(UsedCars.GET_GEO_DATA.path))
                .then()
                .log().ifValidationFails()
                .statusCode(statusCode)
                .extract().response();

        //get total count of returned search objects
        int objCount = searchObj.path("$.size()");
        for(int i = 0; i < objCount; i++) {
            Map<String, Object> tempData = searchObj.path("[" + i + "]");
            Assert.assertNotNull(tempData.get("locationID"));
            if(!tempData.get("locationName").toString().isEmpty()) {
                Assert.assertNotNull(tempData.get("locationName"));
            } else {
                Assert.fail("expected location name was NOT returned for " + tempData.get("cityName") + " location");
            }
            Assert.assertNotNull(tempData.get("locationName"));
            Assert.assertNotNull(tempData.get("href"));
            Assert.assertTrue(tempData.get("locationName").toString().contains("WELCOME TO PULL-A-PART"));
        }
    }
}
