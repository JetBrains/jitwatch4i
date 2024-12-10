package org.adoptopenjdk.jitwatch.logger;

public class Logger
{
    public enum Level
    {
        ERROR,
        RELOAD,
        WARNING,
        INFO,
        DEBUG,
        TRACE
    }

    private final Class clazz;

    public Logger(Class clazz)
    {
        this.clazz = clazz;
    }

    public boolean isLevelEnabled(Level level)
    {
        return level != Level.DEBUG && level != Level.TRACE;
    }

    public void log(Level level, String message, Throwable throwable, Object... args)
    {
        if (isLevelEnabled(level))
        {
            LoggerFactory.writeLog(clazz, level, message, throwable, args);
        }
    }

    public void log(Level level, String message, Object... args)
    {
        log(level, message, null, args);
    }

    public void error(String message, Object... args)
    {
        log(Level.ERROR, message, args);
    }

    public void error(String message, Throwable throwable, Object... args)
    {
        log(Level.ERROR, message, throwable, args);
    }

    public void warn(String message, Object... args)
    {
        log(Level.WARNING, message, args);
    }

    public void warn(String message, Throwable throwable, Object... args)
    {
        log(Level.WARNING, message, throwable, args);
    }

    public void info(String message, Object... args)
    {
        log(Level.INFO, message, args);
    }

    public void info(String message, Throwable throwable, Object... args)
    {
        log(Level.INFO, message, throwable, args);
    }

    public void debug(String message, Object... args)
    {
        log(Level.DEBUG, message, args);
    }

    public void debug(String message, Throwable throwable, Object... args)
    {
        log(Level.DEBUG, message, throwable, args);
    }

    public void trace(String message, Object... args)
    {
        log(Level.TRACE, message, args);
    }

    public void trace(String message, Throwable throwable, Object... args)
    {
        log(Level.TRACE, message, throwable, args);
    }
}
