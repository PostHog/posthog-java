package com.posthog.java;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class TestSender implements Sender {

    public List<List<JSONObject>> calls = new ArrayList<List<JSONObject>>();

    TestSender() {
    }

    public Boolean send(List<JSONObject> events) {
        calls.add(events);
        return true;
    }

    public JSONObject post(String route, String distinctId) {
        String response = "{\n" + //
                "    \"config\": {\n" + //
                "        \"enable_collect_everything\": true\n" + //
                "    },\n" + //
                "    \"toolbarParams\": {},\n" + //
                "    \"isAuthenticated\": false,\n" + //
                "    \"supportedCompression\": [\n" + //
                "        \"gzip\",\n" + //
                "        \"gzip-js\"\n" + //
                "    ],\n" + //
                "    \"featureFlags\": {\n" + //
                "        \"test-flag\": true,\n" + //
                "        \"false-flag\": false\n" + //
                "    },\n" + //
                "    \"sessionRecording\": false,\n" + //
                "    \"errorsWhileComputingFlags\": false,\n" + //
                "    \"featureFlagPayloads\": {\n" + //
                "        \"test-flag\": \"{\\\"key\\\": \\\"value\\\"}\"\n" + //
                "    },\n" + //
                "    \"capturePerformance\": false,\n" + //
                "    \"autocapture_opt_out\": true,\n" + //
                "    \"autocaptureExceptions\": false,\n" + //
                "    \"siteApps\": []\n" + //
                "}";

        return new JSONObject(response);
    }
}
