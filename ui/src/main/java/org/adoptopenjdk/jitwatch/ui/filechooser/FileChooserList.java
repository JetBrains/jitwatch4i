/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.filechooser;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.adoptopenjdk.jitwatch.logger.Logger;
import org.adoptopenjdk.jitwatch.logger.LoggerFactory;
import org.adoptopenjdk.jitwatch.ui.base.JFXStage;
import org.adoptopenjdk.jitwatch.util.UserInterfaceUtil;

import java.util.ArrayList;
import java.util.List;

public class FileChooserList extends VBox
{
	private static final Logger logger = LoggerFactory.getLogger(FileChooserList.class);

    private final Project project;
    private JFXStage stage;

	protected ListView<Label> fileList;

	private VirtualFile lastFolder = null;

	protected VBox vboxButtons;

	public FileChooserList(Project project, JFXStage stage, String title, List<String> items)
	{
        this.project = project;

        this.stage = stage;

		HBox hbox = new HBox();

		fileList = new ListView<>();

		setItems(items);

		Button btnOpenFileDialog = UserInterfaceUtil.createButton("CONFIG_ADD_FILE");
		btnOpenFileDialog.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				chooseFile();
			}
		});

		Button btnOpenFolderDialog = UserInterfaceUtil.createButton("CONFIG_ADD_FOLDER");
		btnOpenFolderDialog.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				chooseFolder();
			}
		});

		Button btnRemove = UserInterfaceUtil.createButton("CONFIG_REMOVE");
		btnRemove.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				Label selected = fileList.getSelectionModel().getSelectedItem();

				if (selected != null)
				{
					fileList.getItems().remove(selected);
				}
			}
		});

		vboxButtons = new VBox();
		vboxButtons.setPadding(new Insets(0,10,10,10));
		vboxButtons.setSpacing(10);

		vboxButtons.getChildren().add(btnOpenFileDialog);
		vboxButtons.getChildren().add(btnOpenFolderDialog);
		vboxButtons.getChildren().add(btnRemove);

		hbox.getChildren().add(fileList);
		hbox.getChildren().add(vboxButtons);

		fileList.prefWidthProperty().bind(this.widthProperty().multiply(0.8));
		vboxButtons.prefWidthProperty().bind(this.widthProperty().multiply(0.2));

		Label titleLabel = UserInterfaceUtil.createLabel(title);

		getChildren().add(titleLabel);
		getChildren().add(hbox);

		setSpacing(10);
	}

	public void setItems(List<String> items)
	{
		fileList.getItems().clear();

		for (String item : items)
		{
			fileList.getItems().add(new Label(item));
		}
	}

	private void chooseFile() {
		// Create a FileChooserDescriptor that allows multiple files to be selected
		FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, true);
		descriptor.setTitle("Choose File(s)");

		VirtualFile initialFile;

		if (lastFolder == null) {
			// Use the user's current working directory if lastFolder is not set
			String userDir = System.getProperty("user.dir");
			initialFile = LocalFileSystem.getInstance().findFileByPath(userDir);
		} else {
			// Use lastFolder as the initial directory
			initialFile = lastFolder;
		}

		// Show the file chooser dialog
		VirtualFile [] selectedFiles = FileChooser.chooseFiles(descriptor, project, initialFile);

		if (selectedFiles != null && selectedFiles.length > 0) {
			for (VirtualFile vf : selectedFiles) {
				try {
					String path = vf.getCanonicalPath();
					if (path != null) {
						fileList.getItems().add(new Label(path));
					}

					lastFolder = vf.getParent();
				} catch (Exception e) {
					logger.error("An error occurred while processing the selected files.", e);
				}
			}
		}
	}

	private void chooseFolder() {
		// Create a FileChooserDescriptor that allows selection of directories only
		FileChooserDescriptor descriptor = new FileChooserDescriptor(
				false,
				true,
				false,
				false,
				false,
				false
		);
		descriptor.setTitle("Choose Folder");

		VirtualFile initialFile;

		if (lastFolder == null) {
			String userDir = System.getProperty("user.dir");
			initialFile = LocalFileSystem.getInstance().findFileByPath(userDir);
		} else {
			initialFile = lastFolder;
		}

		VirtualFile[] selectedFiles = FileChooser.chooseFiles(descriptor, project, initialFile);

		if (selectedFiles != null && selectedFiles.length > 0) {
			VirtualFile result = selectedFiles[0]; // Since only one directory is selected

			try {
				String path = result.getCanonicalPath();
				if (path != null) {
					addPathToList(path);
				}
			} catch (Exception e) {
				logger.error("An error occurred while getting the canonical path.", e);
			}

			lastFolder = result;
		}
	}

	public List<String> getFiles()
	{
		List<String> result = new ArrayList<>();

		for (Label label : fileList.getItems())
		{
			result.add(label.getText());
		}

		return result;
	}

	protected void addPathToList(String path)
	{
		boolean found = false;

		for (Label label : fileList.getItems())
		{
			if (path.equals(label.getText()))
			{
				found = true;
				break;
			}
		}

		if (!found)
		{
			fileList.getItems().add(new Label(path));
		}
	}
}
