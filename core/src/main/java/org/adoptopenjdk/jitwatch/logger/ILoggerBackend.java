package org.adoptopenjdk.jitwatch.logger;

public interface ILoggerBackend
{
    void writeLog(Class<?> clazz, Logger.Level level, String message, Throwable throwable, Object... args);
}
