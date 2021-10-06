package com.posthog.java;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import mockit.Mock;
import mockit.MockUp;

public class PostHogTest {
    private PostHog ph;
    private TestSender sender;
    private QueueManager queueManager;
    private String instantExpected = "2020-02-02T02:02:02Z";
    private Clock clock;

    private void updateInstantNow(String instant) {
        instantExpected = instant;
        clock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
    }

    @Before
    public void setUp() {
        updateInstantNow(instantExpected);
        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };
        sender = new TestSender();
        // by default not sleeping just dependent on queue size of 1, i.e. each call
        // separately
        queueManager = new QueueManager.Builder(sender).sleepMs(0).maxTimeInQueue(Duration.ofDays(5)).maxQueueSize(1)
                .build();
        ph = new PostHog.BuilderWithCustomQueueManager(queueManager).build();
    }

    @Test
    public void testCaptureSimple() {
        ph.capture("test id", "test event");
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertEquals("{\"distinct_id\":\"test id\",\"event\":\"test event\",\"timestamp\":\"" + instantExpected + "\"}",
                json.toString());
    }

    // TODO: comprehensive public functions tests

    private void waitUntilQueueEmpty(QueueManager queueManager, int maxWaitTimeMs) throws InterruptedException {
        // we likely don't need to sleep at all, but this is to insure the queueManager
        // thread has time to execute to avoid test flakiness
        while (queueManager.queueSize() > 0 && maxWaitTimeMs > 0) {
            maxWaitTimeMs--;
            Thread.sleep(1);
        }
    }

    @Test
    public void testQueueSize3() throws InterruptedException {
        queueManager = new QueueManager.Builder(sender).sleepMs(0).maxTimeInQueue(Duration.ofDays(5)).maxQueueSize(3)
                .build();
        ph = new PostHog.BuilderWithCustomQueueManager(queueManager).build();
        ph.capture("id1", "first batch event");
        ph.capture("id2", "first batch event");
        ph.capture("id3", "first batch event");
        waitUntilQueueEmpty(queueManager, 100);
        ph.capture("id6", "second batch event");
        ph.shutdown();
        assertEquals(2, sender.calls.size());
        assertEquals(3, sender.calls.get(0).size());
        assertEquals(1, sender.calls.get(1).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertEquals(
                "{\"distinct_id\":\"id1\",\"event\":\"first batch event\",\"timestamp\":\"" + instantExpected + "\"}",
                json.toString());
        json = sender.calls.get(0).get(1);
        assertEquals(
                "{\"distinct_id\":\"id2\",\"event\":\"first batch event\",\"timestamp\":\"" + instantExpected + "\"}",
                json.toString());
        json = sender.calls.get(0).get(2);
        assertEquals(
                "{\"distinct_id\":\"id3\",\"event\":\"first batch event\",\"timestamp\":\"" + instantExpected + "\"}",
                json.toString());
        json = sender.calls.get(1).get(0);
        assertEquals(
                "{\"distinct_id\":\"id6\",\"event\":\"second batch event\",\"timestamp\":\"" + instantExpected + "\"}",
                json.toString());
    }

    @Test
    public void testMaxTimeInQueue() throws InterruptedException {
        queueManager = new QueueManager.Builder(sender).sleepMs(0).maxTimeInQueue(Duration.ofDays(3))
                .maxQueueSize(10000).build();
        ph = new PostHog.BuilderWithCustomQueueManager(queueManager).build();
        String originalInstant = "2020-02-02T02:02:02Z";
        String secondInstant = "2020-02-03T02:02:02Z";
        String thirdInstant = "2020-02-09T02:02:02Z";
        updateInstantNow(originalInstant);
        ph.capture("id1", "first batch event");
        updateInstantNow(secondInstant);
        ph.capture("id2", "first batch event");
        updateInstantNow(thirdInstant);
        waitUntilQueueEmpty(queueManager, 100);
        ph.capture("id6", "second batch event");
        ph.shutdown();
        assertEquals(2, sender.calls.size());
        assertEquals(2, sender.calls.get(0).size());
        assertEquals(1, sender.calls.get(1).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertEquals(
                "{\"distinct_id\":\"id1\",\"event\":\"first batch event\",\"timestamp\":\"" + originalInstant + "\"}",
                json.toString());
        json = sender.calls.get(0).get(1);
        assertEquals(
                "{\"distinct_id\":\"id2\",\"event\":\"first batch event\",\"timestamp\":\"" + secondInstant + "\"}",
                json.toString());
        json = sender.calls.get(1).get(0);
        assertEquals(
                "{\"distinct_id\":\"id6\",\"event\":\"second batch event\",\"timestamp\":\"" + thirdInstant + "\"}",
                json.toString());

    }

}
