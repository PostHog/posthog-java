package com.posthog.java;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) {
        // TODO: not sure why it didn't work from the test file, this way I was able to
        // run the debugger just fine
        String apiKey = "phc_VvgWABT4LDIPbWpNxVKuT92aCAghr0FEFlb2tHnPXBY";
        String host = "no-such-host";
        host = "http://159.65.213.147";
        PostHog ph = new PostHog.Builder(apiKey).host(host).build();

        String user = "user:32";
        ph.capture(user, "movie played", new HashMap<String, Object>() {
            {
                put("movieId", 123);
                put("category", "romcom");
            }
        });
        ph.identify(user, new HashMap<String, Object>() {
            {
                put("email", "test@t.com");
                put("name", "Mary");
                put("account", 12);
            }
        }, new HashMap<String, Object>() {
            {
                put("first_time", "done");
            }
        });
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String user2 = "user:33";
        ph.set(user2, new HashMap<String, Object>() {
            {
                put("account", 12377);
                put("category", "customer");
            }
        });

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ph.setOnce(user2, new HashMap<String, Object>() {
            {
                put("category", "shouldn't be overriden");
                put("new-key", "set-once");
            }
        });

        ph.alias(user, user2);
        ph.shutdown();
    }
}
