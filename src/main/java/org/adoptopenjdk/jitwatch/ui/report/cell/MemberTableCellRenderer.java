/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.report.cell;

import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.report.Report;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MemberTableCellRenderer extends JPanel implements TableCellRenderer
{
    private JLabel lblMetaClass;
    private JLabel lblMetaMember;
    private JLabel lblCompilation;
    private JLabel lblViewBCI;

    public MemberTableCellRenderer()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        lblMetaClass = new JLabel();
        lblMetaMember = new JLabel();
        lblCompilation = new JLabel();
        lblViewBCI = new JLabel();

        add(lblMetaClass);
        add(lblMetaMember);
        add(lblCompilation);
        add(lblViewBCI);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
    {

        if (value instanceof Report)
        {
            Report report = (Report) value;
            IMetaMember member = report.getCaller();

            if (member != null)
            {
                lblMetaClass.setText(member.getMetaClass().getFullyQualifiedName());
                lblMetaMember.setText(member.toStringUnqualifiedMethodName(false, false));

                Compilation compilation = member.getCompilation(report.getCompilationIndex());
                String compilationText = compilation != null ? "Compilation: " + compilation.getSignature() : "";
                lblCompilation.setText(compilationText);

                lblViewBCI.setText("View BCI " + report.getBytecodeOffset());
                lblViewBCI.setForeground(Color.BLUE.darker());
                lblViewBCI.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lblViewBCI.setToolTipText("Click to view BCI");

                lblViewBCI.setVisible(true);
            }
            else
            {
                lblMetaClass.setText("");
                lblMetaMember.setText("");
                lblCompilation.setText("");
                lblViewBCI.setVisible(false);
            }
        }
        else
        {
            lblMetaClass.setText("");
            lblMetaMember.setText("");
            lblCompilation.setText("");
            lblViewBCI.setVisible(false);
        }

        if (isSelected)
        {
            setBackground(table.getSelectionBackground());
            lblMetaClass.setForeground(table.getSelectionForeground());
            lblMetaMember.setForeground(table.getSelectionForeground());
            lblCompilation.setForeground(table.getSelectionForeground());
            lblViewBCI.setForeground(table.getSelectionForeground());
        }
        else
        {
            setBackground(table.getBackground());
            lblMetaClass.setForeground(table.getForeground());
            lblMetaMember.setForeground(table.getForeground());
            lblCompilation.setForeground(table.getForeground());
            lblViewBCI.setForeground(Color.BLUE.darker());
        }
        return this;
    }
}
