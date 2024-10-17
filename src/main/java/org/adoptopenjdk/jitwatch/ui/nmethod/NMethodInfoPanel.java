/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.nmethod;

import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.main.IPrevNextCompilationListener;
import org.adoptopenjdk.jitwatch.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class NMethodInfoPanel extends JPanel
{
    private JLabel lblCompileID;
    private JLabel lblClass;
    private JLabel lblMember;
    private JLabel lblCompilationNumber;
    private JLabel lblCompiler;

    private JLabel lblAddress;
    private JLabel lblBytecodeSize;
    private JLabel lblNativeSize;
    private JLabel lblQueuedTime;
    private JLabel lblCompilationStartTime;
    private JLabel lblNMethodEmittedTime;
    private JLabel lblCompileDuration;

    private List<JLabel> clearable = new ArrayList<>();

    private IPrevNextCompilationListener listener;

    private static final int DESCRIPTION_WIDTH = 128;

    public NMethodInfoPanel(IPrevNextCompilationListener listener)
    {
        this.listener = listener;

        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setLayout(new GridLayout(1, 2, 16, 0));  // Set grid layout for left and right columns

        add(makeLeftColumn());
        add(makeRightColumn());
    }

    private JPanel makeLeftColumn()
    {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblCompileID = new JLabel();
        lblClass = new JLabel();
        lblMember = new JLabel();
        lblCompilationNumber = new JLabel();
        lblCompiler = new JLabel();

        column.add(makeLabel("Compile ID", lblCompileID));
        column.add(makeLabel("Class", lblClass));
        column.add(makeLabel("Compiled Member", lblMember));
        column.add(makeCompilationNavigator());
        column.add(makeLabel("Compiler Used", lblCompiler));

        return column;
    }

    private JPanel makeRightColumn()
    {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        lblAddress = new JLabel();
        lblBytecodeSize = new JLabel();
        lblNativeSize = new JLabel();
        lblQueuedTime = new JLabel();
        lblCompilationStartTime = new JLabel();
        lblNMethodEmittedTime = new JLabel();
        lblCompileDuration = new JLabel();

        column.add(makeLabel("NMethod Address", lblAddress));
        column.add(makeSizeInfo());
        column.add(makeLabel("Queued at", lblQueuedTime));
        column.add(makeTimingInfo());
        column.add(makeLabel("Compile Duration", lblCompileDuration));

        return column;
    }

    private JPanel makeCompilationNavigator()
    {
        JPanel hbox = new JPanel();
        hbox.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));  // Horizontal layout

        JButton btnPrev = new JButton("Prev");
        JButton btnNext = new JButton("Next");

        btnPrev.setPreferredSize(new Dimension(60, 25));  // Set button size
        btnNext.setPreferredSize(new Dimension(60, 25));

        btnPrev.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                listener.selectPrevCompilation();
            }
        });

        btnNext.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                listener.selectNextCompilation();
            }
        });

        hbox.add(makeLabel("Compilation #", lblCompilationNumber));
        hbox.add(btnPrev);
        hbox.add(btnNext);

        clearable.add(lblCompilationNumber);

        return hbox;
    }

    private JPanel makeSizeInfo()
    {
        JPanel hbox = new JPanel();
        hbox.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        hbox.add(makeLabel("Bytecode Size", lblBytecodeSize));
        hbox.add(makeLabel("Native Size", lblNativeSize));

        clearable.add(lblBytecodeSize);
        clearable.add(lblNativeSize);

        return hbox;
    }

    private JPanel makeTimingInfo()
    {
        JPanel hbox = new JPanel();
        hbox.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        hbox.add(makeLabel("Compile Start", lblCompilationStartTime));
        hbox.add(makeLabel("NMethod Emitted", lblNMethodEmittedTime));

        clearable.add(lblCompilationStartTime);
        clearable.add(lblNMethodEmittedTime);

        return hbox;
    }

    private JPanel makeLabel(String labelText, JLabel labelValue)
    {
        JPanel hbox = new JPanel();
        hbox.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 0));

        JLabel descriptionLabel = new JLabel(labelText);
        descriptionLabel.setPreferredSize(new Dimension(DESCRIPTION_WIDTH, 25));
        hbox.add(descriptionLabel);
        hbox.add(labelValue);

        clearable.add(labelValue);

        return hbox;
    }

    public void setInfo(Compilation compilation)
    {
        IMetaMember compilationMember = compilation.getMember();

        int compilationCount = compilationMember.getCompilations().size();

        lblCompileID.setText(compilation.getCompileID());
        lblClass.setText(compilationMember.getMetaClass().getFullyQualifiedName());

        String fqMemberName = compilationMember.toStringUnqualifiedMethodName(true, true);
        lblMember.setText(fqMemberName);

        String compilerString = compilation.getCompiler();
        if (compilation.getLevel() != -1)
        {
            compilerString += " (Level " + compilation.getLevel() + ")";
        }

        lblCompiler.setText(compilerString);
        lblCompilationNumber.setText((1 + compilation.getIndex()) + " of " + compilationCount);

        lblBytecodeSize.setText(compilation.getBytecodeSize() + " bytes");
        lblQueuedTime.setText(StringUtil.formatTimestamp(compilation.getStampTaskQueued(), true));
        lblCompilationStartTime.setText(StringUtil.formatTimestamp(compilation.getStampTaskCompilationStart(), true));

        if (compilation.isFailed())
        {
            lblAddress.setText("Compilation failed, no nmethod emitted");
            lblNativeSize.setText("NA");
            lblCompileDuration.setText("NA");
            lblNMethodEmittedTime.setText("NA");
        }
        else
        {
            lblAddress.setText(compilation.getNativeAddress());
            lblNativeSize.setText(compilation.getNativeSize() + " bytes");
            lblCompileDuration.setText(compilation.getCompilationDuration() + "ms");
            lblNMethodEmittedTime.setText(StringUtil.formatTimestamp(compilation.getStampNMethodEmitted(), true));
        }
    }

    public void clear()
    {
        for (JLabel label : clearable)
        {
            label.setText("");
        }
    }
}
