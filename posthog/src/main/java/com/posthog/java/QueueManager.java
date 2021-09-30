package com.posthog.java;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

public class QueueManager implements Runnable {
    class QueuePtr {
        // TODO: not sure if there's a datastructure in Java that gives me these
        // operations efficiently, specifically retrieveAndReset
        // Not sure if LinkedList and JSONObject are good choices here
        public List<JSONObject> ptr = new LinkedList<JSONObject>();

        public QueuePtr() {
        };

        public synchronized int size() {
            return ptr.size();
        }

        public synchronized boolean isEmpty() {
            return ptr.isEmpty();
        }

        public synchronized void add(JSONObject json) {
            ptr.add(json);
        }

        public synchronized List<JSONObject> retrieveAndReset() {
            if (isEmpty()) {
                return Collections.emptyList();
            }
            List<JSONObject> cur = ptr;
            ptr = new LinkedList<JSONObject>();
            return cur;
        }
    }

    private QueuePtr queue = new QueuePtr();
    private volatile boolean stop = false;
    private Instant sendAfter;
    // builder inputs
    private Sender sender;
    private int maxQueueSize;
    private Duration maxTimeInQueue;
    private int sleepMs;

    public static class Builder {
        // required
        private final Sender sender;

        // optional
        private int maxQueueSize = 10; // if more than this many items in queue trigger a send
        private Duration maxTimeInQueue = Duration.ofSeconds(5); // if more than this time in queue send trigger a send
        private int sleepMs = 100; // how long do we sleep between checking the above conditions

        public Builder(Sender sender) {
            this.sender = sender;
        }

        public Builder maxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        public Builder maxTimeInQueue(Duration duration) {
            this.maxTimeInQueue = duration;
            return this;
        }

        public Builder sleepMs(int sleepMs) {
            this.sleepMs = sleepMs;
            return this;
        }

        public QueueManager build() {
            return new QueueManager(this);
        }
    }

    private QueueManager(Builder builder) {
        this.sender = builder.sender;
        this.maxQueueSize = builder.maxQueueSize;
        this.maxTimeInQueue = builder.maxTimeInQueue;
        this.sleepMs = builder.sleepMs;
    }

    public void stop() {
        stop = true; // TODO: should interrupt sleep?
    }

    public void add(JSONObject eventJson) {
        queue.add(eventJson);
    }

    public int queueSize() {
        return queue.size();
    }

    public void sendAll() {
        List<JSONObject> toSend = queue.retrieveAndReset();
        updateSendAfter(); // after queue reset but before sending as the latter could take a long time
        if (!toSend.isEmpty()) {
            sender.send(toSend);
        }
    }

    private void updateSendAfter() {
        sendAfter = Instant.now().plus(maxTimeInQueue);
    }

    private void sleep() {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
            stop = true; // TODO: should we stop? should trigger an interrupt up?
        }
    }

    @Override
    public void run() {
        updateSendAfter();
        while (!stop) {
            if (queue.size() < maxQueueSize && Instant.now().isBefore(sendAfter)) {
                sleep();
            } else {
                sendAll();
            }
        }
        sendAll();
    }
}
