package org.adoptopenjdk.jitwatch.ui.base;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static java.util.Optional.ofNullable;

public class JFXDialog extends DialogWrapper implements JFXStage {

    private JFXPanel jfxPanel;
    private Scene scene;
    private StageStyle stageStyle;

    private ReadOnlyDoubleWrapper width =
            new ReadOnlyDoubleWrapper(this, "width", Double.NaN);
    private ReadOnlyDoubleWrapper height =
            new ReadOnlyDoubleWrapper(this, "height", Double.NaN);
    private Integer withOverride;
    private Integer heightOverride;
    private Integer XOverride;
    private Integer YOverride;

    public JFXDialog(@Nullable Project project) {
        super(project, true);
        init();
    }

    @Override
    public Scene getScene() {
        return scene;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        // Create a Swing panel to hold the JFXPanel
        JPanel panel = new JPanel(new BorderLayout());
        jfxPanel = new JFXPanel(); // This is the bridge between Swing and JavaFX
        panel.add(jfxPanel, BorderLayout.CENTER);

        // Initialize JavaFX components on the JavaFX Application Thread
        Platform.runLater(() -> {
            jfxPanel.setScene(scene); // Set the scene on the JFXPanel
        });

        return panel;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    @Override
    public StageType getStageType() {
        return StageType.MODAL_DIALOG;
    }

    @Override
    protected boolean postponeValidation() {
        return super.postponeValidation();
    }

    protected void initStyle(StageStyle stageStyle) {
        this.stageStyle = stageStyle;
    }


    protected void initOwner(JFXStage owner) {
        // TODO: no action
    }

    protected void initModality(Modality modality) {
    }

    protected void centerOnScreen() {
        // TODO: no action
    }

    protected void sizeToScene() {
        // TODO: no action
    }

    protected void showAndWait() {
        show();
    }

    @Override
    public int getWidth() {
        return ofNullable(withOverride).orElse(jfxPanel.getWidth());
    }

    public void setWidth(int width) {
        withOverride = width;
    }

    @Override
    public int getHeight() {
        return ofNullable(heightOverride).orElse(jfxPanel.getHeight());
    }

    public void setHeight(int height) {
        heightOverride = height;
    }

    public final ReadOnlyDoubleProperty widthProperty() {
        width.set(getWidth());
        return width.getReadOnlyProperty();
    }

    public final ReadOnlyDoubleProperty heightProperty() {
        height.set(getHeight());
        return height.getReadOnlyProperty();
    }

    @Override
    public int getX() {
        return ofNullable(XOverride).orElse(0);
    }

    @Override
    public int getY() {
        return ofNullable(YOverride).orElse(0);
    }

    @Override
    public void setX(int childX) {
        XOverride = childX;
    }

    @Override
    public void setY(int childY) {
        YOverride = childY;
    }

    @Override
    public void requestFocus() {
    }

    @Override
    public void close() {
        super.close(OK_EXIT_CODE);
    }
}