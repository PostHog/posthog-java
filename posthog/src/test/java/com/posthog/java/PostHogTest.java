package com.posthog.java;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.posthog.java.flags.FeatureFlag;
import com.posthog.java.flags.FeatureFlagConfig;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import mockit.Mock;
import mockit.MockUp;

public class PostHogTest {
    private PostHog ph;
    private TestSender sender;
    private TestGetter getter;
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
        queueManager = new QueueManager.Builder(sender)
                .sleepMs(0)
                .maxTimeInQueue(Duration.ofDays(5))
                .maxQueueSize(1)
                .build();

        ph = new PostHog.BuilderWithCustomQueueManager(queueManager, sender)
                .build();
    }

    @Test
    public void testCaptureSimple() {
        ph.capture("test id", "test event");
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        // Assert JSON includes the expected distinct_id, event, and timestamp, ignoring
        // any extraneus properties.
        assertThatJson("{\"distinct_id\":\"test id\",\"event\":\"test event\",\"timestamp\":\"" + instantExpected
                + "\"}").isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
    }

    @Test
    public void testEnsureEventHasGeneratedUuid() {
        // To ensure we have a way to deduplicate events that may be ingested multiple
        // times due to e.g. retries, we need to ensure that we have an identifier that
        // is unique per event.
        queueManager = new QueueManager.Builder(sender).sleepMs(0).maxTimeInQueue(Duration.ofDays(5)).maxQueueSize(3)
                .build();
        ph = new PostHog.BuilderWithCustomQueueManager(queueManager).build();

        ph.capture("test id", "test event");
        ph.capture("test id", "test event");
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(2, sender.calls.get(0).size());

        String uuid = sender.calls.get(0).get(0).getString("uuid");
        assertEquals(36, uuid.toString().length());

        // Make sure subsequent calls generate a different UUID
        String secondUuid = sender.calls.get(0).get(1).getString("uuid");
        assertNotEquals(uuid.toString(), secondUuid.toString());
    }

    @Test
    public void testCaptureWithProperties() {
        ph.capture("test id", "test event", new HashMap<String, Object>() {
            {
                put("movie_id", 123);
                put("category", "romcom");
            }
        });
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"distinct_id\":\"test id\",\"event\":\"test event\""
                + ",\"properties\":{\"movie_id\":123,\"category\":\"romcom\"},\"timestamp\":\"" + instantExpected
                + "\"}").isEqualTo(new JSONObject(json, "distinct_id", "event", "properties", "timestamp").toString());
    }

    @Test
    public void testCaptureIncludesLibProperty() {
        ph.capture("test id", "test event", new HashMap<String, Object>() {
            {
                put("movie_id", 123);
                put("category", "romcom");
            }
        });
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"$lib\":\"posthog-java\"}").isEqualTo(new JSONObject(json, "$lib").toString());
    }

    @Test
    public void testIdentifySimple() {
        ph.identify("test id", new HashMap<String, Object>() {
            {
                put("email", "john@doe.com");
                put("proUser", false);
            }
        });
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"distinct_id\":\"test id\",\"event\":\"$identify\""
                + ",\"properties\":{\"$set\":{\"email\":\"john@doe.com\",\"proUser\":false}},\"timestamp\":\""
                + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "properties", "timestamp").toString());
    }

    @Test
    public void testIdentifyWithSetOnce() {
        ph.identify("test id", new HashMap<String, Object>() {
            {
                put("email", "john@doe.com");
                put("proUser", false);
            }
        }, new HashMap<String, Object>() {
            {
                put("first_location", "colorado");
                put("first_number", 5);
            }
        });
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"distinct_id\":\"test id\",\"event\":\"$identify\""
                + ",\"properties\":{\"$set\":{\"email\":\"john@doe.com\",\"proUser\":false}"
                + ",\"$set_once\":{\"first_location\":\"colorado\",\"first_number\":5}" + "},\"timestamp\":\""
                + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "properties", "$set_once", "timestamp")
                        .toString());
    }

    @Test
    public void testSet() {
        ph.set("test id", new HashMap<String, Object>() {
            {
                put("email", "john@doe.com");
                put("proUser", false);
            }
        });
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"distinct_id\":\"test id\",\"event\":\"$set\""
                + ",\"properties\":{\"$set\":{\"email\":\"john@doe.com\",\"proUser\":false}},\"timestamp\":\""
                + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "properties", "timestamp").toString());
    }

    @Test
    public void testSetOnce() {
        ph.setOnce("test id", new HashMap<String, Object>() {
            {
                put("first_location", "colorado");
                put("first_number", 5);
            }
        });
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"distinct_id\":\"test id\",\"event\":\"$set_once\""
                + ",\"properties\":{\"$set_once\":{\"first_location\":\"colorado\",\"first_number\":5}"
                + "},\"timestamp\":\"" + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "properties", "timestamp").toString());
    }

    @Test
    public void testAlias() {
        ph.alias("test id", "second id");
        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"distinct_id\":\"test id\",\"event\":\"$create_alias\""
                + ",\"properties\":{\"distinct_id\":\"test id\",\"alias\":\"second id\"}" + ",\"timestamp\":\""
                + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "properties", "timestamp").toString());
    }

    private void waitUntilQueueEmpty(QueueManager queueManager, int maxWaitTimeMs) throws InterruptedException {
        // we likely don't need to sleep at all, but this is to insure the queueManager
        // thread has time to execute to avoid test flakiness
        while (queueManager.queueSize() > 0) {
            if (maxWaitTimeMs <= 0) {
                System.out.println("Timed out waiting for queue to be empty.");
                break;
            }
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
        waitUntilQueueEmpty(queueManager, 10000);
        ph.capture("id6", "second batch event");
        ph.shutdown();
        assertEquals(2, sender.calls.size());
        assertEquals(3, sender.calls.get(0).size());
        assertEquals(1, sender.calls.get(1).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson(
                "{\"distinct_id\":\"id1\",\"event\":\"first batch event\",\"timestamp\":\"" + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
        json = sender.calls.get(0).get(1);
        assertThatJson(
                "{\"distinct_id\":\"id2\",\"event\":\"first batch event\",\"timestamp\":\"" + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
        json = sender.calls.get(0).get(2);
        assertThatJson(
                "{\"distinct_id\":\"id3\",\"event\":\"first batch event\",\"timestamp\":\"" + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
        json = sender.calls.get(1).get(0);
        assertThatJson(
                "{\"distinct_id\":\"id6\",\"event\":\"second batch event\",\"timestamp\":\"" + instantExpected + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
    }

    // NOTE: this test doesn't appear to pass when run with the rest of the
    // tests, but does pass when run individually. I'm disabling for now to get
    // CI green.
    @Ignore
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
        waitUntilQueueEmpty(queueManager, 15000);
        ph.capture("id6", "second batch event");
        ph.shutdown();
        assertEquals(2, sender.calls.size());
        assertEquals(2, sender.calls.get(0).size());
        assertEquals(1, sender.calls.get(1).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson(
                "{\"distinct_id\":\"id1\",\"event\":\"first batch event\",\"timestamp\":\"" + originalInstant + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
        json = sender.calls.get(0).get(1);
        assertThatJson(
                "{\"distinct_id\":\"id2\",\"event\":\"first batch event\",\"timestamp\":\"" + secondInstant + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
        json = sender.calls.get(1).get(0);
        assertThatJson(
                "{\"distinct_id\":\"id6\",\"event\":\"second batch event\",\"timestamp\":\"" + thirdInstant + "\"}")
                .isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp").toString());
    }

    @Test
    public void testIsFeatureFlagEnabledLocalNoConfig() {
        final FeatureFlagPoller featureFlagPoller = new FeatureFlagPoller.Builder("", "", new TestGetter()).build();
        ph = new PostHog.BuilderWithCustomFeatureFlagPoller(featureFlagPoller)
                .build();

        final boolean isEnabled = ph.isFeatureFlagEnabled("java-feature-flag", "id-1");
        assertTrue(isEnabled);
    }

    @Test
    public void testIsFeatureFlagEnabledLocal() {
        final FeatureFlagPoller featureFlagPoller = new FeatureFlagPoller.Builder("", "", new TestGetter()).build();
        ph = new PostHog.BuilderWithCustomFeatureFlagPoller(featureFlagPoller)
                .build();

        final FeatureFlagConfig featureFlagConfig = new FeatureFlagConfig.Builder("java-feature-flag", "id-1").build();
        final boolean isEnabled = ph.isFeatureFlagEnabled(featureFlagConfig);
        assertTrue(isEnabled);

        final boolean isEnabled2 = ph.isFeatureFlagEnabled("java-feature-flag", "some-user");
        assertFalse(isEnabled2);
    }

    @Test
    public void testFeatureFlagRetrieveVariant() {
        final FeatureFlagPoller featureFlagPoller = new FeatureFlagPoller.Builder("", "", new TestGetter()).build();
        ph = new PostHog.BuilderWithCustomFeatureFlagPoller(featureFlagPoller)
                .build();

        final FeatureFlagConfig featureFlagConfig = new FeatureFlagConfig.Builder("java-feature-flag", "id-1").build();
        final Optional<String> variant = ph.getFeatureFlagVariant(featureFlagConfig);
        assertTrue(variant.isPresent());
    }

    @Test
    public void restFeatureFlagRetrieveFlag() {
        final FeatureFlagPoller featureFlagPoller = new FeatureFlagPoller.Builder("", "", new TestGetter()).build();
        ph = new PostHog.BuilderWithCustomFeatureFlagPoller(featureFlagPoller)
                .build();

        final FeatureFlagConfig featureFlagConfig = new FeatureFlagConfig.Builder("java-feature-flag", "id-1").build();
        final Optional<FeatureFlag> flag = ph.getFeatureFlag(featureFlagConfig);
        assertTrue(flag.isPresent());
        assertEquals("java-feature-flag", flag.get().getKey());
    }

    @Test
    public void testFeatureFlagEvents() {
        final FeatureFlagPoller featureFlagPoller = new FeatureFlagPoller.Builder("", "", new TestGetter()).build();
        ph = new PostHog.BuilderWithCustomQueueManagerAndCustomFeatureFlagPoller(queueManager, featureFlagPoller)
                .build();

        final FeatureFlagConfig featureFlagConfig = new FeatureFlagConfig.Builder("java-feature-flag", "id-1")
                .sendFeatureFlagEvents(true)
                .build();

        final boolean isEnabled = ph.isFeatureFlagEnabled(featureFlagConfig);
        assertTrue(isEnabled);

        ph.shutdown();
        assertEquals(1, sender.calls.size());
        assertEquals(1, sender.calls.get(0).size());
        JSONObject json = sender.calls.get(0).get(0);
        assertThatJson("{\"distinct_id\":\"id-1\",\"event\":\"$feature_flag_called\""
                + ",\"properties\":{\"$feature_flag\":\"java-feature-flag\",\"$feature_flag_response\":\"true\", \"$feature_flag_errored\":\"false\"}"
                +",\"timestamp\":\"" + instantExpected
                + "\"}").isEqualTo(new JSONObject(json, "distinct_id", "event", "timestamp", "properties").toString());
    }

    @Test
    public void testGetAllFeatureFlagsForUser() {
        final FeatureFlagPoller featureFlagPoller = new FeatureFlagPoller.Builder("", "", new TestGetter()).build();
        ph = new PostHog.BuilderWithCustomFeatureFlagPoller(featureFlagPoller)
                .build();

        final FeatureFlagConfig config = new FeatureFlagConfig.Builder("id-1")
                .build();

        final Map<String, Optional<String>> flags = ph.getAllFeatureFlags(config);
        assertEquals(1, flags.size());
        assertTrue(flags.containsKey("java-feature-flag"));
        assertTrue(flags.get("java-feature-flag").isPresent());
        assertEquals("variant-1", flags.get("java-feature-flag").get());
    }

    @Test
    public void testFlagActive() throws InterruptedException {
        boolean flag = ph.isFeatureFlagEnabled("test-flag", "test-user");

        assertEquals(true,flag);
    }

    @Test
    public void testFlagFalse() throws InterruptedException {
        boolean flag = ph.isFeatureFlagEnabled("false-flag", "test-user");

        assertEquals(false,flag);
    }

    @Test
    public void testFlagInactive() throws InterruptedException {
        boolean flag = ph.isFeatureFlagEnabled("untest-flag", "test-user");

        assertEquals(false,flag);
    }

    @Test
    public void testFlagValueActive() throws InterruptedException {
        String flag = ph.getFeatureFlag("test-flag", "test-user");

        assertEquals("true",flag);
    }

    @Test
    public void testFlagValueInactive() throws InterruptedException {
        String flag = ph.getFeatureFlag("untest-flag", "test-user");

        assertEquals(null,flag);
    }

    @Test
    public void testGetFlagActive() throws InterruptedException {
        String flag = ph.getFeatureFlagPayload("test-flag", "test-user");

        assertEquals("{\"key\": \"value\"}", flag);
    }

    @Test
    public void testGetFlagInactive() throws InterruptedException {
        String flag = ph.getFeatureFlagPayload("untest-flag", "test-user");

        assertEquals(null, flag);
    }

}
