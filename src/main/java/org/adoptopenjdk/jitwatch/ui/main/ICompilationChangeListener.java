/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.main;

import org.adoptopenjdk.jitwatch.model.IMetaMember;

public interface ICompilationChangeListener
{
	void compilationChanged(IMetaMember member);
}