package com.pullapart.api.helper;

import java.util.StringJoiner;

public class URIFormatter {

    public final String formatUri(String baseURL, String ... paths) {
        final StringJoiner joiner = new StringJoiner("/")
                .add(baseURL);
        for (String p : paths) {
            joiner.add(p);
        }
        return joiner.toString();
    }
}
