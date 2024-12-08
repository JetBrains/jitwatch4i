/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.main;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.adoptopenjdk.jitwatch.core.JITWatchConfig;
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
    private Tree treeView;
    private DefaultMutableTreeNode rootItem;
    private DefaultTreeModel treeModel;

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

        rootItem = new DefaultMutableTreeNode(TREE_PACKAGE_ROOT);
        treeModel = new DefaultTreeModel(rootItem);
        treeView = new Tree(treeModel);
        treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        treeView.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent e)
            {
                if (!selectedProgrammatically)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeView.getLastSelectedPathComponent();
                    if (node == null) return;
                    Object value = node.getUserObject();
                    if (value instanceof MetaClass)
                    {
                        parent.metaClassSelectedFromClassTree((MetaClass) value);
                    }
                }
            }
        });

        treeView.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                Object value = node.getUserObject();
                if (value instanceof MetaPackage)
                {
                    openPackageNodes.add(value.toString());
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                Object value = node.getUserObject();
                if (value instanceof MetaPackage)
                {
                    openPackageNodes.remove(value.toString());
                }
            }
        });

        treeView.setCellRenderer(new CustomTreeCellRenderer());

        JBScrollPane treeScrollPane = new JBScrollPane(treeView);
        add(treeScrollPane, BorderLayout.CENTER);
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
            boolean hasCompiledChildren = false;

            if (value instanceof MetaPackage && ((MetaPackage) value).hasCompiledClasses())
            {
                hasCompiledChildren = true;
            }
            else if (value instanceof MetaClass && ((MetaClass) value).hasCompiledMethods())
            {
                hasCompiledChildren = true;
            }

            found = new DefaultMutableTreeNode(value);
            parent.insert(found, placeToInsert);

            if (value instanceof MetaPackage)
            {
                final String packageName = value.toString();
                if (sameVmCommand && openPackageNodes.contains(packageName))
                {
                    TreePath path = new TreePath(found.getPath());
                    treeView.expandPath(path);
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

        treeModel.reload();
    }

    private void showTree(DefaultMutableTreeNode currentNode, MetaPackage mp)
    {
        DefaultMutableTreeNode packageNode = findOrCreateTreeNode(currentNode, mp);

        List<MetaPackage> childPackages = mp.getChildPackages();

        for (MetaPackage childPackage : childPackages)
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

        List<MetaClass> packageClasses = mp.getPackageClasses();

        for (MetaClass packageClass : packageClasses)
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
        treeView.setSelectionPath(path);
        selectedProgrammatically = false;
    }

    public void scrollTo(int rowsAbove)
    {
        treeView.scrollRowToVisible(rowsAbove);
    }

    public DefaultMutableTreeNode getRootItem()
    {
        return rootItem;
    }

    public void clear()
    {
        rootItem.removeAllChildren();
        treeModel.reload();
    }

    public void scrollPathToVisible(DefaultMutableTreeNode node)
    {
        TreePath path = new TreePath(node.getPath());
        treeView.scrollPathToVisible(path);
    }

    private class CustomTreeCellRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

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

            return this;
        }
    }

    private static final String TREE_PACKAGE_ROOT = "Root";
}
