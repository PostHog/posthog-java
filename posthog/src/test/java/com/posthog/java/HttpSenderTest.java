package com.posthog.java;

import static org.junit.Assert.assertEquals;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class HttpSenderTest {

    public MockWebServer mockWebServer;
    private HttpSender sender;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        var httpUrl = mockWebServer.url("").toString();
        var host = httpUrl.substring(0, httpUrl.length() - 1); // strip trailing /
        var apiKey = "UNIT_TESTING_API_KEY";
        sender = new HttpSender.Builder(apiKey).host(host).build();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testEmpty() {
        sender.send(Collections.emptyList());
    }

    @Test
    public void testOneItem() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse());
        var json = new JSONObject("{'key': 'value'}");
        var input = new ArrayList<JSONObject>();
        input.add(json);
        sender.send(input);
        var request = mockWebServer.takeRequest();
        assertEquals("/batch", request.getPath());
        assertThatJson("{\"api_key\":\"UNIT_TESTING_API_KEY\",\"batch\":[{\"key\":\"value\"}]}")
                .isEqualTo(request.getBody().readUtf8());
    }

    @Test
    public void testMultipleItems() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse());
        var json = new JSONObject("{'key': 'value'}");
        var json2 = new JSONObject("{'key2': 'value2'}");
        var json3 = new JSONObject("{'key3': 'value3'}");
        List<JSONObject> input = new ArrayList<JSONObject>();
        input.add(json);
        input.add(json2);
        input.add(json3);
        sender.send(input);
        var request = mockWebServer.takeRequest();
        assertEquals("/batch", request.getPath());
        assertThatJson("{\"api_key\":\"UNIT_TESTING_API_KEY\",\"batch\":"
                + "[{\"key\":\"value\"},{\"key2\":\"value2\"},{\"key3\":\"value3\"}]}")
                        .isEqualTo(request.getBody().readUtf8());
    }
}
