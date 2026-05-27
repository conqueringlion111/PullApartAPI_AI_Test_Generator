package com.pullapart.api.endpoint;

public enum PartsPrice {
    INTERCHANGE("interchange"),
    GET_PARTS_TERM_SEARCH("GetPartsTermSearch"),
    FOUR("4"),
    ZERO("0");

    public final String path;

    PartsPrice(String path) {
        this.path = path;
    }

}
