/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.report;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.adoptopenjdk.jitwatch.report.Report;
import org.adoptopenjdk.jitwatch.ui.report.eliminatedallocation.EliminatedAllocationRowBean;
import org.adoptopenjdk.jitwatch.ui.report.eliminatedallocation.EliminatedAllocationTable;
import org.adoptopenjdk.jitwatch.ui.report.inlining.InliningRowBean;
import org.adoptopenjdk.jitwatch.ui.report.locks.OptimisedLockRowBean;
import org.adoptopenjdk.jitwatch.ui.report.locks.OptimisedLockTable;
import org.adoptopenjdk.jitwatch.ui.report.suggestion.SuggestionRowBean;
import org.adoptopenjdk.jitwatch.ui.report.suggestion.SuggestionTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReportPanel extends JPanel
{
    private JBTable table;
    private Set<String> filterPackageSet = new HashSet<>();
    private List<Report> reportList;
    private ReportStageType type;
    private JBScrollPane tableScrollPane;

    private JTextField filterTextField;
    private JButton applyFilterButton;

    public ReportPanel(ReportStageType type, List<Report> reportList)
    {
        this.reportList = reportList;
        this.type = type;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filterPanel = new JPanel(new BorderLayout(5, 5));
        JLabel filterLabel = new JLabel("Filter Packages:");
        filterTextField = new JTextField();
        applyFilterButton = new JButton("Apply");
        applyFilterButton.addActionListener(e -> setFilter(filterTextField.getText()));
        filterPanel.add(filterLabel, BorderLayout.WEST);
        filterPanel.add(filterTextField, BorderLayout.CENTER);
        filterPanel.add(applyFilterButton, BorderLayout.EAST);

        add(filterPanel, BorderLayout.NORTH);

        switch (type)
        {
            case SUGGESTION:
                table = new SuggestionTable(getReportRowBeanList());
                break;
            case ELIMINATED_ALLOCATION:
                table = new EliminatedAllocationTable(getReportRowBeanList());
                break;
            case ELIDED_LOCK:
                table = new OptimisedLockTable(getReportRowBeanList());
                break;
            case INLINING:
                break;
            default:
                throw new IllegalArgumentException("Unsupported ReportStageType");
        }

        table.setFillsViewportHeight(true);
        tableScrollPane = new JBScrollPane(table);
        add(tableScrollPane, BorderLayout.CENTER);

        display();
    }

    public String getTitle()
    {
        switch (type)
        {
            case SUGGESTION:
                return "Code Suggest";
            case ELIMINATED_ALLOCATION:
                return "Elim. Alloc";
            case ELIDED_LOCK:
                return "Opti Lock";
            case INLINING:
                break;
        }
        return "Rep.";
    }
    public void clear()
    {
        IReportTable reportTable = (IReportTable) table;
        reportTable.setRows(List.of());
    }

    public ReportStageType getType()
    {
        return type;
    }

    private void display()
    {
        clear();

        if (reportList.isEmpty())
        {
            remove(tableScrollPane);
            JLabel noResultsLabel = new JLabel("No results");
            noResultsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(noResultsLabel, BorderLayout.CENTER);
        }
        else
        {
            IReportTable reportTable = (IReportTable) table;
            reportTable.setRows(getReportRowBeanList());
        }

        revalidate();
        repaint();
    }

    private List<IReportRowBean> getReportRowBeanList()
    {
        List<IReportRowBean> result = new ArrayList<>();
        for (Report report : reportList)
        {
            boolean show = false;

            if (filterPackageSet.isEmpty())
            {
                show = true;
            }
            else
            {
                for (String allowedPackage : filterPackageSet)
                {
                    if (report.getCaller() != null
                            && report.getCaller().getFullyQualifiedMemberName().startsWith(allowedPackage.trim()))
                    {
                        show = true;
                    }
                }
            }

            if (show)
            {
                switch (type)
                {
                    case SUGGESTION:
                        result.add(new SuggestionRowBean(report));
                        break;
                    case ELIMINATED_ALLOCATION:
                        result.add(new EliminatedAllocationRowBean(report));
                        break;
                    case ELIDED_LOCK:
                        result.add(new OptimisedLockRowBean(report));
                        break;
                    case INLINING:
                        result.add(new InliningRowBean(report));
                        break;
                }
            }
        }
        return result;
    }

    public void setFilter(String packageFilter)
    {
        String[] packages = packageFilter.split(",");
        filterPackageSet.clear();
        for (String pkg : packages)
        {
            if (!pkg.trim().isEmpty())
            {
                filterPackageSet.add(pkg.trim());
            }
        }
        display();
    }

}
