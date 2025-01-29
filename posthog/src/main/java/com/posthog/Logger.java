public interface PostHogLogger {
    void debug(String message);
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable throwable);
}

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
