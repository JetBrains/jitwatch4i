/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.main;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.util.ui.ColumnInfo;
import org.adoptopenjdk.jitwatch.core.JITWatchConfig;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.adoptopenjdk.jitwatch.model.MetaPackage;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassTreePanel extends JPanel
{
    private TreeTable treeTable;
    private DefaultMutableTreeNode rootItem;
    private ListTreeTableModel treeTableModel;

    private JITWatchUI parent;
    private JITWatchConfig config;

    private boolean sameVmCommand;
    private Set<String> openPackageNodes = new HashSet<>();

    private boolean selectedProgrammatically = false;

    private Icon disabledPackageIcon = IconLoader.getDisabledIcon(AllIcons.Nodes.Package);
    private Icon disabledClassIcon = IconLoader.getDisabledIcon(AllIcons.Nodes.Class);

    public ClassTreePanel(final JITWatchUI parent, final JITWatchConfig config)
    {
        this.parent = parent;
        this.config = config;

        setLayout(new BorderLayout());

        rootItem = new DefaultMutableTreeNode("Root");

        ColumnInfo<DefaultMutableTreeNode, Long> compilationTimeColumnInfo = new ColumnInfo<DefaultMutableTreeNode, Long>("Compilation Time (ms)")
        {
            @Override
            public Long valueOf(DefaultMutableTreeNode node)
            {
                return calculateCompilationTime(node);
            }

            @Override
            public Class<?> getColumnClass()
            {
                return Long.class;
            }
        };

        ColumnInfo[] columns = new ColumnInfo[] { new TreeColumnInfo("Class Name"), compilationTimeColumnInfo };

        treeTableModel = new ListTreeTableModel(rootItem, columns);
        treeTable = new TreeTable(treeTableModel);

        JTree tree = treeTable.getTree();
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new CustomTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent e)
            {
                if (!selectedProgrammatically)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (node == null)
                    {
                        return;
                    }
                    Object value = node.getUserObject();
                    if (value instanceof MetaClass)
                    {
                        parent.metaClassSelectedFromClassTree((MetaClass) value);
                    }
                }
            }
        });

        tree.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            @Override
            public void treeWillExpand(TreeExpansionEvent event)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                Object value = node.getUserObject();
                if (value instanceof MetaPackage)
                {
                    openPackageNodes.add(value.toString());
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                Object value = node.getUserObject();
                if (value instanceof MetaPackage)
                {
                    openPackageNodes.remove(value.toString());
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(treeTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private DefaultMutableTreeNode findOrCreateTreeNode(final DefaultMutableTreeNode parent, final Object value)
    {
        int childCount = parent.getChildCount();
        DefaultMutableTreeNode found = null;

        int placeToInsert = 0;
        boolean foundInsertPos = false;

        for (int i = 0; i < childCount; i++)
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            int stringCompare = child.getUserObject().toString().compareTo(value.toString());

            if (stringCompare == 0)
            {
                found = child;
                break;
            }
            else if (!foundInsertPos && stringCompare < 0)
            {
                if (!(child.getUserObject() instanceof MetaPackage && value instanceof MetaClass))
                {
                    placeToInsert++;
                }
            }
            else
            {
                if (child.getUserObject() instanceof MetaPackage && value instanceof MetaClass)
                {
                    placeToInsert++;
                }
                else
                {
                    foundInsertPos = true;
                }
            }
        }

        if (found == null)
        {
            found = new DefaultMutableTreeNode(value);
            parent.insert(found, placeToInsert);

            if (value instanceof MetaPackage)
            {
                final String packageName = value.toString();
                if (sameVmCommand && openPackageNodes.contains(packageName))
                {
                    TreePath path = new TreePath(found.getPath());
                    treeTable.getTree().expandPath(path);
                }
            }
        }

        return found;
    }

    public void clearOpenPackageHistory()
    {
        openPackageNodes.clear();
    }

    public void showTree(boolean sameVmCommand)
    {
        this.sameVmCommand = sameVmCommand;

        List<MetaPackage> roots = parent.getPackageManager().getRootPackages();

        for (MetaPackage mp : roots)
        {
            boolean allowed = true;

            if (!mp.hasCompiledClasses() && config.isShowOnlyCompiledClasses())
            {
                allowed = false;
            }

            if (allowed)
            {
                showTree(rootItem, mp);
            }
        }

        treeTable.getTree().expandRow(0);
    }

    private void showTree(DefaultMutableTreeNode currentNode, MetaPackage mp)
    {
        DefaultMutableTreeNode packageNode = findOrCreateTreeNode(currentNode, mp);

        for (MetaPackage childPackage : mp.getChildPackages())
        {
            boolean allowed = true;
            if (!childPackage.hasCompiledClasses() && config.isShowOnlyCompiledClasses())
            {
                allowed = false;
            }

            if (allowed)
            {
                showTree(packageNode, childPackage);
            }
        }

        for (MetaClass packageClass : mp.getPackageClasses())
        {
            boolean allowed = true;

            if (!packageClass.hasCompiledMethods() && config.isShowOnlyCompiledClasses())
            {
                allowed = false;
            }

            if (allowed)
            {
                findOrCreateTreeNode(packageNode, packageClass);
            }
        }
    }

    public void select(DefaultMutableTreeNode node)
    {
        selectedProgrammatically = true;
        TreePath path = new TreePath(node.getPath());
        treeTable.getTree().setSelectionPath(path);
        selectedProgrammatically = false;
    }

    public void scrollTo(int rowsAbove)
    {
        treeTable.getTree().scrollRowToVisible(rowsAbove);
    }

    public DefaultMutableTreeNode getRootItem()
    {
        return rootItem;
    }

    public void clear()
    {
        rootItem.removeAllChildren();
        treeTableModel.reload();
    }

    public void scrollPathToVisible(DefaultMutableTreeNode node)
    {
        TreePath path = new TreePath(node.getPath());
        treeTable.getTree().scrollPathToVisible(path);
    }

    private long calculateCompilationTime(DefaultMutableTreeNode node)
    {
        long total = 0;
        Object userObject = node.getUserObject();

        if (userObject instanceof MetaClass)
        {
            MetaClass metaClass = (MetaClass) userObject;
            for (IMetaMember method: metaClass.getMetaMembers())
            {
                for (Compilation compilation : method.getCompilations())
                {
                   total += compilation.getCompilationDuration();
                }
            }
        }
        else if (userObject instanceof MetaPackage)
        {
            for (int i = 0; i < node.getChildCount(); i++)
            {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                total += calculateCompilationTime(child);
            }
        }

        return total;
    }

    private class CustomTreeCellRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof MetaPackage)
                {
                    if (((MetaPackage) userObject).hasCompiledClasses())
                    {
                        setIcon(AllIcons.Nodes.Package);
                    }
                    else
                    {
                        setIcon(disabledPackageIcon);
                    }
                }
                else if (userObject instanceof MetaClass)
                {
                    if (((MetaClass) userObject).hasCompiledMethods())
                    {
                        setIcon(AllIcons.Nodes.Class);
                    }
                    else
                    {
                        setIcon(disabledClassIcon);
                    }
                }
            }

            return this;
        }
    }
}
