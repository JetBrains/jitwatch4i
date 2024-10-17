package org.adoptopenjdk.jitwatch.ui.code;

import org.adoptopenjdk.jitwatch.model.IMetaMember;

import java.util.List;
import java.util.Set;

public class InlineFailureGroup
{
    private final IMetaMember callee;
    private final int calleeSize;
    private final Integer calleeInvocationCount;
    private final Set<String> reasons;
    private final List<InlineCallSite> callSites;

    public InlineFailureGroup(IMetaMember callee, int calleeSize, Integer calleeInvocationCount,
                              Set<String> reasons, List<InlineCallSite> callSites)
    {
        this.callee = callee;
        this.calleeSize = calleeSize;
        this.calleeInvocationCount = calleeInvocationCount;
        this.reasons = reasons;
        this.callSites = callSites;
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

    public Set<String> getReasons()
    {
        return reasons;
    }

    public List<InlineCallSite> getCallSites()
    {
        return callSites;
    }
}
