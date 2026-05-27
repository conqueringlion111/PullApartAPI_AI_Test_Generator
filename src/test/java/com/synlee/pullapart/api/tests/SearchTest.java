package com.pullapart.api.tests;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.pullapart.api.endpoint.Search;
import com.pullapart.api.helper.URIFormatter;
import com.pullapart.api.model.Vehicle;
import com.pullapart.api.payload.SearchPayload;
import com.pullapart.api.properties.AppConstants;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.pullapart.api.payload.SearchPayload.vehicleSearchPayload;
import static io.restassured.RestAssured.given;

public class SearchTest extends TestBase {

    @Test(groups = {"search"}, description = "test coverage for /Vehicle/Search end point")
    public void searchVehicle() throws JsonProcessingException {

        String searchedMake = "INFINITI";
        String searchedModel = "Q45";
        List<Integer> locations = List.of(4);
        int MakeID = 29;
        List<Integer> Models = List.of(720);
        List<Integer> Years = List.of();
        //Create the search request payload with the above test data
        String payload = SearchPayload.searchPayload(locations, MakeID, Models, Years);

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        //make the POST search call
        Response searchObj =
        given()
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                .body(payload)
                .when()
                .post(inventoryBaseURL.concat(Search.VEHICLE.path).concat(Search.SEARCH.path))
                .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        //If search results are returned, assert that the initially searched make and model are returned
        int totalObj = searchObj.path("$.size()");
        if (totalObj > 0) {
            int totalExact = searchObj.path("[0].exact.size()");
            for (int i = 0; i < totalExact; ++i) {
                Map<String, Object> tempData = searchObj.path("[0].exact[" + i + "]");
                Assert.assertEquals(tempData.get("makeName").toString(), searchedMake, "the initially searched make was not returned");
                Assert.assertEquals(tempData.get("modelName").toString(), searchedModel, "the searched model was not returned");
            }
        }
    }

    @Test(groups = {"search"}, description = "test coverage for /Vehicle/Search end point using vehicle pojo")
    public void searchVehicleWithPoJo() throws JsonProcessingException {

        String searchedMake = "INFINITI";
        String searchedModel = "Q45";
        List<Integer> locations = List.of(4);
        int makeId = 29;
        List<Integer> models = List.of(720);
        List<Integer> years = List.of();
        //Create the search request payload with the new pojo class
        String payload = vehicleSearchPayload(locations, makeId, models, years);

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        //make the POST search call
        Response searchObj =
                given()
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                        .body(payload)
                        .when()
                        .post(inventoryBaseURL.concat(Search.VEHICLE.path).concat(Search.SEARCH.path))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract().response();
        //If search results are returned, assert that the initially searched make and model are returned
        int totalObj = searchObj.path("$.size()");
        if (totalObj > 0) {
            int totalExact = searchObj.path("[0].exact.size()");
            for (int i = 0; i < totalExact; ++i) {
                Map<String, Object> tempData = searchObj.path("[0].exact[" + i + "]");
                Assert.assertEquals(tempData.get("makeName").toString(), searchedMake, "the initially searched make was not returned");
                Assert.assertEquals(tempData.get("modelName").toString(), searchedModel, "the searched model was not returned");
            }
        }
    }

    @Test(groups = {"search"}, description = "test coverage for /Vehicle/Search end point passing vehicle pojo into call")
    public void searchVehicleWithDirectPOJO() {

        String searchedMake = "INFINITI";
        String searchedModel = "Q45";
        List<Integer> locations = List.of(4);
        int makeId = 29;
        List<Integer> models = List.of(720);
        List<Integer> years = List.of();
        //Pass the pojo directly into the request
        Vehicle payload = new Vehicle(locations, makeId, models, years);
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        //make the POST search call
        Response searchObj =
                given()
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                        .body(payload)
                        .when()
                        .post(inventoryBaseURL.concat(Search.VEHICLE.path).concat(Search.SEARCH.path))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract().response();
        //If search results are returned, assert that the initially searched make and model are returned
//        int totalObj = searchObj.path("$.size()");
        int totalObj = searchObj.jsonPath().getList("$").size();
        if (totalObj > 0) {
            int totalExact = searchObj.path("[0].exact.size()");
            for (int i = 0; i < totalExact; ++i) {
                Map<String, Object> tempData = searchObj.path("[0].exact[" + i + "]");
                Assert.assertEquals(tempData.get("makeName").toString(), searchedMake, "the initially searched make was not returned");
                Assert.assertEquals(tempData.get("modelName").toString(), searchedModel, "the searched model was not returned");
            }
        }
    }


    @Test(groups = {"search"}, dataProvider = "dataProvider", description = "test coverage for /Vehicle/Search advanced search" +
            "using testNG data provider to bring in the test data")
    public void searchVehicleWithDataProvider(String searchedMake, String searchedModel) throws JsonProcessingException {

        List<Integer> locations = List.of(4);
        int MakeID = 29;
        List<Integer> Models = List.of(720);
        List<Integer> Years = List.of();
        //Create the search request payload with the above test data
        String payload = SearchPayload.searchPayload(locations, MakeID, Models, Years);
        //make the POST search call

        Response searchObj =
                given()
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                        .body(payload)
                        .when()
                        .post(inventoryBaseURL.concat(Search.VEHICLE.path).concat(Search.SEARCH.path))
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract().response();
        //If search results are returned, assert that the initially searched make and model are returned
        int totalObj = searchObj.path("$.size()");
        if (totalObj > 0) {
            int totalExact = searchObj.path("[0].exact.size()");
            for (int i = 0; i < totalExact; ++i) {
                Map<String, Object> tempData = searchObj.path("[0].exact[" + i + "]");
                Assert.assertEquals(tempData.get("makeName").toString(), searchedMake, "the initially searched make was not returned");
                Assert.assertEquals(tempData.get("modelName").toString(), searchedModel, "the searched model was not returned");
            }
        }
    }

    @Test(groups = {"search"}, description = "test coverage for /Vehicle/Search end point")
    public void advancedVehiclesearch() throws JsonProcessingException {

        String searchedMake = "INFINITI";
        String searchedModel = "Q45";
        int  years = 1994;
        List<Integer> locations = Arrays.asList(21, 4, 3);
        int MakeID = 29;
        List<Integer> Models = List.of(720);
        List<Integer> Years = List.of(years);
        //Create the search request payload with the above test data
        String payload = SearchPayload.searchPayload(locations, MakeID, Models, Years);
        //make the POST search call

        Response searchObj =
                given()
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                        .body(payload)
                        .when()
                        .post(inventoryBaseURL.concat(Search.VEHICLE.path).concat(Search.SEARCH.path))
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract().response();
        //If search results are returned, assert that the initially searched make and model are returned, note exact year match may not be available
        int totalObj = searchObj.path("$.size()");
        if (totalObj > 0) {
            int totalExact = searchObj.path("[0].exact.size()");
            for (int i = 0; i < totalExact; ++i) {
                Map<String, Object> tempData = searchObj.path("[0].exact[" + i + "]");
                Assert.assertEquals(tempData.get("makeName").toString(), searchedMake, "the initially searched make was not returned");
                Assert.assertEquals(tempData.get("modelName").toString(), searchedModel, "the searched model was not returned");
            }
        }

        //To assert no cookies are returned
        Assert.assertTrue(searchObj.getCookies().isEmpty());
    }

    @Test(groups = {"search"}, description = "test coverage for /Vehicle/Search end point using URI Formatter")
    public void advancedVehiclesearchUsingURIFormatter() throws JsonProcessingException {

        String searchedMake = "INFINITI";
        String searchedModel = "Q45";
        int  years = 1994;
        List<Integer> locations = Arrays.asList(21, 4, 3);
        int MakeID = 29;
        List<Integer> Models = List.of(720);
        List<Integer> Years = List.of(years);
        //Create the search request payload with the above test data
        String payload = SearchPayload.searchPayload(locations, MakeID, Models, Years);
        //make the POST search call
        URIFormatter format = new URIFormatter();
        //To see full log
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        Response searchObj =
                given()
                        .urlEncodingEnabled(false)
                        .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                        .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                        .body(payload)
                        .when()
                        .post(format.formatUri(inventoryBaseURL, Search.VEHICLE.path.replace("/", ""), Search.SEARCH.path.replace("/", "")))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract().response();
        //If search results are returned, assert that the initially searched make and model are returned, note exact year match may not be available
        int totalObj = searchObj.path("$.size()");
        if (totalObj > 0) {
            int totalExact = searchObj.path("[0].exact.size()");
            for (int i = 0; i < totalExact; ++i) {
                Map<String, Object> tempData = searchObj.path("[0].exact[" + i + "]");
                Assert.assertEquals(tempData.get("makeName").toString(), searchedMake, "the initially searched make was not returned");
                Assert.assertEquals(tempData.get("modelName").toString(), searchedModel, "the searched model was not returned");
            }
        }
    }
}
