package org.adoptopenjdk.jitwatch.ui.report.cell;

import org.adoptopenjdk.jitwatch.report.Report;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ButtonCellRenderer implements TableCellRenderer
{
    private final ButtonCellModule buttonCellModule;

    public ButtonCellRenderer()
    {
        buttonCellModule = new ButtonCellModule();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (value instanceof Report)
        {
            buttonCellModule.setReport((Report) value);
        }

        return buttonCellModule.getPanel();
    }
}