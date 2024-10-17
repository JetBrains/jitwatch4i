/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.report.eliminatedallocation;

import com.intellij.ui.table.JBTable;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.report.Report;
import org.adoptopenjdk.jitwatch.ui.report.IReportRowBean;
import org.adoptopenjdk.jitwatch.ui.report.IReportTable;
import org.adoptopenjdk.jitwatch.ui.report.cell.LinkedBCICellRenderer;
import org.adoptopenjdk.jitwatch.ui.report.cell.TextTableCellRenderer;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class EliminatedAllocationTable extends JBTable implements IReportTable
{
    private List<IReportRowBean> rows;

    public EliminatedAllocationTable(List<IReportRowBean> rows)
    {
        super(new EliminatedAllocationTableModel(rows));
        this.rows = rows;

        TableColumnModel columnModel = getColumnModel();
        int totalWidth = 1000;
        columnModel.getColumn(0).setPreferredWidth((int) (totalWidth * 0.2));
        columnModel.getColumn(1).setPreferredWidth((int) (totalWidth * 0.2));
        columnModel.getColumn(2).setPreferredWidth((int) (totalWidth * 0.2));
        columnModel.getColumn(3).setPreferredWidth((int) (totalWidth * 0.12));
        columnModel.getColumn(4).setPreferredWidth((int) (totalWidth * 0.1));
        columnModel.getColumn(5).setPreferredWidth((int) (totalWidth * 0.18));

        // Set custom cell renderers
        getColumnModel().getColumn(0).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(1).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(2).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(3).setCellRenderer(new LinkedBCICellRenderer());
        getColumnModel().getColumn(4).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(5).setCellRenderer(new TextTableCellRenderer());

        // Add mouse listener for the BCI column
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

    public void setRows(List<IReportRowBean> rows)
    {
        this.rows = rows;
        EliminatedAllocationTableModel model = (EliminatedAllocationTableModel) getModel();
        model.setRows(rows);
        model.fireTableDataChanged();
    }

    public static class EliminatedAllocationTableModel extends AbstractTableModel
    {
        private List<IReportRowBean> rows;
        private final String[] columnNames = {
                "Class", "Member", "Compilation", "BCI", "How", "Eliminated Type"
        };

        public EliminatedAllocationTableModel(List<IReportRowBean> rows)
        {
            this.rows = rows;
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
            EliminatedAllocationRowBean row = (EliminatedAllocationRowBean) rows.get(rowIndex);
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
                    return row.getEliminatedType();
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
            return false;
        }
    }
}
