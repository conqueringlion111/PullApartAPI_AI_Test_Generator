package com.pullapart.api.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pullapart.api.model.Locations;
import com.pullapart.api.endpoint.Location;
import com.pullapart.api.properties.AppConstants;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class LocationTest extends TestBase {

    @Test(groups = {"location"}, description = "test coverage for /location end point")
    public void getEnterpriseLocation() {

        //test data
        String param1 = "siteTypeID";
        String paramValue = "-1";
        String myLocation = "Atlanta North";
        String expectedAddress = "4416 Buford Hwy";
        String expectedCity = "Norcross";

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        Response locationObj =
        given()
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                .param(param1, paramValue)
                .when()
                .get(enterpriseBaseURL.concat(Location.LOCATION.path))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().response();

        //Assert Atlanta North location was returned
        boolean locationFound = false;
        int totalObjCount = locationObj.path("$.size()");

        for (int i = 0; i < totalObjCount; ++i) {
            Map<String, Object> tempData = locationObj.path("[" + i + "]");
            if (tempData.get("locationName").toString().equals(myLocation)) {
                locationFound = true;
                Assert.assertEquals(tempData.get("address1").toString(), expectedAddress, "expected address not returned");
                Assert.assertEquals(tempData.get("cityName").toString(), expectedCity, "expected city not returned");
                break;
            }
        }
        Assert.assertTrue(locationFound, "expected location was not returned");
    }

    @Test(groups = {"location"}, dataProvider = "dataProvider", description = "test coverage for /location end point")
    public void getEnterpriseLocationWithDataProvider(String param1, String paramValue, String myLocation, String expectedAddress,
                                                      String expectedCity) {

        Response locationObj =
                given()
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .param(param1, paramValue)
                        .when()
                        .get(enterpriseBaseURL.concat(Location.LOCATION.path))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract().response();

        //Assert Atlanta North location was returned
        boolean locationFound = false;
        int totalObjCount = locationObj.path("$.size()");

        for (int i = 0; i < totalObjCount; ++i) {
            Map<String, Object> tempData = locationObj.path("[" + i + "]");
            if (tempData.get("locationName").toString().equals(myLocation)) {
                locationFound = true;
                Assert.assertEquals(tempData.get("address1").toString(), expectedAddress, "expected address not returned");
                Assert.assertEquals(tempData.get("cityName").toString(), expectedCity, "expected city not returned");
                break;
            }
        }
        Assert.assertTrue(locationFound, "expected location was not returned");
    }

    @Test(groups = {"location"}, description = "test coverage for /location end point data validation Atlanta North ID")
    public void validateLocationIDAtlantaNorth() {

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        Response locationObj =
                given()
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .param("siteTypeID", "3")
                        .when()
                        .get(enterpriseBaseURL.concat(Location.LOCATION.path))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract().response();
        //Assert expected location ID was returned for Atlanta North location
        boolean locationFound = false;
        int totalObjCount = locationObj.path("$.size()");
        for (int i = 0; i < totalObjCount; ++i) {
            Map<String, Object> tempData = locationObj.path("[" + i + "]");
            if (tempData.get("locationName").toString().equals("Atlanta North")) {
                locationFound = true;
                Assert.assertEquals(tempData.get("locationID").toString(), "4", "expected location ID not returned");
                break;
            }
        }
        Assert.assertTrue(locationFound, "expected location was not returned");
    }

    @Test(groups = {"location"}, description = "test coverage for /location end point data validation deserialize")
    public void validateLocationIDAtlantaNorDeserialize() {

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        Response response = given()
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                .param("siteTypeID", "3")
                .when()
                .get(enterpriseBaseURL.concat(Location.LOCATION.path))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().response();

        // Deserialize full array into List<Location>
        List<Locations> locs = response.jsonPath().getList("", Locations.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            String jsonOutput = mapper.writeValueAsString(locs);
            System.out.println(jsonOutput);
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println();

        // Filter for "Atlanta North"
        Locations atlantaNorth = locs.stream()
                .filter(loc -> "Atlanta North".equals(loc.getLocationName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Atlanta North not found"));

        // Assertions
        Assert.assertEquals(atlantaNorth.getLocationID(), 4, "Expected location ID not returned");
        Assert.assertEquals(atlantaNorth.getCityName(), "Norcross", "City mismatch"); // optional
        Assert.assertEquals(atlantaNorth.getStateName(), "GA", "State mismatch");     // optional
    }

    @Test(groups = {"location"}, dataProvider = "dataProvider", description = "test coverage for /location end point data validation Atlanta North ID")
    public void validateLocationIDAtlantaNorthV2(String param1, String paramValue) {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        Response locationObj =
                given()
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .param(param1, paramValue)
                        .when()
                        .get(enterpriseBaseURL.concat(Location.LOCATION.path))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract().response();
        //Assert expected location ID was returned for Atlanta North location
        boolean locationFound = false;
        int totalObjCount = locationObj.path("$.size()");
        for (int i = 0; i < totalObjCount; ++i) {
            Map<String, Object> tempData = locationObj.path("[" + i + "]");
            if (tempData.get("locationName").toString().equals("Atlanta North")) {
                locationFound = true;
                Assert.assertEquals(tempData.get("locationID").toString(), "4", "expected location ID not returned");
                break;
            }
        }
        Assert.assertTrue(locationFound, "expected location was not returned");
    }

}
