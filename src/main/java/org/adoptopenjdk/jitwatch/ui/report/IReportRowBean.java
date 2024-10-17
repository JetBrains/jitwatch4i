/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.report;

import org.adoptopenjdk.jitwatch.report.Report;

public interface IReportRowBean
{
    Report getReport();

    String getText();

    int getBytecodeOffset();
}
