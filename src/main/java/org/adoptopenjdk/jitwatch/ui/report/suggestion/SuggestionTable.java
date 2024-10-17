/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.report.suggestion;

import com.intellij.ui.table.JBTable;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.report.Report;
import org.adoptopenjdk.jitwatch.ui.report.IReportRowBean;
import org.adoptopenjdk.jitwatch.ui.report.IReportTable;
import org.adoptopenjdk.jitwatch.ui.report.cell.IntegerTableCellRenderer;
import org.adoptopenjdk.jitwatch.ui.report.cell.MemberTableCellRenderer;
import org.adoptopenjdk.jitwatch.ui.report.cell.TextTableCellRenderer;
import org.adoptopenjdk.jitwatch.ui.report.cell.TextWrapTableCellRenderer;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SuggestionTable extends JBTable implements IReportTable
{
    private List<IReportRowBean> rows;

    public SuggestionTable(List<IReportRowBean> rows)
    {
        super(new SuggestionTableModel(rows));
        this.rows = rows;

        TableColumnModel columnModel = getColumnModel();
        int totalWidth = 1000;
        columnModel.getColumn(0).setPreferredWidth((int) (totalWidth * 0.05)); // Score
        columnModel.getColumn(1).setPreferredWidth((int) (totalWidth * 0.1));  // Type
        columnModel.getColumn(2).setPreferredWidth((int) (totalWidth * 0.35)); // Caller
        columnModel.getColumn(3).setPreferredWidth((int) (totalWidth * 0.5));  // Suggestion

        getColumnModel().getColumn(0).setCellRenderer(new IntegerTableCellRenderer());
        getColumnModel().getColumn(1).setCellRenderer(new TextTableCellRenderer());
        getColumnModel().getColumn(2).setCellRenderer(new MemberTableCellRenderer());
        getColumnModel().getColumn(3).setCellRenderer(new TextWrapTableCellRenderer());

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int column = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());
                if (column == 2 && row >= 0)
                { // Caller column
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
        SuggestionTableModel model = (SuggestionTableModel) getModel();
        model.setRows(rows);
        model.fireTableDataChanged();
    }

    public static class SuggestionTableModel extends AbstractTableModel
    {
        private List<IReportRowBean> rows;
        private final String[] columnNames = {"Score", "Type", "Caller", "Suggestion"};

        public SuggestionTableModel(List<IReportRowBean> rows)
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
            SuggestionRowBean row = (SuggestionRowBean) rows.get(rowIndex);
            switch (columnIndex)
            {
                case 0:
                    return row.getScore();
                case 1:
                    return row.getType();
                case 2:
                    return row.getReport();
                case 3:
                    return row.getText();
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            switch (columnIndex)
            {
                case 0:
                    return Integer.class;
                case 2:
                    return Report.class;
                default:
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
