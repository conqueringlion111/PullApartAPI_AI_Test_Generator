package com.pullapart.api.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class JsonReader {
    public static Object[][] getData(String jsonPath, Method method) throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(new FileReader(jsonPath)).getAsJsonObject();

        JsonArray testArray = jsonObject.getAsJsonArray(method.getName());
        if (testArray == null) {
            throw new RuntimeException("No test data found for test: " + method.getName());
        }

        Parameter[] parameters = method.getParameters();
        Object[][] dataSet = new Object[testArray.size()][parameters.length];

        for (int i = 0; i < testArray.size(); i++) {
            JsonObject rowObject = testArray.get(i).getAsJsonObject();
            for (int j = 0; j < parameters.length; j++) {
                String paramName = parameters[j].getName(); // Must be compiled with -parameters
                JsonElement value = rowObject.get(paramName);

                if (value == null) {
                    throw new RuntimeException("Missing value for parameter '" + paramName + "' in test data row " + i);
                }
                // Map JSON value to Java type
                if (parameters[j].getType() == String.class) {
                    dataSet[i][j] = value.getAsString();
                } else if (parameters[j].getType() == int.class || parameters[j].getType() == Integer.class) {
                    dataSet[i][j] = value.getAsInt();
                } else if (parameters[j].getType() == boolean.class || parameters[j].getType() == Boolean.class) {
                    dataSet[i][j] = value.getAsBoolean();
                } else if (parameters[j].getType() == double.class || parameters[j].getType() == Double.class) {
                    dataSet[i][j] = value.getAsDouble();
                } else {
                    dataSet[i][j] = value.toString(); // fallback
                }
            }
        }
        return dataSet;
    }
}
