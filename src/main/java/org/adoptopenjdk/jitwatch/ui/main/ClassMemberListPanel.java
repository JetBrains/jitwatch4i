/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.main;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBList;
import org.adoptopenjdk.jitwatch.core.JITWatchConfig;
import org.adoptopenjdk.jitwatch.intrinsic.IntrinsicFinder;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassMemberListPanel extends JPanel
{
    private JBList<IMetaMember> memberList;
    private DefaultListModel<IMetaMember> memberListModel;
    private MetaClass metaClass = null;
    private JITWatchConfig config;

    private boolean selectedProgrammatically = false;

    private List<IMemberSelectedListener> listeners = new ArrayList<>();
    private Icon disabledMethodIcon = IconLoader.getDisabledIcon(AllIcons.Nodes.Method);

    public void registerListener(IMemberSelectedListener listener)
    {
        listeners.add(listener);
    }

    public void clear()
    {
        if (memberListModel != null)
        {
            memberListModel.clear();
        }
        metaClass = null;
    }

    private void notifyListeners(IMetaMember member)
    {
        for (IMemberSelectedListener listener : listeners)
        {
            listener.selectMember(member, false, true);
        }
    }

    public ClassMemberListPanel(final IStageAccessProxy proxy, final JITWatchConfig config)
    {
        this.config = config;

        memberListModel = new DefaultListModel<>();
        memberList = new JBList<>(memberListModel);
        memberList.setCellRenderer(new MetaMethodCell());
        memberList.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if (!selectedProgrammatically && !e.getValueIsAdjusting())
                {
                    IMetaMember newVal = memberList.getSelectedValue();
                    notifyListeners(newVal);
                }
            }
        });

        final JPopupMenu menuCompiled = buildContextMenuCompiledMember(proxy);
        final JPopupMenu menuUncompiled = buildContextMenuUncompiledMember(proxy);

        memberList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    showContextMenu(e);
                }
            }

            // TODO: remove?
            private void showContextMenu(MouseEvent e)
            {
                int index = memberList.locationToIndex(e.getPoint());
                if (index >= 0)
                {
                    memberList.setSelectedIndex(index);
                    IMetaMember selectedMember = memberList.getSelectedValue();
                    if (selectedMember.isCompiled())
                    {
                        menuCompiled.show(memberList, e.getX(), e.getY());
                    }
                    else
                    {
                        menuUncompiled.show(memberList, e.getX(), e.getY());
                    }
                }
            }
        });

        setLayout(new BorderLayout());
        add(new JScrollPane(memberList), BorderLayout.CENTER);
    }

    private JPopupMenu buildContextMenuCompiledMember(IStageAccessProxy proxy)
    {
        final JPopupMenu menu = new JPopupMenu();

        JMenuItem menuItemInlinedInto = new JMenuItem("Show inlined into");
        JMenuItem menuItemIntrinsics = new JMenuItem("Show intrinsics used");
        JMenuItem menuItemCallChain = new JMenuItem("Show compile chain");
        JMenuItem menuItemOptimizedVCalls = new JMenuItem("Show optimized virtual calls");

        menu.add(menuItemInlinedInto);
        menu.add(menuItemIntrinsics);
        menu.add(menuItemCallChain);
        menu.add(menuItemOptimizedVCalls);

        menuItemInlinedInto.addActionListener(getEventHandlerMenuItemInlinedInto(proxy));
        menuItemIntrinsics.addActionListener(getEventHandlerMenuItemIntrinsics(proxy));
        menuItemCallChain.addActionListener(getEventHandlerMenuItemCallChain(proxy));

        return menu;
    }

    private JPopupMenu buildContextMenuUncompiledMember(IStageAccessProxy proxy)
    {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem menuItemInlinedInto = new JMenuItem("Show inlined into");

        menu.add(menuItemInlinedInto);

        menuItemInlinedInto.addActionListener(getEventHandlerMenuItemInlinedInto(proxy));

        return menu;
    }

    private ActionListener getEventHandlerMenuItemInlinedInto(final IStageAccessProxy proxy)
    {
        return new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                proxy.openInlinedIntoReport(memberList.getSelectedValue());
            }
        };
    }

    private ActionListener getEventHandlerMenuItemIntrinsics(final IStageAccessProxy proxy)
    {
        return new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                IMetaMember member = memberList.getSelectedValue();
                String intrinsicsUsed = findIntrinsicsUsedByMember(member);
                // proxy.openTextViewer("Intrinsics used by " + member.toString(), intrinsicsUsed, false, false);
            }
        };
    }

    private ActionListener getEventHandlerMenuItemCallChain(final IStageAccessProxy proxy)
    {
        return new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
//                proxy.openCompileChain(memberList.getSelectedValue());
            }
        };
    }

    private String findIntrinsicsUsedByMember(IMetaMember member)
    {
        StringBuilder builder = new StringBuilder();

        IntrinsicFinder finder = new IntrinsicFinder();

        Map<String, String> intrinsics = finder.findIntrinsics(member);

        if (intrinsics.size() > 0)
        {
            addArrowWithNewLineToEachIntrinsicEntry(builder, intrinsics);
        }
        else
        {
            builder.append("No intrinsics used in this method");
        }

        return builder.toString();
    }

    private void addArrowWithNewLineToEachIntrinsicEntry(StringBuilder builder, Map<String, String> intrinsics)
    {
        for (Map.Entry<String, String> entry : intrinsics.entrySet())
        {
            builder.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }
    }

    public void setMetaClass(MetaClass metaClass)
    {
        this.metaClass = metaClass;

        if (metaClass != null)
        {
            List<IMetaMember> members = metaClass.getMetaMembers();

            if (members.size() > 0)
            {
                selectMember(members.get(0));
            }

            refresh();
        }
    }

    public void refresh()
    {
        clearClassMembers();

        if (metaClass != null)
        {
            List<IMetaMember> metaMembers = metaClass.getMetaMembers();

            for (IMetaMember member : metaMembers)
            {
                if (member.isCompiled() || !config.isShowOnlyCompiledMembers())
                {
                    addMember(member);
                }
            }
        }
    }

    private void addMember(IMetaMember member)
    {
        memberListModel.addElement(member);
    }

    public void clearClassMembers()
    {
        selectedProgrammatically = true;
        memberListModel.clear();
        selectedProgrammatically = false;
    }

    public void selectMember(IMetaMember selected)
    {
        selectedProgrammatically = true;
        memberList.clearSelection();

        if (selected != null)
        {
            for (int i = 0; i < memberListModel.getSize(); i++)
            {
                IMetaMember member = memberListModel.getElementAt(i);

                if (member.toString().equals(selected.toString()))
                {
                    memberList.setSelectedIndex(i);
                    memberList.ensureIndexIsVisible(i);
                    break;
                }
            }
        }

        selectedProgrammatically = false;
    }

    class MetaMethodCell extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            IMetaMember item = (IMetaMember) value;

            if (item != null)
            {
                setText(item.toStringUnqualifiedMethodName(false, false));
                if (item.isCompiled())
                {
                    setIcon(AllIcons.Nodes.Method);
                }
                else
                {
                    setIcon(disabledMethodIcon);
                }
            }
            else
            {
                setText("");
                setIcon(null);
            }
            return this;
        }
    }
}
