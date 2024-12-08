/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.toplist;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.IReadOnlyJITDataModel;
import org.adoptopenjdk.jitwatch.toplist.*;
import org.adoptopenjdk.jitwatch.ui.main.IMemberSelectedListener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.*;

public class TopListPanel extends JPanel
{
    private static final String MEMBER = "Member";
    private DefaultTableModel tableModel;
    private JBTable tableView;
    private TopListWrapper topListWrapper;

    public TopListPanel(IMemberSelectedListener selectionListener, IReadOnlyJITDataModel model)
    {
        setLayout(new BorderLayout());

        TopListWrapper tlLargestNative = new TopListWrapper("Largest Native Methods",
                new NativeMethodSizeTopListVisitable(model, true), new String[]{"Bytes", MEMBER, "_"});

        TopListWrapper tlInlineFailReasons = new TopListWrapper("Inlining Failure Reasons",
                new InliningFailReasonTopListVisitable(model, true), new String[]{"Count", "Reason", "_"});

        TopListWrapper tlIntrinsics = new TopListWrapper("Most-used Intrinsics",
                new MostUsedIntrinsicsTopListVisitable(model, true), new String[] { "Count", "Intrinsic", "_" });

        TopListWrapper tlHotThrows = new TopListWrapper("Hot throws",
                new HotThrowTopListVisitable(model, true), new String[] { "Count", "Hot Throw", "_" });

        TopListWrapper tlLargestBytecode = new TopListWrapper("Largest Bytecode Methods",
                new CompiledAttributeTopListVisitable(model, ATTR_BYTES, true), new String[] { "Bytes", MEMBER, "_" });

        TopListWrapper tlSlowestCompilation = new TopListWrapper("Slowest Compilation Times",
                new CompileTimeTopListVisitable(model, true), new String[] { "Milliseconds", MEMBER, "_" });

        TopListWrapper tlMostDecompiled = new TopListWrapper("Most Decompiled Methods",
                new CompiledAttributeTopListVisitable(model, ATTR_DECOMPILES, true), new String[] { "Decompiles", MEMBER, "_" });

        TopListWrapper tlCompilationOrder = new TopListWrapper("Compilation Order",
                new AbstractTopListVisitable(model, false)
                {
                    @Override
                    public void visit(IMetaMember mm)
                    {
                        String compileID = mm.getCompiledAttribute(ATTR_COMPILE_ID);
                        String compileKind = mm.getCompiledAttribute(ATTR_COMPILE_KIND);
                        if (compileID != null && (compileKind == null || !OSR.equals(compileKind)))
                        {
                            long value = Long.valueOf(mm.getCompiledAttribute(ATTR_COMPILE_ID));
                            topList.add(new MemberScore(mm, value));
                        }
                    }
                }, new String[]{"Order", MEMBER, "_"});

        TopListWrapper tlCompilationOrderOSR = new TopListWrapper("Compilation Order (OSR)",
                new AbstractTopListVisitable(model, false)
                {
                    @Override
                    public void visit(IMetaMember mm)
                    {
                        String compileID = mm.getCompiledAttribute(ATTR_COMPILE_ID);
                        String compileKind = mm.getCompiledAttribute(ATTR_COMPILE_KIND);
                        if (compileID != null && compileKind != null && OSR.equals(compileKind))
                        {
                            long value = Long.valueOf(mm.getCompiledAttribute(ATTR_COMPILE_ID));
                            topList.add(new MemberScore(mm, value));
                        }
                    }
                }, new String[] { "Order", MEMBER, "_" });

        TopListWrapper tlStaleTasks = new TopListWrapper("Most Stale Tasks", new StaleTaskToplistVisitable(model, true),
                new String[] { "Count", "Member", "_" });

        final Map<String, TopListWrapper> attrMap = new HashMap<>();

        attrMap.put(tlLargestNative.getTitle(), tlLargestNative);
        attrMap.put(tlInlineFailReasons.getTitle(), tlInlineFailReasons);
        attrMap.put(tlIntrinsics.getTitle(), tlIntrinsics);
        attrMap.put(tlHotThrows.getTitle(), tlHotThrows);
        attrMap.put(tlLargestBytecode.getTitle(), tlLargestBytecode);
        attrMap.put(tlSlowestCompilation.getTitle(), tlSlowestCompilation);
        attrMap.put(tlMostDecompiled.getTitle(), tlMostDecompiled);
        attrMap.put(tlCompilationOrder.getTitle(), tlCompilationOrder);
        attrMap.put(tlCompilationOrderOSR.getTitle(), tlCompilationOrderOSR);
        attrMap.put(tlStaleTasks.getTitle(), tlStaleTasks);

        List<String> keyList = new ArrayList<>(attrMap.keySet());
        Collections.sort(keyList);  // Sort the list alphabetically

        ComboBox<String> comboBox = new ComboBox<>(new DefaultComboBoxModel<>(keyList.toArray(new String[0])));
        comboBox.setSelectedItem(tlLargestNative.getTitle());
        topListWrapper = tlLargestNative;

        comboBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String selected = (String) comboBox.getSelectedItem();
                topListWrapper = attrMap.get(selected);
                buildTableView(topListWrapper);
            }
        });

        tableModel = new DefaultTableModel();
        tableView = new JBTable(tableModel);
        JBScrollPane scrollPane = new JBScrollPane(tableView);

        tableModel.setColumnIdentifiers(topListWrapper.getColumns());
        tableView.removeColumn(tableView.getColumnModel().getColumn(2));

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setComparator(0, Comparator.comparing(o -> ((Long) o)));
        tableView.setRowSorter(sorter);

        tableView.getSelectionModel().addListSelectionListener(event ->
        {
            int selectedRow = tableView.getSelectedRow();
            if (selectedRow >= 0)
            {
                ITopListScore selectedScore = (ITopListScore) tableModel.getValueAt(selectedRow, 2);
                if (selectedScore != null && selectedScore instanceof MemberScore)
                {
                    selectionListener.selectMember((IMetaMember) selectedScore.getKey(), true, true);
                }
            }
        });


        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        //comboPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        comboPanel.add(comboBox);

        add(comboPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        buildTableView(topListWrapper);
    }

    public String getTitle()
    {
        return "Tops";
    }

    private void buildTableView(TopListWrapper topListWrapper)
    {
        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(topListWrapper.getColumns());
        tableView.removeColumn(tableView.getColumnModel().getColumn(2));
        tableView.getColumnModel().getColumn(0).setPreferredWidth(100);
        tableView.getColumnModel().getColumn(1).setPreferredWidth(900);

        List<ITopListScore> topList = topListWrapper.getVisitable().buildTopList();
        for (ITopListScore score : topList)
        {
            tableModel.addRow(new Object[]{score.getScore(), score.getKey(), score});
        }
    }
}
