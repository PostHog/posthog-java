package com.posthog.java;

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

    private Sender sending;
    private QueuePtr queue = new QueuePtr();
    private volatile boolean stop = false;
    // TODO: these should be customizable & set good defaults
    private int maxQueueSize = 10; // if more than this many items in queue trigger a send
    private int maxTimeToSendMs = 1000; // if more than this time since last send trigger a send
    private int sleepMs = 100;
    private Instant sendAfter;

    public QueueManager(Sender sending) {
        this.sending = sending;
    }

    public void stop() {
        stop = true; // TODO: should interrupt sleep?
    }

    public synchronized void add(JSONObject eventJson) {
        queue.add(eventJson);
    }

    private void sendAll() {
        List<JSONObject> toSend = queue.retrieveAndReset();
        updateSendAfter(); // after queue reset but before sending as the latter could take a long time
        sending.send(toSend);
    }

    private void updateSendAfter() {
        sendAfter = Instant.now().plusMillis(maxTimeToSendMs);
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
            }
            sendAll();
        }
    }
}
