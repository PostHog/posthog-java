package com.posthog.java_example;

import java.util.HashMap;
import java.lang.Thread;

import com.posthog.java.PostHog;

// Testing against local PostHog snapshot jar:
// 1. `mvn install` in posthog folder to put the jar in local repo (~/.m2/repository/com/posthog/java/posthog)
// 2. Set the envs (e.g. `export POSTHOG_API_KEY="<key_value>"`)
// 3. `mvn compile exec:java` in example folder
public final class App {
    private static final String POSTHOG_API_KEY = System.getenv("POSTHOG_API_KEY");
    private static final String POSTHOG_HOST = System.getenv("POSTHOG_HOST");

    private App() {
    }

    public static void main(String[] args) {
        PostHog posthog = new PostHog.Builder(POSTHOG_API_KEY).host(POSTHOG_HOST).build();

        posthog.capture("distinct id 1", "movie played", new HashMap<String, Object>() {
            {
                put("movie_id", 123);
                put("category", "romcom");
            }
        });

        posthog.identify("distinct id 1", new HashMap<String, Object>() {
            {
                put("email", "john@doe.com");
                put("proUser", false);
            }
        });

        posthog.alias("anonymous session id", "distinct id 1");

        posthog.capture("distinct id 2", "movie played", new HashMap<String, Object>() {
            {
                put("eventProperty", "value1"); // event properties
                put("$set", new HashMap<String, Object>() { // user properties
                    {
                        put("email", "john@doe.com");
                        put("proUser", false);
                    }
                });
                put("$set_once", new HashMap<String, Object>() { // user properties
                    {
                        put("user_first_location", "colorado");
                    }
                });
            }
        });

        posthog.identify("distinct id 3", new HashMap<String, Object>() { // set
            {
                put("email", "john@doe.com");
                put("proUser", false);
            }
        }, new HashMap<String, Object>() { // set_once
            {
                put("user_first_location", "colorado");
            }
        });

        posthog.set("distinct id 4", new HashMap<String, Object>() {
            {
                put("email", "john@doe.com");
                put("proUser", false);
            }
        });

        posthog.setOnce("distinct id 5", new HashMap<String, Object>() {
            {
                put("user_first_location", "colorado");
            }
        });

        posthog.capture("id 3", "event 2");
        posthog.capture("id 3", "event 3");
        posthog.capture("id 3", "event 4");

        posthog.shutdown(); // send the last events in queue
    }
}
