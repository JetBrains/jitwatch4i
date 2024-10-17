package org.adoptopenjdk.jitwatch.ui.base;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.StageStyle;

import static java.util.Optional.ofNullable;

public class JFXPanelStage extends JFXPanel implements JFXStage {
    private StageStyle style;
    private String title;
    private ReadOnlyDoubleWrapper width =
            new ReadOnlyDoubleWrapper(this, "width", Double.NaN);
    private ReadOnlyDoubleWrapper height =
            new ReadOnlyDoubleWrapper(this, "height", Double.NaN);
    private Integer withOverride;
    private Integer heightOverride;


    @Override
    public String getTitle() {
        return title;
    }

    public void show() {
        // not implemented
    }

    public StageStyle getStyle() {
        return style;
    }

    public void setStyle(StageStyle style) {
        this.style = style;
    }

    public void initStyle(StageStyle style) {
        this.style = style;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void  toFront() {
       // TODO:
    }

    @Override
    public int getWidth() {
        return ofNullable(withOverride).orElse(super.getWidth());
    }

    public void setWidth(int width) {
        withOverride = width;
    }

    @Override
    public int getHeight() {
        return ofNullable(heightOverride).orElse(super.getHeight());
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
        return super.getX();
    }

    @Override
    public int getY() {
        return super.getY();
    }

    @Override
    public StageType getStageType() {
        return StageType.TAB;
    }

    public void close() {
        // TODO: idea
    }

    @Override
    public void setX(int childX) {
        // no implementation ?
    }

    @Override
    public void setY(int childY) {
        // no implementation ?
    }

    @Override
    public void requestFocus() {
        // no implementation ?
    }
}
