/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.parserchooser;

import org.adoptopenjdk.jitwatch.parser.ParserType;

public interface IParserSelectedListener
{
	void parserSelected(ParserType parserType);
}