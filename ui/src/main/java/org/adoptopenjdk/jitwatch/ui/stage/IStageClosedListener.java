/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.stage;

import javafx.stage.Stage;
import org.adoptopenjdk.jitwatch.ui.base.JFXStage;

public interface IStageClosedListener
{
	void handleStageClosed(JFXStage stage);
}