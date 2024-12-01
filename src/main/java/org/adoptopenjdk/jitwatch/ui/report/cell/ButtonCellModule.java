package org.adoptopenjdk.jitwatch.ui.report.cell;

import org.adoptopenjdk.jitwatch.report.Report;

import javax.swing.*;
import java.awt.*;

public class ButtonCellModule
{
    private final JPanel panel;
    private final JButton button;

    public ButtonCellModule()
    {
        panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel.setOpaque(true);
        button = new JButton();
        button.setOpaque(true);
        panel.add(button);
    }

    public void setReport(Report report)
    {
        int bci = report.getBytecodeOffset();
        button.setText("View BCI " + bci);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Click to view BCI " + bci);
    }

    public JPanel getPanel()
    {
        return panel;
    }

    public JButton getButton()
    {
        return button;
    }
}
