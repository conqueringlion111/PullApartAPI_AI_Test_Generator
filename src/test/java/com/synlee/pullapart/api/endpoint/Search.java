package com.pullapart.api.endpoint;

public enum Search {

    VEHICLE("/Vehicle"),
    SEARCH("/Search");

    public final String path;

    Search(String path) {
        this.path = path;
    }

}
