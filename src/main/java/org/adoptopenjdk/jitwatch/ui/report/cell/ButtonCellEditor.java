package org.adoptopenjdk.jitwatch.ui.report.cell;

import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.report.Report;
import org.adoptopenjdk.jitwatch.ui.main.IMemberSelectedListener;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
{
    private final IMemberSelectedListener selectionListener;
    private final ButtonCellModule buttonCellModule;
    private int row;
    private Object value;

    public ButtonCellEditor(IMemberSelectedListener selectionListener)
    {
        super();
        this.selectionListener = selectionListener;
        buttonCellModule = new ButtonCellModule();
        buttonCellModule.getButton().addActionListener( this );
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column)
    {
        this.row = row;
        this.value = value;
        if (value instanceof Report)
        {
            buttonCellModule.setReport((Report) value);
        }
        return buttonCellModule.getPanel();
    }

    @Override
    public Object getCellEditorValue()
    {
        return value;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (value instanceof Report)
        {
            Report report = (Report) value;
            IMetaMember member = report.getCaller();
            if (member != null)
            {
                selectionListener.selectMember(member, true, true);
                if (report.getCompilationIndex() != -1)
                {
                    member.setSelectedCompilation(report.getCompilationIndex());
                }
            }
        }
    }

    @Override
    public boolean isCellEditable(EventObject e)
    {
        return true;
    }
}
