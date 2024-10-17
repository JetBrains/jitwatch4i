/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import javafx.application.Platform;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.base.JFXPanelStage;
import org.adoptopenjdk.jitwatch.ui.base.JFXStage;
import org.adoptopenjdk.jitwatch.ui.main.ICompilationChangeListener;
import org.adoptopenjdk.jitwatch.ui.report.ReportStage;
import org.adoptopenjdk.jitwatch.util.UserInterfaceUtil;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.swing.*;

public class StageManager
{
	private static List<JFXStage> openStages = new ArrayList<>();

	private static List<IStageClosedListener> listenerStageClosed = new ArrayList<>();
	private static List<ICompilationChangeListener> listenerCompilationChanged = new ArrayList<>();

	private StageManager()
	{
	}

	public static void registerStageClosedListener(IStageClosedListener listener)
	{
		listenerStageClosed.add(listener);
	}

	public static void registerCompilationChangeListener(ICompilationChangeListener listener)
	{
		listenerCompilationChanged.add(listener);
	}

	public static void notifyCompilationChanged(IMetaMember member)
	{
		for (ICompilationChangeListener listener : listenerCompilationChanged)
		{
			listener.compilationChanged(member);
		}
	}

	private static void notifyStageClosedListeners(JFXStage stage)
	{
		for (IStageClosedListener listener : listenerStageClosed)
		{
			listener.handleStageClosed(stage);

			if (stage instanceof ICompilationChangeListener)
			{
				listenerCompilationChanged.remove((ICompilationChangeListener)stage);
			}
		}
	}

	public static void clearReportStages()
	{
		for (JFXStage stage : openStages)
		{
			if (stage instanceof ReportStage)
			{
				((ReportStage) stage).clear();
			}
		}
	}

	// Adds a close buttong to stages
	// for fullscreen JavaFX systems with no window decorations
	private static void addCloseButton(final JFXStage stage)
	{
		Scene scene = stage.getScene();

		Parent rootNode = scene.getRoot();

		Button btnClose = new Button("X");

		btnClose.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				closeStage(stage);
			}
		});

		HBox hbox = new HBox();
		hbox.setSpacing(16);
		hbox.getChildren().addAll(btnClose, new Label(stage.getTitle()));

		if (rootNode instanceof BorderPane)
		{
			BorderPane pane = (BorderPane) rootNode;

			Node topNode = pane.getTop();

			if (topNode instanceof VBox)
			{
				VBox vbox = (VBox) topNode;
				vbox.getChildren().add(0, hbox);
			}
			else
			{
				VBox newTopNode = new VBox();
				newTopNode.setPadding(new Insets(0));

				newTopNode.getChildren().addAll(hbox, topNode);
				pane.setTop(newTopNode);
			}
		}
		else
		{
			VBox newTopNode = new VBox();
			newTopNode.setPadding(new Insets(0));

			newTopNode.getChildren().addAll(hbox, rootNode);

			scene.setRoot(newTopNode);
		}
	}

	public static void addAndShow(final ContentManager contentManager, final JFXStage childStage)
	{
		addAndShow(contentManager, childStage, null);
	}

	public static void addAndShow(ContentManager contentManager, final JFXStage childStage, final IStageClosedListener closedListener)
	{
		openStages.add(childStage);

		if (childStage instanceof ICompilationChangeListener)
		{
			registerCompilationChangeListener((ICompilationChangeListener)childStage);
		}
		
		if (UserInterfaceUtil.ADD_CLOSE_DECORATION)
		{
			addCloseButton(childStage);
		}

		// TODO: idea no tab close?
		/*
		childStage.setOnCloseRequest(new EventHandler<WindowEvent>()
		{
			@Override
			public void handle(WindowEvent arg0)
			{
				if (closedListener != null)
				{
					closedListener.handleStageClosed(childStage);
				}

				closeStage(childStage);
			}
		});
		 */

		childStage.show();

		double parentX = 0;
		double parentY = 0;
		double parentWidth = 100;
		double parentHeight = 100;

		double childWidth = childStage.getWidth();
		double childHeight = childStage.getHeight();

		double childX = parentX + (parentWidth - childWidth) / 2;
		double childY = parentY + (parentHeight - childHeight) / 2;

		childStage.setX((int) childX);
		childStage.setY((int) childY);

		if (childStage instanceof JFXPanelStage)
		{
			JFXPanelStage panelStage = (JFXPanelStage) childStage;
			SwingUtilities.invokeLater(() -> {
				Content content = contentManager.getFactory().createContent(panelStage, panelStage.getTitle(), false);
				contentManager.addContent(content);
			});
		}

		childStage.requestFocus();

		childStage.toFront();
	}

	public static EventHandler<ActionEvent> getCloseHandler(final JFXStage stage)
	{
		return new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				StageManager.closeStage(stage);
			}
		};
	}

	public static void closeStage(JFXStage stage)
	{
		notifyStageClosedListeners(stage);
		openStages.remove(stage);

		stage.close();
	}
}