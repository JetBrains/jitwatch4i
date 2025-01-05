package org.adoptopenjdk.jitwatch.ui.log;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;

public class LogPanel extends JPanel
{
    private JBTextArea textAreaLog;

    public LogPanel()
    {
        setLayout(new BorderLayout());

        textAreaLog = new JBTextArea();
        textAreaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JBScrollPane(textAreaLog), BorderLayout.CENTER);

        this.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event)
            {
                SwingUtilities.invokeLater(() -> textAreaLog.setCaretPosition(textAreaLog.getDocument().getLength()));
            }
            @Override
            public void ancestorRemoved(AncestorEvent event) {}
            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });

    }

    public void clear()
    {
        textAreaLog.setText("");
    }

    public void setText(String logText)
    {
        textAreaLog.setText(logText);
    }

    public void setCaretPosition(int i)
    {
        textAreaLog.setCaretPosition(i);
    }
}
