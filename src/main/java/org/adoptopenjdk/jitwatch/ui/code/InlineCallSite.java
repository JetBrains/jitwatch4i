package org.adoptopenjdk.jitwatch.ui.code;

import org.adoptopenjdk.jitwatch.model.IMetaMember;

public class InlineCallSite
{
    private final IMetaMember member;
    private final int bci;

    public InlineCallSite(IMetaMember member, int bci)
    {
        this.member = member;
        this.bci = bci;
    }

    public IMetaMember getMember()
    {
        return member;
    }

    public int getBci()
    {
        return bci;
    }
}
