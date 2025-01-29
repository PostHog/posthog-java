package com.posthog.java;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultPostHogLogger implements PostHogLogger {
    private final Logger logger;

    public DefaultPostHogLogger() {
        this.logger = Logger.getLogger(PostHog.class.getName());
    }

    @Override
    public void debug(String message) {
        logger.fine(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void error(String message) {
        logger.severe(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
