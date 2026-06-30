package org.afetch;

public class Logger {
    private static Logger instance;

    public enum Level {
        INFO, DEBUG, WARN, ERROR
    }

    private Logger() {}

    public static Logger getInstance() {
        if (instance == null) {
            return new Logger();
        }

        return instance;

    }

    public void log(Level level, String tag, String msg) {
        String prefix = switch (level) {
            case INFO  -> "[INFO]";
            case DEBUG -> "[DEBUG]";
            case WARN  -> "[WARN]";
            case ERROR -> "[ERROR]";
        };

        String fmt = String.format(
          "%s [%s] %s\n", prefix, tag, msg
        );
        if (level != Level.INFO) {
          System.err.printf(fmt);
        } else {
          System.out.printf(fmt);
        }
    }
}
