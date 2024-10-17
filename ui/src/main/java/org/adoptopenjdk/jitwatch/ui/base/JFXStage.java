package org.adoptopenjdk.jitwatch.ui.base;

import javafx.scene.Scene;

public interface JFXStage {

    enum StageType {
        TAB,
        MODAL_DIALOG;
    }
    StageType getStageType();
    Scene getScene();
    String getTitle();
    void show();
    int getWidth();
    int getHeight();
    int getX();
    int getY();
    void setX(int childX);
    void setY(int childY);
    void requestFocus();
    void toFront();
    void close();

}
