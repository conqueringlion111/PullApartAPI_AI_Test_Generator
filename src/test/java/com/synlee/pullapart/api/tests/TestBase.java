package com.pullapart.api.tests;

import com.pullapart.api.utils.JsonReader;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;


public class TestBase {

    protected static String enterpriseBaseURL = "";
    protected static String inventoryBaseURL = "";
    protected static String baseURLOrigin = "";
    protected static String externalInterchangeURL = "";

    @BeforeSuite(alwaysRun = true)
    public void setUp() throws IOException {
        Properties testConfig = new Properties();
        InputStream input = TestBase.class.getClassLoader().getResourceAsStream("config/apitestconfig.properties");
        testConfig.load(input);

        if (System.getProperty("enterprise.env") != null) {
            enterpriseBaseURL = System.getProperty("enterprise.env");
            System.out.println("using " + enterpriseBaseURL);
        } else {
            enterpriseBaseURL = testConfig.getProperty("enterpriseBaseURL");
        }
        if (System.getProperty("inventory.env") != null ) {
            inventoryBaseURL = System.getProperty("inventory.env");
            System.out.println("using " + inventoryBaseURL);
        } else {
            inventoryBaseURL = testConfig.getProperty("inventoryBaseURL");
        }
        if (System.getProperty("origin.env") != null ) {
            baseURLOrigin = System.getProperty("origin.env");
            System.out.println("using " + baseURLOrigin);
        } else {
            baseURLOrigin = testConfig.getProperty("baseURLOrigin");
        }
        if (System.getProperty("externalInterchange.env") != null ) {
            externalInterchangeURL = System.getProperty("externalInterchange.env");
        } else {
            externalInterchangeURL = testConfig.getProperty("externalInterchange");
        }
    }

    @DataProvider(name = "dataProvider")
    public Object[][] passData(Method method) throws Exception {
        String className = this.getClass().getSimpleName();
        String filePath = "src/test/java/com/pullapart/api/dataprovider/" + className + ".json";
        return JsonReader.getData(filePath, method);
    }
}
