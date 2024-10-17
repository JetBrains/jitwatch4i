package org.adoptopenjdk.jitwatch.ui.code;

import org.adoptopenjdk.jitwatch.model.IMetaMember;

import java.util.ArrayList;
import java.util.List;

public class PsiMetaMemberWrapper
{
    private final IMetaMember member;
    private List<String> paramTypeNames;
    private String returnTypeName;

    public PsiMetaMemberWrapper(IMetaMember member)
    {
        this.member = member;
    }

    public String getMemberName()
    {
        return member.getMemberName();
    }

    public List<String> getParamTypeNames()
    {
        if (paramTypeNames == null)
        {
            paramTypeNames = new ArrayList<>();
            String[] origParamTypeNames = member.getParamTypeNames();

            if (origParamTypeNames != null)
            {
                for (String paramTypeName : origParamTypeNames)
                {
                    paramTypeNames.add(paramTypeName.replaceAll("\\$", "."));
                }
            }
        }
        return paramTypeNames;
    }

    public String getReturnTypeName()
    {
        if (returnTypeName == null)
        {
            returnTypeName = member.getReturnTypeName().replaceAll("\\$", ".");
        }
        return returnTypeName;
    }
}
