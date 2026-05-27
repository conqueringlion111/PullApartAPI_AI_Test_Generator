package com.pullapart.api.endpoint;

public enum Location {
    LOCATION("/Location");

    public final String path;

    Location(String path) {
        this.path = path;
    }
}
