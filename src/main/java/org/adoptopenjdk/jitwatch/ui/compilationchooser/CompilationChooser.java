/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.compilationchooser;

import com.intellij.openapi.ui.ComboBox;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.main.IMemberSelectedListener;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;

public class CompilationChooser
{
    private DefaultComboBoxModel<String> comboCompilationList = new DefaultComboBoxModel<>();

    private ComboBox<String> comboSelectedCompilation;

    private IMetaMember member;

    private boolean ignoreChange = false;

    public CompilationChooser(final IMemberSelectedListener selectionListener)
    {
        comboSelectedCompilation = new ComboBox<>(comboCompilationList);
        comboSelectedCompilation.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (!ignoreChange)
                {
                    int index = comboSelectedCompilation.getSelectedIndex();
                    selectionListener.selectCompilation(member, index);
                }
            }
        });
    }

    public synchronized void clear()
    {
        ignoreChange = true;

        comboCompilationList.removeAllElements();
        comboSelectedCompilation.setSelectedIndex(-1);

        ignoreChange = false;
    }

    public synchronized void compilationChanged(IMetaMember member)
    {
        ignoreChange = true;

        comboSelectedCompilation.setSelectedIndex(-1);
        comboCompilationList.removeAllElements();

        if (member != null)
        {
            this.member = member;

            List<Compilation> compilations = member.getCompilations();

            for (Compilation compilation : compilations)
            {
                comboCompilationList.addElement(compilation.getSignature());
            }

            Compilation selectedCompilation = member.getSelectedCompilation();

            if (selectedCompilation != null)
            {
                comboSelectedCompilation.setSelectedIndex(selectedCompilation.getIndex());
            }
        }

        ignoreChange = false;
    }

    public JComboBox<String> getCombo()
    {
        return comboSelectedCompilation;
    }
}
