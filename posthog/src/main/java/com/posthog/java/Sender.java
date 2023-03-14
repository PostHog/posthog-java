package com.posthog.java;

import java.util.List;

import org.json.JSONObject;

public interface Sender {
    void send(List<JSONObject> events);
}
