package com.posthog.java;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import org.json.JSONObject;
import org.json.JSONException;
import java.lang.Exception;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;

public class PostHog {
    private String apiKey;
    private String host;
    private Worker worker;
    private OkHttpClient client;
    private Thread thread1;

    private class Sending {
        String apiKey;
        String host;

        public Sending(String apiKey, String host) {
            this.apiKey = apiKey;
            this.host = host;
        }

        private void send(List<JSONObject> events) {
            if (events == null || events.isEmpty()) {
                return;
            }
            String json = getRequestBody(events);
            System.out.println(json);

            try {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder().url(host + "/batch").post(body).build();
                System.out.println(request);
                Call call = client.newCall(request);
                Response response = call.execute();
                System.out.println(response.body().string());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }
        }

        private String getRequestBody(List<JSONObject> events) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("api_key", apiKey);
                jsonObject.put("batch", events);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject.toString();
        }

    }

    private Sending sending;

    public static class Builder {
        // required
        private final String apiKey;

        // optional
        // private String host = "https://app.posthog.com";
        private String host = "localhost";

        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder host(String value) {
            host = value;
            return this;
        }

        public PostHog build() {
            return new PostHog(this);
        }
    }

    private PostHog(Builder builder) {
        apiKey = builder.apiKey;
        host = builder.host;
        client = new OkHttpClient();
        sending = new Sending(apiKey, host);
        startWorker();
    }

    private void startWorker() {
        worker = new Worker(sending);
        thread1 = new Thread(worker, "Thread 1");
        thread1.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                worker.sendAll(); // the runner could be processing a large queue atm and get killed while not
                                  // having sent it as this run would then be fast
            }
        });
    }

    private void enqueue(String distinctId, String event, HashMap<String, Object> properties) {
        JSONObject eventJson = getEventJson(event, distinctId, properties);
        worker.add(eventJson);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     * @param properties an array with any event properties you'd like to set.
     */
    public void capture(String distinctId, String event, HashMap<String, Object> properties) {
        enqueue(distinctId, event, properties);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     */
    public void capture(String distinctId, String event) {
        enqueue(distinctId, event, null);
    }

    /**
     * 
     * @param distinctId        which uniquely identifies your user in your
     *                          database. Must not be null or empty.
     * @param properties        an array with any person properties you'd like to
     *                          set.
     * @param propertiesSetOnce an array with any person properties you'd like to
     *                          set without overwriting previous values.
     */
    public void identify(String distinctId, HashMap<String, Object> properties,
            HashMap<String, Object> propertiesSetOnce) {
        HashMap<String, Object> props = new HashMap<String, Object>();
        if (properties != null) {
            props.put("$set", properties);
        }
        if (propertiesSetOnce != null) {
            props.put("$set_once", propertiesSetOnce);
        }
        enqueue(distinctId, "$identify", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void identify(String distinctId, HashMap<String, Object> properties) {
        identify(distinctId, properties, null);
    }

    /**
     * 
     * @param distinctId distinct ID to merge. Must not be null or empty. Note: If
     *                   there is a conflict, the properties of this person will
     *                   take precedence.
     * @param alias      distinct ID to merge. Must not be null or empty. Note: If
     *                   there is a conflict, the properties of this person will be
     *                   overriden.
     */
    public void alias(String distinctId, String alias) {
        HashMap<String, Object> props = new HashMap<String, Object>() {
            {
                put("distinct_id", distinctId);
                put("alias", alias);
            }
        };
        enqueue(distinctId, "$create_alias", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void set(String distinctId, HashMap<String, Object> properties) {
        HashMap<String, Object> props = new HashMap<String, Object>() {
            {
                put("$set", properties);
            }
        };
        enqueue(distinctId, "$set", props);
    }

    /**
     * 
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     *                   Previous values will not be overwritten.
     */
    public void setOnce(String distinctId, HashMap<String, Object> properties) {
        HashMap<String, Object> props = new HashMap<String, Object>() {
            {
                put("$set_once", properties);
            }
        };
        enqueue(distinctId, "$set_once", props);
    }

    private JSONObject getEventJson(String event, String distinctId, HashMap<String, Object> properties) {
        JSONObject eventJson = new JSONObject();
        try {
            eventJson.put("timestamp", Instant.now().toString());
            eventJson.put("distinct_id", distinctId);
            eventJson.put("event", event);
            if (properties != null) {
                eventJson.put("properties", properties);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return eventJson;
    }

    private class Worker implements Runnable {
        private Sending sending;

        class QueueHead { // not sure if this is needed, wanted to make sure we can efficiently flush
                          // while not blocking adds to the queue
            public List<JSONObject> queue = new LinkedList<JSONObject>(); // TODO: JSONObject might not be the most
                                                                          // efficient
            // storage ;
            // List might not be the most performant for what I'm doing

            public QueueHead() {
            };
        }

        public Worker(Sending sending) {
            this.sending = sending;
        }

        private QueueHead head = new QueueHead();

        public synchronized void add(JSONObject eventJson) {
            head.queue.add(eventJson);
        }

        private void sendAll() {
            List<JSONObject> toSend = null;
            synchronized (this) {
                toSend = head.queue;
                head.queue = new LinkedList<JSONObject>();
            }
            sending.send(toSend); // there's something wrong with calling send from here ; but I don't need that I
                                  // can just move the sending related info down
        }

        @Override
        public void run() {
            // currently only time based batching, should also have queue size based?
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                if (head.queue.isEmpty()) {
                    continue;
                }
                sendAll();
            }
        }
    }
}
