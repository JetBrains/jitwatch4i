package org.adoptopenjdk.jitwatch.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StdOutLoggerBackend implements ILoggerBackend
{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    @Override
    public void writeLog(Class clazz, Logger.Level level, String message, Throwable throwable, Object... args)
    {
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
        stringBuffer.append(level);
        stringBuffer.append(" (");
        stringBuffer.append(clazz.getName());
        stringBuffer.append(") - ");
        stringBuffer.append(messageWithArgs);

        if (throwable != null)
        {
            stringBuffer.append("\n");
            stringBuffer.append(formatErrorTrace(throwable));
        }

        printMessage(stringBuffer.toString());
    }

    private String formatErrorTrace(Throwable throwable)
    {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    private void printMessage(String message)
    {
        String log = "JITWatch: " + sdf.format(new Date()) + " " + message;
        System.out.println(log);
    }
}
