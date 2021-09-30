package com.posthog.java;

import java.util.List;

import org.json.JSONObject;

public interface Sender {
    public void send(List<JSONObject> events);
}
