/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.main;

import org.adoptopenjdk.jitwatch.core.JITWatchConfig;
import org.adoptopenjdk.jitwatch.model.IMetaMember;

public interface IStageAccessProxy
{
//    void openTriView(IMetaMember member);

//    void openTriView(IMetaMember member, int highlightBCI);

//    void openBrowser(String title, String html, String stylesheet);

//    void openTextViewer(String title, String contents, boolean lineNumbers, boolean highlighting);

//    void openCompileChain(IMetaMember member);

    void openInlinedIntoReport(IMetaMember member);

    JITWatchConfig getConfig();
}
