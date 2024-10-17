/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.sandbox;

import java.io.File;

import com.intellij.openapi.project.Project;
import javafx.stage.Stage;

import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.parser.ILogParseErrorListener;
import org.adoptopenjdk.jitwatch.ui.base.JFXStage;

public interface ISandboxStage extends ILogParseErrorListener
{
	void openTriView(IMetaMember member);

	void showOutput(String output);

	void showError(String error);

	void runFile(EditorPane editor);

	void addSourceFolder(File dir);

	JFXStage getStageForChooser();

	void log(String msg);

	void setModified(EditorPane pane, boolean isModified);

	Project getProject();
}