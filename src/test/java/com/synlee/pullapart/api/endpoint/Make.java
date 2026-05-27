package com.pullapart.api.endpoint;

public enum Make {
    MAKE("/Make");
    public final String path;

    Make(String path) {
        this.path = path;
    }
}
