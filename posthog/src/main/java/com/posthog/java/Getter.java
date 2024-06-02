package com.posthog.java;

import org.json.JSONObject;

import java.util.Map;

interface Getter {

    JSONObject get(String route, Map<String, String> headers);

}
