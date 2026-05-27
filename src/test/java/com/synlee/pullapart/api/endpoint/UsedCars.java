package com.pullapart.api.endpoint;

public enum UsedCars {

    UMBRACO("/umbraco"),
    SURFACE("/surface"),
    USED_CARS("/UsedCars"),
    GET_GEO_DATA("/GetGeoDataForZipCode");

    public final String path;
    UsedCars (String path) {
        this.path = path;
    }
}
