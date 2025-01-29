package com.posthog.java;

/**
 * Allows you to inject a logger to the PostHog library
 * We configure the DefaultPostHogLogger if one is not provided
 */
public interface PostHogLogger {
    void debug(String message);
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable throwable);
}