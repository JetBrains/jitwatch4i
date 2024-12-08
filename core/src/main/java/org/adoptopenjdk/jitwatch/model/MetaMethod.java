/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.model;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.DEBUG_MEMBER_CREATION;

public class MetaMethod extends AbstractMetaMember
{
    private String methodToString;
    private MemberSignatureParts msp;

    public MetaMethod(Method method, MetaClass methodClass)
    {
    	super(method.getName());
    	
        this.methodToString = method.toString();
        this.metaClass = methodClass;

        returnTypeName = method.getReturnType().getName();
        paramTypesNames = new ArrayList<>();
        for (Class<?> paramType : method.getParameterTypes())
        {
            paramTypesNames.add(paramType.getName());
        }

        // Can include non-method modifiers such as volatile so AND with
        // acceptable values
        modifier = method.getModifiers() & Modifier.methodModifiers();

        isVarArgs = method.isVarArgs();

        checkPolymorphicSignature(method);

        if (DEBUG_MEMBER_CREATION)
        {
        	logger.debug("Created MetaMethod: {}", toString());
        }
    }

    public MetaMethod(MemberSignatureParts msp, MetaClass metaClass)
    {
        super(msp.getMemberName());

        this.msp = msp;
        this.methodToString = msp.toStringSingleLine();
        this.metaClass = metaClass;

        returnTypeName = msp.getReturnType();
        paramTypesNames = msp.getParamTypes();

        // Can include non-method modifiers such as volatile so AND with
        // acceptable values
        modifier = Modifier.PUBLIC;

        isVarArgs = false;

        if (DEBUG_MEMBER_CREATION)
        {
            logger.debug("Created MetaMethod: {}", toString());
        }
    }

    public void setParamTypesNames(List<String> typesNames)
    {
    	this.paramTypesNames = typesNames;
    }
    
    public void setReturnTypeName(String returnTypeName)
    {
    	this.returnTypeName = returnTypeName;
    }

    @Override
    public String toString()
    {
        String methodSigWithoutThrows = methodToString;

        int closingParentheses = methodSigWithoutThrows.indexOf(')');

        if (closingParentheses != methodSigWithoutThrows.length() - 1)
        {
            methodSigWithoutThrows = methodSigWithoutThrows.substring(0, closingParentheses + 1);
        }

        return methodSigWithoutThrows;
    }

    @Override
    public boolean matchesSignature(MemberSignatureParts msp, boolean matchTypesExactly)
    {
        if (this.msp == null)
        {
            return super.matchesSignature(msp, matchTypesExactly);
        }
        return this.msp.equals(msp);
    }
}