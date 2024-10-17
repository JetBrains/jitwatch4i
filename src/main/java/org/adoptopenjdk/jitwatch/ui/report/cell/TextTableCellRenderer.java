/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.report.cell;

import javax.swing.table.DefaultTableCellRenderer;

public class TextTableCellRenderer extends DefaultTableCellRenderer
{
    @Override
    public void setValue(Object value)
    {
        setText(value != null ? value.toString() : "");
    }
}
