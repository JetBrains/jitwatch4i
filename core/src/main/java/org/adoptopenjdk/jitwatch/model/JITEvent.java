/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.model;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.C_COLON;
import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.C_SPACE;

import org.adoptopenjdk.jitwatch.util.StringUtil;

public class JITEvent
{
    private long stamp;
    private EventType eventType;
    private IMetaMember eventMember;
    private int level;

    public JITEvent(long stamp, EventType eventType, IMetaMember eventMember, int level)
    {
        this.stamp = stamp;
        this.eventType = eventType;
        this.eventMember = eventMember;
        this.level = level;
    }

    public long getStamp()
    {
        return stamp;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public IMetaMember getEventMember()
    {
        return eventMember;
    }

    public int getLevel()
    {
        return level;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(StringUtil.formatTimestamp(stamp, true)).append(C_SPACE);

        sb.append(StringUtil.alignRight(eventType.getText(), 14)).append(C_SPACE).append(C_COLON).append(C_SPACE);
     
        sb.append(eventMember.toString()).append(C_SPACE);
        sb.append(level);

        return sb.toString();
    }
}
