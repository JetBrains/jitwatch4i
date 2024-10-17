/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.nmethod;

import com.intellij.ui.components.JBScrollPane;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.main.ICompilationChangeListener;
import org.adoptopenjdk.jitwatch.ui.main.IPrevNextCompilationListener;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNMethodPanel extends JPanel implements ICompilationChangeListener, IPrevNextCompilationListener
{
    protected static final Color COLOR_UNSELECTED_COMPILATION = new Color(0, 196, 0);
    protected static final Color COLOR_SELECTED_COMPILATION = new Color(0, 220, 255);
    protected static final Color COLOR_OTHER_MEMBER_COMPILATIONS = new Color(0, 0, 160);

    private static class CompilationListener
    {
        final int x;
        final int y;
        final int width;
        final int height;
        final Compilation compilation;

        public CompilationListener(int x, int y, int width, int height, Compilation compilation)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.compilation = compilation;
        }
    }

    protected JPanel vBoxStack;
    protected JPanel pane;
    protected JBScrollPane scrollPane;
    protected JPanel vBoxControls;

    private NMethodInfoPanel nMethodInfo;

    protected JITWatchUI parent;

    protected JButton btnZoomIn;
    protected JButton btnZoomOut;
    protected JButton btnZoomReset;

    protected double zoom = 1;

    private final List<CompilationListener> compilationListeners = new ArrayList<>();

    public AbstractNMethodPanel(JITWatchUI parent)
    {
        this.parent = parent;

        setLayout(new BorderLayout());

        vBoxStack = new JPanel();
        vBoxStack.setLayout(new BoxLayout(vBoxStack, BoxLayout.Y_AXIS));

        pane = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                paintGraph(g);
            }
        };

        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectCompilationFromClick(e.getPoint());
            }
        });

        pane.setPreferredSize(new Dimension(800, 300));

        scrollPane = new JBScrollPane(pane);
        scrollPane.setPreferredSize(new Dimension(800, 300));

        nMethodInfo = new NMethodInfoPanel(this);

        vBoxControls = buildControls();

        vBoxStack.add(scrollPane);
        vBoxStack.add(vBoxControls);
        vBoxStack.add(nMethodInfo);

        add(vBoxStack, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                resizeComponents();
            }
        });

        pane.setBackground(Color.BLACK);
    }

    protected abstract JPanel buildControls();

    protected void paintGraph(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, pane.getWidth(), pane.getHeight());
    }

    private void resizeComponents()
    {
        int newWidth = pane.getWidth();
        int newHeight = pane.getHeight();

        scrollPane.setPreferredSize(new Dimension(newWidth, newHeight));
        pane.setPreferredSize(new Dimension((int) (newWidth * zoom), newHeight));
        revalidate();
    }

    protected void plotMarker(Graphics g, double x, double y, Compilation compilation)
    {
        plotMarker(g, x, y, compilation, false);
    }

    protected void plotMarker(Graphics g, double x, double y, Compilation compilation, boolean invert)
    {
        double side = 12;
        double centre = x;

        double left = x - side / 2;
        double right = x + side / 2;

        double top;
        double bottom;

        if (invert)
        {
            top = y + side;
            bottom = y;
        }
        else
        {
            top = y - side;
            bottom = y;
        }

        Polygon triangle = new Polygon();
        triangle.addPoint((int) left, (int) bottom);
        triangle.addPoint((int) centre, (int) top);
        triangle.addPoint((int) right, (int) bottom);
        g.setColor(Color.WHITE);
        g.fillPolygon(triangle);
        g.setColor(Color.BLACK);
        g.drawPolygon(triangle);

        // attachListener(markerPanel, compilation);
    }

    protected void addMouseListenerForCompilation(int x, int y, int width, int height, Compilation compilation)
    {
        compilationListeners.add(new CompilationListener(x, y, width, height, compilation));
    }

    private void selectCompilationFromClick(Point point)
    {
        for (CompilationListener listener : compilationListeners)
        {
            if (point.x >= listener.x && point.y >= listener.y &&
                point.x < (listener.x + listener.width) && point.y < (listener.y + listener.height))
            {
                selectCompilation(listener.compilation.getMember(), listener.compilation.getIndex());
                return;
            }
        }
    }

    @Override
    public void selectPrevCompilation()
    {
        IMetaMember selectedMember = parent.getSelectedMember();

        if (selectedMember != null && selectedMember.getSelectedCompilation() != null)
        {
            int prevIndex = selectedMember.getSelectedCompilation().getIndex() - 1;
            selectCompilation(selectedMember, prevIndex);
        }
    }

    @Override
    public void selectNextCompilation()
    {
        IMetaMember selectedMember = parent.getSelectedMember();

        if (selectedMember != null && selectedMember.getSelectedCompilation() != null)
        {
            int nextIndex = selectedMember.getSelectedCompilation().getIndex() + 1;
            selectCompilation(selectedMember, nextIndex);
        }
    }

    private void selectCompilation(IMetaMember member, int index)
    {
        try
        {
            parent.selectCompilation(member, index);
        }
        catch (Exception e)
        {
            // TODO: just swallow FX exceptions
        }
        repaint();
    }

    @Override
    public void compilationChanged(IMetaMember member)
    {
        repaint();
    }

    protected void clear()
    {
        nMethodInfo.clear();
        compilationListeners.clear();

        IMetaMember member = parent.getSelectedMember();

        Compilation selectedCompilation = (member == null) ? null : member.getSelectedCompilation();

        if (selectedCompilation != null)
        {
            nMethodInfo.setInfo(selectedCompilation);
        }
    }
}
