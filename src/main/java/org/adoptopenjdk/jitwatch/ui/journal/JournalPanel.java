/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.journal;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.main.ICompilationChangeListener;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.*;

public class JournalPanel extends JPanel implements ICompilationChangeListener
{
  private final JTextPane textPane;
  private final StyledDocument doc;

  private final Style styleDefault;
  private final Style styleRed;
  private final Style styleGreen;
  private final Style styleBlue;

  public JournalPanel()
  {
    setLayout(new BorderLayout());

    textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    doc = textPane.getStyledDocument();

    styleDefault = createStyle(JBColor.namedColor("TextPane.foreground", new JBColor(0x000000, 0xBBBBBB)));

    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    styleRed     = createStyle(scheme.getAttributes(DefaultLanguageHighlighterColors.KEYWORD).getForegroundColor());
    styleGreen   = createStyle(scheme.getAttributes(DefaultLanguageHighlighterColors.STRING).getForegroundColor());
    styleBlue    = createStyle(scheme.getAttributes(DefaultLanguageHighlighterColors.NUMBER).getForegroundColor());

    add(new JBScrollPane(textPane), BorderLayout.CENTER);
  }

  private Style createStyle(Color color)
  {
    Style s = textPane.addStyle(null, null);
    StyleConstants.setForeground(s, color);
    StyleConstants.setFontFamily(s, Font.MONOSPACED);
    StyleConstants.setFontSize(s, 12);
    return s;
  }

  @Override
  public void compilationChanged(IMetaMember member)
  {
    clear();

    if (member == null || !member.isCompiled())
    {
      return;
    }

    Compilation c = member.getSelectedCompilation();
    if (c == null)
    {
      return;
    }

    renderCompilation(c);
  }

  private void renderCompilation(Compilation compilation)
  {
    String[] lines = compilation.toStringVerbose().split("\n");

    for (String line : lines)
    {
      Style style = styleDefault;

      if (line.contains("<" + TAG_INLINE_FAIL))
      {
        style = styleRed;
      }
      else if (line.contains("<" + TAG_INLINE_SUCCESS))
      {
        style = styleGreen;
      }
      else if (line.contains("<" + TAG_INTRINSIC))
      {
        style = styleBlue;
      }

      appendLine(line, style);
    }
  }

  private void appendLine(String text, Style style)
  {
    try
    {
      doc.insertString(doc.getLength(), text + "\n", style);
    }
    catch (BadLocationException ignored)
    {
    }
  }

  public void clear()
  {
    textPane.setText("");
  }
}
