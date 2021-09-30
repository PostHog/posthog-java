package com.posthog.java;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class TestSender implements Sender {

    public List<List<JSONObject>> calls = new ArrayList<List<JSONObject>>();

    TestSender() {
    }

    public void send(List<JSONObject> events) {
        calls.add(events);
    }
}
