package com.posthog.java;

import static org.junit.Assert.assertEquals;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class HttpSenderTest {

    public MockWebServer mockWebServer;
    private HttpSender sender;
    private String apiKey = "UNIT_TESTING_API_KEY";

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String httpUrl = mockWebServer.url("").toString();
        String host = httpUrl.substring(0, httpUrl.length() - 1); // strip trailing /
        sender = new HttpSender.Builder(apiKey).host(host).maxRetries(1).build();
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
        JSONObject json = new JSONObject("{'key': 'value'}");
        List<JSONObject> input = new ArrayList<JSONObject>();
        input.add(json);
        sender.send(input);
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/batch", request.getPath());
        assertThatJson("{\"api_key\":\"UNIT_TESTING_API_KEY\",\"batch\":[{\"key\":\"value\"}]}")
                .isEqualTo(request.getBody().readUtf8());
    }

    @Test
    public void testMultipleItems() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse());
        JSONObject json = new JSONObject("{'key': 'value'}");
        JSONObject json2 = new JSONObject("{'key2': 'value2'}");
        JSONObject json3 = new JSONObject("{'key3': 'value3'}");
        List<JSONObject> input = new ArrayList<JSONObject>();
        input.add(json);
        input.add(json2);
        input.add(json3);
        sender.send(input);
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/batch", request.getPath());
        assertThatJson("{\"api_key\":\"UNIT_TESTING_API_KEY\",\"batch\":"
                + "[{\"key\":\"value\"},{\"key2\":\"value2\"},{\"key3\":\"value3\"}]}")
                .isEqualTo(request.getBody().readUtf8());
    }

    @Test
    public void testHandlesInitial503ErrorWithRetry() throws InterruptedException {
        // 503 is a server error, so we should retry
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse());
        JSONObject json = new JSONObject("{'key': 'value'}");
        List<JSONObject> input = new ArrayList<JSONObject>();
        input.add(json);
        Boolean success = sender.send(input);
        assertEquals(success, true);
    }

    @Test
    public void testOnlyMakesOneRequestOnSuccess() throws InterruptedException {
        // 200 is a success, so we should not retry
        mockWebServer.enqueue(new MockResponse());
        JSONObject json = new JSONObject("{'key': 'value'}");
        List<JSONObject> input = new ArrayList<JSONObject>();
        input.add(json);
        Boolean success = sender.send(input);
        assertEquals(success, true);

        // Now verify that we only
        RecordedRequest firstRequest = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS);
        assertEquals(firstRequest.getPath(), "/batch");
        RecordedRequest secondRequest = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS);
        assertEquals(secondRequest, null);
    }

    @Test
    public void testDoesNotRetryOnClientErrors() throws InterruptedException {
        // 400 is a client error, so we should not retry
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        mockWebServer.enqueue(new MockResponse());
        JSONObject json = new JSONObject("{'key': 'value'}");
        List<JSONObject> input = new ArrayList<JSONObject>();
        input.add(json);
        Boolean success = sender.send(input);
        assertEquals(success, false);

        // Now verify that we only
        RecordedRequest firstRequest = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS);
        assertEquals(firstRequest.getPath(), "/batch");
        RecordedRequest secondRequest = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS);
        assertEquals(secondRequest, null);
    }

    @Test
    public void testReturnFalseOnRetriesExhausted() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        JSONObject json = new JSONObject("{'key': 'value'}");
        List<JSONObject> input = new ArrayList<JSONObject>();
        input.add(json);

        Boolean success = sender.send(input);
        assertEquals(success, false);

        // Verify we made two requests
        RecordedRequest firstRequest = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS);
        assertEquals(firstRequest.getPath(), "/batch");
        RecordedRequest secondRequest = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS);
        assertEquals(secondRequest.getPath(), "/batch");
    }
}
