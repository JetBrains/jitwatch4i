/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.parserchooser;

import com.intellij.openapi.ui.ComboBox;
import org.adoptopenjdk.jitwatch.parser.ParserType;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ParserChooser
{
    private ComboBox<ParserType> comboParser;

    public ParserChooser(final IParserSelectedListener selectionListener) {
        comboParser = new ComboBox<>();

        clear();

        comboParser.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ParserType selected = (ParserType) comboParser.getSelectedItem();
                selectionListener.parserSelected(selected);
            }
        });
    }

    public synchronized void clear()
    {
        comboParser.removeAllItems();

        for (ParserType parserType : ParserType.values())
        {
            comboParser.addItem(parserType);
        }

        comboParser.setSelectedIndex(-1); // Clear selection
    }

    public ComboBox<ParserType> getCombo()
    {
        return comboParser;
    }
}
