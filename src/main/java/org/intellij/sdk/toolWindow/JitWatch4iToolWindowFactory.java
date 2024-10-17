package org.intellij.sdk.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import javafx.application.Platform;
import org.adoptopenjdk.jitwatch.ui.base.JFXPanelStage;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;
import org.jetbrains.annotations.NotNull;

final class JitWatch4iToolWindowFactory implements ToolWindowFactory, DumbAware {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    Platform.setImplicitExit(false);

    final JFXPanelStage fxPanelStage = new JFXPanelStage();

    ContentManager contentManager = toolWindow.getContentManager();

    JITWatchUI jitWatchUI = new JITWatchUI(project, contentManager);

    Platform.runLater(() -> {
      try {
        jitWatchUI.start(fxPanelStage);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });


    Content content1 = contentManager.getFactory().createContent(fxPanelStage, "Main", false);
    contentManager.addContent(content1);
  }
}
