package org.adoptopenjdk.jitwatch.util;

import org.adoptopenjdk.jitwatch.logger.ILoggerBackend;
import org.adoptopenjdk.jitwatch.logger.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class IntelliJLoggerBackend implements ILoggerBackend
{
    private static final com.intellij.openapi.diagnostic.Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance("JitWatch4i");

    @Override
    public void writeLog(Class<?> clazz, Logger.Level level, String message, Throwable throwable, Object... args)
    {
        switch (level)
        {
            case DEBUG:
                if (!LOG.isDebugEnabled())
                {
                    return;
                }
                break;
            case TRACE:
                if (!LOG.isTraceEnabled())
                {
                    return;
                }
                break;
        }

        String messageWithArgs = message;
        for (Object arg : args)
        {
            int index = messageWithArgs.indexOf("{}");
            if (index >= 0)
            {
                messageWithArgs = messageWithArgs.substring(0, index) + arg + messageWithArgs.substring(index + 2);
            }
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("(");
        stringBuffer.append(clazz.getName());
        stringBuffer.append(") - ");
        stringBuffer.append(messageWithArgs);

        if (throwable != null)
        {
            stringBuffer.append("\n");
            stringBuffer.append(formatErrorTrace(throwable));
        }

        String msg = stringBuffer.toString();

        if (throwable != null)
        {
            switch (level)
            {
                case ERROR:
                    LOG.error(msg, throwable);
                    break;
                case RELOAD:
                case WARNING:
                    LOG.warn(msg, throwable);
                    break;
                case INFO:
                    LOG.info(msg, throwable);
                    break;
                case DEBUG:
                    LOG.debug(msg, throwable);
                    break;
                case TRACE:
                    LOG.trace(msg);
                    break;
            }
        }
        else
        {
            switch (level)
            {
                case ERROR:
                    LOG.error(msg);
                    break;
                case RELOAD:
                case WARNING:
                    LOG.warn(msg);
                    break;
                case INFO:
                    LOG.info(msg);
                    break;
                case DEBUG:
                    LOG.debug(msg);
                    break;
                case TRACE:
                    LOG.trace(msg);
                    break;
            }
        }
    }

    private String formatErrorTrace(Throwable throwable)
    {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }
}
