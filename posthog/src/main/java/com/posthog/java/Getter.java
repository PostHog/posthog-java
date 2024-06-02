package com.posthog.java;

import org.json.JSONObject;

import java.util.Map;

/*
 * Getter interface for making HTTP GET requests to the PostHog API
 */
interface Getter {

    /*
     * Make a GET request to the PostHog API
     *
     * @param route The route to make the GET request to
     * @param headers The headers to include in the GET request
     * @return The JSON response from the GET request
     */
    JSONObject get(String route, Map<String, String> headers);

}
