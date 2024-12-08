/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.model;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.DEBUG_MEMBER_CREATION;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

import org.adoptopenjdk.jitwatch.core.JITWatchConstants;
import org.adoptopenjdk.jitwatch.util.StringUtil;

public class MetaConstructor extends AbstractMetaMember
{
	private String constructorToString;

	public MetaConstructor(Constructor<?> constructor, MetaClass methodClass)
	{
		super(StringUtil.getUnqualifiedMemberName(constructor.getName()));
		
		this.constructorToString = constructor.toString();
		this.metaClass = methodClass;

		returnTypeName = Void.TYPE.getName();
		
		paramTypesNames = new ArrayList<>();

		for (Class<?> paramType : constructor.getParameterTypes())
		{
			paramTypesNames.add(paramType.getName());
		}

		modifier = constructor.getModifiers();
		
        isVarArgs = constructor.isVarArgs();

        if (DEBUG_MEMBER_CREATION)
        {
        	logger.debug("Created MetaConstructor: {}", toString());
        }
	}

	public MetaConstructor(MemberSignatureParts msp, MetaClass metaClass)
	{
		super(StringUtil.getUnqualifiedMemberName(msp.getMemberName()));

		this.constructorToString = msp.toStringSingleLine();
		this.metaClass = metaClass;

		returnTypeName = Void.TYPE.getName();
		paramTypesNames = Arrays.asList();

		// Can include non-method modifiers such as volatile so AND with
		// acceptable values
		modifier = Modifier.PUBLIC;

		isVarArgs = false;

		if (DEBUG_MEMBER_CREATION)
		{
			logger.debug("Created MetaMethod: {}", toString());
		}
	}

	@Override
	public String toString()
	{
		String methodSigWithoutThrows = constructorToString;

		int closingParentheses = methodSigWithoutThrows.indexOf(JITWatchConstants.S_CLOSE_PARENTHESES);

		if (closingParentheses != methodSigWithoutThrows.length() - 1)
		{
			methodSigWithoutThrows = methodSigWithoutThrows.substring(0, closingParentheses + 1);
		}

		return methodSigWithoutThrows;
	}
}