package com.posthog.java;

import java.util.List;

import org.json.JSONObject;

public interface Sender {
    public Boolean send(List<JSONObject> events);
    public JSONObject post(String route, String distinctId);
}
