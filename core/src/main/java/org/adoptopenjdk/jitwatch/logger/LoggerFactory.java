package org.adoptopenjdk.jitwatch.logger;

import java.nio.file.Path;

public class LoggerFactory
{
    private static ILoggerBackend loggerBackend = new StdOutLoggerBackend();

    public static Logger getLogger(Class<?> clazz)
    {
        return new Logger(clazz);
    }

    public static void setLogFile(Path path) {
        // relax for now
    }

    public static ILoggerBackend getLoggerBackend()
    {
        return loggerBackend;
    }

    public static void setLoggerBackend(ILoggerBackend loggerBackend)
    {
        LoggerFactory.loggerBackend = loggerBackend;
    }

    protected static void writeLog(Class clazz, Logger.Level level, String message, Throwable throwable, Object... args)
    {
        loggerBackend.writeLog(clazz, level, message, throwable, args);
    }
}
