package org.adoptopenjdk.jitwatch.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public enum Level {
        ERROR,
        RELOAD,
        WARNING,
        INFO,
        DEBUG,
        TRACE
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private final Class clazz;

    public Logger(Class clazz) {
        this.clazz = clazz;
    }

    private String formatErrorTrace(Throwable throwable) {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    protected void printMessage(String message) {
        String log = "JITWATCH: " + sdf.format(new Date()) +  " " + message;
        System.out.println(log);
    }

    public void print(Class clazz, Level level, String message, Throwable throwable, Object... args) {

        String messageWithArgs = message;
        for (Object arg : args) {
            int index = messageWithArgs.indexOf("{}");
            if (index >= 0) {
                messageWithArgs = messageWithArgs.substring(0, index) + String.valueOf(arg) + messageWithArgs.substring(index + 2);
            }
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(level);
        stringBuffer.append(" (");
        stringBuffer.append(clazz.getName());
        stringBuffer.append(") - ");
        stringBuffer.append(messageWithArgs);

        if (throwable != null) {
            stringBuffer.append("\n");
            stringBuffer.append(formatErrorTrace(throwable));
        }

        printMessage(stringBuffer.toString());
    }

    public boolean isLevelEnabled(Level level) {
        return true;
    }

    public void log(Level level, String message, Throwable throwable, Object... args) {
        if (isLevelEnabled(level))
            print(clazz, level, message, throwable, args);
    }

    public void log(Level level, String message, Object... args) {
        log(level, message, null, args);
    }

    public void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    public void error(String message, Throwable throwable, Object... args) {
        log(Level.ERROR, message, throwable, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARNING, message, args);
    }

    public void warn(String message, Throwable throwable, Object... args) {
        log(Level.WARNING, message, throwable, args);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void info(String message, Throwable throwable, Object... args) {
        log(Level.INFO, message, throwable, args);
    }

    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    public void debug(String message, Throwable throwable, Object... args) {
        log(Level.DEBUG, message, throwable, args);
    }

    public void trace(String message, Object... args) {
        log(Level.TRACE, message, args);
    }

    public void trace(String message, Throwable throwable, Object... args) {
        log(Level.TRACE, message, throwable, args);
    }
}
