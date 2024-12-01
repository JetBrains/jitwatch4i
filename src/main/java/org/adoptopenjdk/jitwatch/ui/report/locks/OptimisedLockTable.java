/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.report.locks;

import com.intellij.ui.table.JBTable;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.report.Report;
import org.adoptopenjdk.jitwatch.ui.main.IMemberSelectedListener;
import org.adoptopenjdk.jitwatch.ui.report.IReportRowBean;
import org.adoptopenjdk.jitwatch.ui.report.IReportTable;
import org.adoptopenjdk.jitwatch.ui.report.cell.*;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class OptimisedLockTable extends JBTable implements IReportTable
{
    private JBTable table;
    private List<IReportRowBean> rows;

    public OptimisedLockTable(IMemberSelectedListener selectionListener, List<IReportRowBean> rows)
    {
        super(new OptimisedLockTableModel(rows, 3));
        this.rows = rows;

        TableColumnModel columnModel = getColumnModel();
        int totalWidth = 1000;
        columnModel.getColumn(0).setPreferredWidth((int) (totalWidth * 0.2));
        columnModel.getColumn(1).setPreferredWidth((int) (totalWidth * 0.2));
        columnModel.getColumn(2).setPreferredWidth((int) (totalWidth * 0.2));
        columnModel.getColumn(3).setPreferredWidth((int) (totalWidth * 0.12));
        columnModel.getColumn(4).setPreferredWidth((int) (totalWidth * 0.1));
        columnModel.getColumn(5).setPreferredWidth((int) (totalWidth * 0.18));

        getColumnModel().getColumn(0).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(1).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(2).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(3).setCellRenderer(new ButtonCellRenderer());
        getColumnModel().getColumn(3).setCellEditor(new ButtonCellEditor(selectionListener));
        getColumnModel().getColumn(4).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(5).setCellRenderer(new TextTableCellRenderer());

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int column = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());
                if (column == 3 && row >= 0)
                { // BCI column
                    int modelRow = convertRowIndexToModel(row);
                    IReportRowBean rowBean = rows.get(modelRow);
                    Report report = rowBean.getReport();
                    if (report != null && report.getCaller() != null)
                    {
                        IMetaMember member = report.getCaller();
                        if (report.getCompilationIndex() != -1)
                        {
                            member.setSelectedCompilation(report.getCompilationIndex());
                        }
                    }
                }
            }
        });
    }

    public JBTable getTable()
    {
        return table;
    }

    public void setRows(List<IReportRowBean> rows)
    {
        this.rows = rows;
        OptimisedLockTableModel model = (OptimisedLockTableModel) getModel();
        model.setRows(rows);
        model.fireTableDataChanged();
    }

    public static class OptimisedLockTableModel extends AbstractTableModel
    {
        private List<IReportRowBean> rows;
        private final int editableColumn;
        private final String[] columnNames = {
                "Class", "Member", "Compilation", "BCI", "How", "Optimisation Kind"
        };

        public OptimisedLockTableModel(List<IReportRowBean> rows, int editableColumn)
        {
            this.rows = rows;
            this.editableColumn = editableColumn;
        }

        public void setRows(List<IReportRowBean> rows)
        {
            this.rows = rows;
        }

        @Override
        public int getRowCount()
        {
            return rows.size();
        }

        @Override
        public int getColumnCount()
        {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column)
        {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            OptimisedLockRowBean row = (OptimisedLockRowBean) rows.get(rowIndex);
            switch (columnIndex)
            {
                case 0:
                    return row.getMetaClass();
                case 1:
                    return row.getMember();
                case 2:
                    return row.getCompilation();
                case 3:
                    return row.getReport();
                case 4:
                    return row.getKind();
                case 5:
                    return row.getOptimisationKind();
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex == 3)
            {
                return Report.class;
            }
            else
            {
                return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == editableColumn;
        }
    }
}
