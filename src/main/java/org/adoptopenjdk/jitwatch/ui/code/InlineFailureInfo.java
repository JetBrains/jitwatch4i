package org.adoptopenjdk.jitwatch.ui.code;

import org.adoptopenjdk.jitwatch.model.IMetaMember;

public class InlineFailureInfo
{
    private final IMetaMember callSite;
    private final int bci;
    private final IMetaMember callee;
    private final int calleeSize;
    private final Integer calleeInvocationCount;
    private final String reason;

    public InlineFailureInfo(IMetaMember callSite, int bci, IMetaMember callee, int calleeSize,
                             Integer calleeInvocationCount, String reason)
    {
        this.callSite = callSite;
        this.bci = bci;
        this.callee = callee;
        this.calleeSize = calleeSize;
        this.calleeInvocationCount = calleeInvocationCount;
        this.reason = reason;
    }

    public IMetaMember getCallSite()
    {
        return callSite;
    }

    public int getBci()
    {
        return bci;
    }

    public IMetaMember getCallee()
    {
        return callee;
    }

    public int getCalleeSize()
    {
        return calleeSize;
    }

    public Integer getCalleeInvocationCount()
    {
        return calleeInvocationCount;
    }

    public String getReason()
    {
        return reason;
    }
}

