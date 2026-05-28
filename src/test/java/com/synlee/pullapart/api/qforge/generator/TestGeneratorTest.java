package com.synlee.pullapart.api.qforge.generator;

import com.synlee.qforge.generator.TestGeneratorService;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * QForge — Module 1: AI Test Generator Tests
 *
 * Runs the TestGeneratorService with real endpoint descriptions
 * and validates that Claude returns proper Java test code.
 */

public class TestGeneratorTest {

    private TestGeneratorService generatorService;

    @BeforeClass
    public void setUp() {
        System.out.println("Initializing TestGeneratorTest...");
        generatorService = new TestGeneratorService();
    }

    /**
     * Test 1 — Basic GET endpoint
     * Asks Claude to generate tests for a simple GET /umbraco/surface/UsedCars/GetGeoDataForZipCode endpoint
     */
    @Test
    public void testGenerateForSearchUsedCarLocationByZipcode() {
        String endpointDescription = """
                GET /umbraco/surface/UsedCars/GetGeoDataForZipCode
                query parameters: zipcode=30096, targetUrlAlias=usedCarLocation
                - Returns an array of location objects
                - Expected response fields for each location object: locationId (integer), locationName (string), address1 (string), cityName (string),
                stateName (string), zipCode (string), phoneNumber (string), latitude (float)
                - Assert against each object by utilizing Response response and response.jsonPath
                - Returns 200 OK when locations exist
                - Returns 404 Not Found when invalid zipcode is used
                - Base URL: https://www.pullapart.com
                """;

        String generatedCode = generatorService.generateTests(endpointDescription);

        System.out.println("\n========== GENERATED TEST CODE ==========");
        System.out.println(generatedCode);
        System.out.println("=========================================\n");

        // Validate the generated code contains expected Java/Rest Assured elements
        Assert.assertNotNull(generatedCode, "Generated code should not be null");
        Assert.assertFalse(generatedCode.isBlank(), "Generated code should not be blank");
        Assert.assertTrue(generatedCode.contains("import io.restassured"),
                "Generated code should import Rest Assured");
        Assert.assertTrue(generatedCode.contains("@Test"),
                "Generated code should contain @Test annotations");
        Assert.assertTrue(generatedCode.contains("given()") || generatedCode.contains("RestAssured"),
                "Generated code should use Rest Assured syntax");
        Assert.assertTrue(generatedCode.contains("statusCode"),
                "Generated code should validate status codes");

        System.out.println("[QForge] testGenerateForSearchUsedCarLocationByZipcode — PASSED ");
    }

}
