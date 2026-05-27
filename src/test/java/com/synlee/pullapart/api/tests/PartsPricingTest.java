package com.pullapart.api.tests;

import com.pullapart.api.endpoint.PartsPrice;
import com.pullapart.api.helper.URIFormatter;
import com.pullapart.api.properties.AppConstants;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class PartsPricingTest extends TestBase {

    @Test(groups = {"partsprice"}, description = "parts price test for generic and premium alternators")
    public void getPriceForAlternator() {
        String part = "alternator";
        URIFormatter url = new URIFormatter();
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        Response parts =
        given()
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON_TEXT_JS_Q_01)
                .when()
                .get(url.formatUri(externalInterchangeURL, PartsPrice.INTERCHANGE.path, PartsPrice.GET_PARTS_TERM_SEARCH.path,
                PartsPrice.FOUR.path, PartsPrice.ZERO.path, part))
                .then().statusCode(200)
                .extract().response();

        //get size of objects returned
        int count = parts.path("$.size()");
        for (int i = 0; i < count; i++) {
            Map<String, Object> tempData = parts.path("[" + i + "]");
            Assert.assertNotNull(tempData.get("price"));
            Float tempPrice = (Float) tempData.get("price");
            if (tempPrice > 0.00) {
                // Assert that the price of alternators are either 26.95 (for simple alternator) or 54.95 (premium)
                Assert.assertTrue(tempPrice.toString().equals("26.95") || tempPrice.toString().equals("54.95"));
            }
        }
    }
}
