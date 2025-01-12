/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.nmethod.compilerthread;

import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.CompilerThread;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;
import org.adoptopenjdk.jitwatch.ui.nmethod.AbstractNMethodPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class CompilerThreadPanel extends AbstractNMethodPanel
{
    private int maxNativeSize = 0;
    private int maxBytecodeSize = 0;

    private long minTime;
    private long maxTime;
    private long timeRange;
    private long maxQueueLength = 0;

    private double panePlotWidth;
    private double paneLabelWidth = 48;

    private double paneWidth;
    private double paneHeight;

    private JPanel contextualControls;
    private JCheckBox cbOnlyFailures;
    private boolean showOnlyFailedCompiles = false;

    private enum PlotMode
    {
        NATIVE_SIZE, BYTECODE_SIZE, EXPANSIONS, TIMINGS, QUEUE_LENGTH
    }

    private class QueueCounter
    {
        private long timestamp;
        private boolean add;
        private Compilation compilation;

        public QueueCounter(long timestamp, boolean add, Compilation compilation)
        {
            this.timestamp = timestamp;
            this.add = add;
            this.compilation = compilation;
        }

        public long getTimestamp()
        {
            return timestamp;
        }

        public boolean isAdd()
        {
            return add;
        }

        public Compilation getCompilation()
        {
            return compilation;
        }
    }

    private PlotMode plotMode = PlotMode.NATIVE_SIZE;

    public CompilerThreadPanel(JITWatchUI parent)
    {
        super(parent);
    }

    public String getTitle()
    {
       return "Comp. Activity";
    }

    @Override
    protected JPanel buildControls()
    {
        JPanel vBoxControls = new JPanel();
        vBoxControls.setLayout(new BoxLayout(vBoxControls, BoxLayout.Y_AXIS)); // VBox equivalent
        vBoxControls.add(buildControlButtons());

        return vBoxControls;
    }

    private void findRanges()
    {
        List<CompilerThread> threads = parent.getJITDataModel().getCompilerThreads();

        int compilerThreadCount = threads.size();

        for (int i = 0; i < compilerThreadCount; i++)
        {
            CompilerThread thread = threads.get(i);

            long earliestQueuedTime = thread.getEarliestQueuedTime();
            long latestEmittedTime = thread.getLatestNMethodEmittedTime();

            if (i == 0)
            {
                minTime = earliestQueuedTime;
                maxTime = latestEmittedTime;
                maxNativeSize = thread.getLargestNativeSize();
                maxBytecodeSize = thread.getLargestBytecodeSize();
            }
            else
            {
                minTime = Math.min(minTime, earliestQueuedTime);
                maxTime = Math.max(maxTime, latestEmittedTime);
                maxNativeSize = Math.max(maxNativeSize, thread.getLargestNativeSize());
                maxBytecodeSize = Math.max(maxBytecodeSize, thread.getLargestBytecodeSize());
            }
        }
    }

    private JPanel buildControlButtons()
    {
        JPanel hboxControls = new JPanel();
        hboxControls.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));

        btnZoomIn = new JButton("Zoom In");
        btnZoomOut = new JButton("Zoom Out");
        btnZoomReset = new JButton("Reset");

        // btnZoomIn.setPreferredSize(new Dimension(60, 25));  // Setting button sizes
        // btnZoomOut.setPreferredSize(new Dimension(60, 25));
        // btnZoomReset.setPreferredSize(new Dimension(40, 25));

        btnZoomIn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                zoom += 0.2;
                repaint();
            }
        });

        btnZoomOut.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                zoom -= 0.2;
                zoom = Math.max(zoom, 1);
                repaint();
            }
        });

        btnZoomReset.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                zoom = 1;
                repaint();
            }
        });

        Component spacerStatus = Box.createHorizontalGlue();

        contextualControls = new JPanel();  // You can customize this JPanel as needed

        hboxControls.add(btnZoomIn);
        hboxControls.add(btnZoomOut);
        hboxControls.add(btnZoomReset);
        hboxControls.add(buildModeHBox());  // Assuming buildModePanel() builds the panel similar to buildModeHBox()
        hboxControls.add(contextualControls);
        hboxControls.add(spacerStatus);  // Spacer to align components

        return hboxControls;
    }

    private boolean preDraw()
    {
        clear();

        boolean ok = false;

        paneWidth = pane.getWidth() * zoom;
        panePlotWidth = paneWidth - paneLabelWidth;
        paneHeight = pane.getHeight();

        List<CompilerThread> threads = parent.getJITDataModel().getCompilerThreads();

        if (threads != null && !threads.isEmpty())
        {
            findRanges();
            timeRange = maxTime - minTime;
            timeRange *= 1.01;
            ok = true;
        }

        return ok;
    }

    private double getXOffset()
    {
        return paneLabelWidth;
    }

    protected void paintGraph(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        super.paintGraph(g);

        if (!preDraw())
        {
            return;
        }

        List<CompilerThread> threads = parent.getJITDataModel().getCompilerThreads();
        if (threads == null || threads.isEmpty())
        {
            return;
        }

        double rowHeight = paneHeight / threads.size();
        maxQueueLength = 0;
        double usableHeight = rowHeight * 0.9;
        double y = rowHeight / 2;

        for (CompilerThread thread : threads)
        {
            g.setColor(new Color(32, 32, 32));
            g.fillRect((int) getXOffset(), (int) (y - usableHeight / 2), (int) panePlotWidth, (int) usableHeight);

            if (plotMode == PlotMode.QUEUE_LENGTH)
            {
                plotQueueLengths(g2d, thread, y, usableHeight);
            }
            else
            {
                plotThread(g, thread, y, usableHeight);
            }

            y += rowHeight;
        }
    }

    private void plotThread(Graphics g, CompilerThread thread, double y, double rowHeight)
    {
        IMetaMember selectedMember = parent.getSelectedMember();
        Compilation selectedCompilation = (selectedMember == null) ? null : selectedMember.getSelectedCompilation();
        List<Compilation> compilations = thread.getCompilations();

        plotThreadHeader(g, thread, y, rowHeight);

        Color fillColour;
        boolean isCompilationOfSelectedMember;

        Graphics2D g2d = (Graphics2D) g;

        for (Compilation compilation : compilations)
        {
            if (selectedMember != null && selectedMember.equals(compilation.getMember()))
            {
                if (compilation.equals(selectedCompilation))
                {
                    fillColour = new Color(0, 220, 255);
                }
                else
                {
                    fillColour = new Color(0, 0, 160);
                }
                isCompilationOfSelectedMember = true;
            }
            else
            {
                fillColour = new Color(0, 196, 0);
                isCompilationOfSelectedMember = false;
            }

            if (plotMode == PlotMode.NATIVE_SIZE)
            {
                if (!compilation.isFailed())
                {
                    plotNativeSize(g2d, compilation, y, rowHeight, fillColour, isCompilationOfSelectedMember);
                }
            }
            else if (plotMode == PlotMode.BYTECODE_SIZE)
            {
                if (!compilation.isFailed())
                {
                    plotBytecodeSize(g2d, compilation, y, rowHeight, fillColour, isCompilationOfSelectedMember);
                }
            }
            else if (plotMode == PlotMode.EXPANSIONS)
            {
                if (!compilation.isFailed())
                {
                    plotExpansions(g2d, compilation, y, rowHeight, fillColour, isCompilationOfSelectedMember);
                }
            }
            else if (plotMode == PlotMode.TIMINGS)
            {
				if (!showOnlyFailedCompiles || (showOnlyFailedCompiles == compilation.isFailed()))
                {
                    plotQueuedCompileTimes(g2d, compilation, y, rowHeight, fillColour, isCompilationOfSelectedMember);
                }
            }
        }
    }

    private void plotQueueLengths(Graphics2D g, CompilerThread thread, double y, double rowHeight)
    {
        List<QueueCounter> counters = getQueueCounters(thread);

        long lastTimestamp = -1;
        plotThreadHeader(g, thread, y, rowHeight);  // Draw thread header

        List<Compilation> liveQueue = new LinkedList<>();
        double oneHeight = (1.0 / maxQueueLength) * rowHeight;
        oneHeight = Math.min(oneHeight, rowHeight / 20);

        IMetaMember selectedMember = parent.getSelectedMember();
        Compilation selectedCompilation = (selectedMember == null) ? null : selectedMember.getSelectedCompilation();

        int stringHeight = g.getFontMetrics().getHeight();

        for (QueueCounter counter : counters)
        {
            long timestamp = counter.getTimestamp();
            int queueLength = liveQueue.size();

            if (lastTimestamp != -1)
            {
                double x1 = getScaledTimestampX(lastTimestamp);
                double x2 = getScaledTimestampX(timestamp);
                double baseLine = y + rowHeight / 2;

                boolean compilationMemberInQueue = false;

                for (int i = 0; i < queueLength; i++)
                {
                    Compilation compilation = liveQueue.get(i);
                    double startY = baseLine - (i + 1) * oneHeight;
                    double rectWidth = x2 - x1;

                    g.setColor(Color.WHITE);
                    if (compilation.getMember().equals(selectedMember))
                    {
                        g.setColor(new Color(0, 220, 255));  // COLOR_SELECTED_COMPILATION
                        compilationMemberInQueue = true;
                    }

                    int ix = (int) Math.round(x1);
                    int iy = (int) Math.round(startY);
                    int iw = (int) Math.ceil(rectWidth);
                    int ih = (int) Math.round(oneHeight);

                    g.fillRect(ix, iy, iw, ih);

                    //if (rectWidth > 4)
                    {
                        g.drawRect(ix, iy, iw, ih);
                    }

                    addMouseListenerForCompilation(ix, iy, iw, ih, compilation);
                }

                float labelX = (float) getXOffset() + 4;
                float labelY = (float) (baseLine - rowHeight + stringHeight / 2 + 4);
                String label = String.valueOf(maxQueueLength);

                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(label);
                int textHeight = fm.getAscent();

                g.setColor(Color.BLACK);
                g.fillRect(Math.round(labelX), Math.round(labelY - textHeight), textWidth, textHeight);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.YELLOW);
                g.drawString(label, labelX, labelY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                if (compilationMemberInQueue)
                {
                    plotMarker(g, x1, y - rowHeight / 2, selectedCompilation, true);
                }
            }

            Compilation compilation = counter.getCompilation();
            if (counter.isAdd())
            {
                liveQueue.add(compilation);
            }
            else
            {
                liveQueue.remove(compilation);
            }

            lastTimestamp = timestamp;
        }
    }

    private List<QueueCounter> getQueueCounters(CompilerThread thread)
    {
        List<QueueCounter> result = new ArrayList<>();

        for (Compilation compilation : thread.getCompilations())
        {
            result.add(new QueueCounter(compilation.getStampTaskQueued(), true, compilation));
            result.add(new QueueCounter(compilation.getStampTaskCompilationStart(), false, compilation));
        }

        result.sort(Comparator.comparingLong(QueueCounter::getTimestamp));

        int tempQueueLength = 0;

        for (QueueCounter counter : result)
        {
            if (counter.isAdd())
            {
                tempQueueLength++;
            }
            else
            {
                tempQueueLength--;
            }
            maxQueueLength = Math.max(tempQueueLength, maxQueueLength);
        }

        return result;
    }

    private void plotThreadHeader(Graphics g, CompilerThread thread, double y, double rowHeight)
    {
        switch (plotMode)
        {
            case NATIVE_SIZE:
            case BYTECODE_SIZE:
            case EXPANSIONS:
                plotHorizontalLine(g, y);
                plotThreadName(g, getCompilerThreadName(thread), y);
                break;
            case TIMINGS:
                double yQueued = y - rowHeight * 0.25;
                double yCompiled = y + rowHeight * 0.25;

                plotHorizontalLine(g, yQueued);
                plotHorizontalLine(g, yCompiled);

                plotThreadName(g, getCompilerThreadName(thread) + "-Q", yQueued);
                plotThreadName(g, getCompilerThreadName(thread) + "-C", yCompiled);
                break;
            case QUEUE_LENGTH:
                plotHorizontalLine(g, y + rowHeight / 2);
                plotThreadName(g, getCompilerThreadName(thread), y + rowHeight / 2);
                break;
        }
    }

    private double getScaledTimestampX(long timestamp)
    {
        return getXOffset() + ((timestamp - minTime) / (double) timeRange) * panePlotWidth;
    }

    private String getCompilerThreadName(CompilerThread thread)
    {
        String threadName = thread.getThreadName();
        return (threadName != null && threadName.length() >= 3) ? threadName.substring(0, 3) : "??";
    }

    private void plotHorizontalLine(Graphics g, double y)
    {
        g.setColor(Color.DARK_GRAY);
        g.drawLine((int) getXOffset(), (int) y, (int) paneWidth, (int) y);
    }

    private void plotThreadName(Graphics g, String name, double y)
    {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));  // Set font if needed
        g.drawString(name, 4, (int) y);
    }

    private void plotNativeSize(Graphics2D g, Compilation compilation, double y, double rowHeight, Color fillColour,
                                boolean isCompilationOfSelectedMember)
    {
        long stampCompilationStart = compilation.getStampTaskCompilationStart();
        long stampNMethodEmitted = compilation.getStampNMethodEmitted();

        int nativeSize = compilation.getNativeSize();

        double x1 = getScaledTimestampX(stampCompilationStart);
        double x2 = getScaledTimestampX(stampNMethodEmitted);

        double nativeSizeHeight = (double) nativeSize / (double) maxNativeSize * rowHeight;

        int ix = (int) Math.round(x1);
        int iy = (int) Math.round(y - nativeSizeHeight / 2);
        int iw = (int) Math.round(x2 - x1);
        int ih = (int) Math.round(nativeSizeHeight);

        g.setColor(fillColour);
        g.fillRect(ix, iy, iw, ih);

        addMouseListenerForCompilation(ix, iy, iw, ih, compilation);

        if (isCompilationOfSelectedMember)
        {
            plotMarker(g, x1, y + rowHeight / 2, compilation);
        }
    }

    private void plotExpansions(Graphics2D g, Compilation compilation, double y, double rowHeight, Color fillColour,
                                boolean isCompilationOfSelectedMember)
    {
        long stampCompilationStart = compilation.getStampTaskCompilationStart();
        long stampNMethodEmitted = compilation.getStampNMethodEmitted();

        int nativeSize = compilation.getNativeSize();
        int bytecodeSize = compilation.getBytecodeSize();

        double x1 = getScaledTimestampX(stampCompilationStart);
        double x2 = getScaledTimestampX(stampNMethodEmitted);

        double maxBytes = Math.max(maxBytecodeSize, maxNativeSize);
        double bytecodeSizeHeight = bytecodeSize / maxBytes * rowHeight;
        double nativeSizeHeight = nativeSize / maxBytes * rowHeight;

        int[] xPoints = {(int) x1, (int) x1, (int) x2, (int) x2};
        int[] yPoints = {(int) (y - bytecodeSizeHeight / 2), (int) (y + bytecodeSizeHeight / 2),
                (int) (y + nativeSizeHeight / 2), (int) (y - nativeSizeHeight / 2)};

        g.setColor(fillColour);
        g.fillPolygon(xPoints, yPoints, 4);

        // TODO:
        //addMouseListenerForPolygon(xPoints, yPoints, 4, compilation);

        if (isCompilationOfSelectedMember)
        {
            plotMarker(g, x1, y + rowHeight / 2, compilation);
        }
    }

    private void plotBytecodeSize(Graphics2D g, Compilation compilation, double y, double rowHeight, Color fillColour,
                                  boolean isCompilationOfSelectedMember)
    {
        long stampCompilationStart = compilation.getStampTaskCompilationStart();
        long stampNMethodEmitted = compilation.getStampNMethodEmitted();

        int bytecodeSize = compilation.getBytecodeSize();

        double x = getScaledTimestampX(stampCompilationStart);
        double w = getScaledTimestampX(stampNMethodEmitted) - x;

        double bytecodeSizeHeight = (double) bytecodeSize / (double) maxBytecodeSize * rowHeight;

        // Set the color and draw the rectangle
        g.setColor(fillColour);

        int ix = (int) Math.round(x);
        int iy = (int) Math.round((y - bytecodeSizeHeight / 2));
        int iw = (int) Math.round(w);
        int ih = (int) Math.round(bytecodeSizeHeight);

        g.fillRect(ix, iy, iw, ih);

        addMouseListenerForCompilation(ix, iy, iw, ih, compilation);

        if (isCompilationOfSelectedMember)
        {
            plotMarker(g, x, y + rowHeight / 2, compilation);
        }
    }


    private void plotQueuedCompileTimes(Graphics2D g, Compilation compilation, double y, double rowHeight, Color fillColour,
                                        boolean isCompilationOfSelectedMember)
    {
        int nativeSize = compilation.getNativeSize();

        double xQueued = getScaledTimestampX(compilation.getStampTaskQueued());
        double xCompileStart = getScaledTimestampX(compilation.getStampTaskCompilationStart());
        double xNMethodEmitted = getScaledTimestampX(compilation.getStampNMethodEmitted());

        double yQueued = y - rowHeight * 0.25;
        double yCompiled = y + rowHeight * 0.25;

        double halfRowHeight = rowHeight / 2;
        double nativeSizeHeight = (double) nativeSize / (double) maxNativeSize * halfRowHeight;

        Color joinColor = compilation.isFailed() ? Color.RED : fillColour;

        g.setColor(joinColor);
        g.drawLine((int) xQueued, (int) yQueued, (int) xCompileStart, (int) (yCompiled + nativeSizeHeight / 2));

        g.setColor(joinColor);
        g.fillOval((int) xQueued - 4, (int) yQueued - 4, 8, 8);  // Circle with radius 4

        // TODO:
        // addMouseListenerForCircle((int) xQueued - 4, (int) yQueued - 4, 8, 8, compilation);


        if (!compilation.isFailed())
        {
            g.setColor(fillColour);

            int ix = (int) Math.round(xCompileStart);
            int iy = (int) Math.round((yCompiled - nativeSizeHeight / 2));
            int iw = (int) Math.round((xNMethodEmitted - xCompileStart));
            int ih = (int) Math.ceil(nativeSizeHeight);

            g.fillRect(ix, iy, iw, ih);

            addMouseListenerForCompilation(ix, iy, iw, ih, compilation);
        }


        if (isCompilationOfSelectedMember)
        {
            plotMarker(g, xCompileStart, y + rowHeight / 2, compilation);
        }
    }

    private JPanel getContextualControlsTimings()
    {
        JPanel hBox = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JCheckBox cbOnlyFailures = new JCheckBox("Only failed compilations");
        cbOnlyFailures.setSelected(showOnlyFailedCompiles);

        cbOnlyFailures.addItemListener(e ->
        {
            showOnlyFailedCompiles = cbOnlyFailures.isSelected();
            repaint();
        });

        hBox.add(cbOnlyFailures);

        return hBox;
    }

    private JPanel getContextualControlsQueueLength()
    {
        return new JPanel();
    }

    private JPanel buildModeHBox()
    {
        JRadioButton rbNativeSize = new JRadioButton("Native Sizes");
        JRadioButton rbBytecodeSize = new JRadioButton("Bytecode Sizes");
        JRadioButton rbExpansions = new JRadioButton("Expansions");
        JRadioButton rbTimings = new JRadioButton("Timings");
        JRadioButton rbQueueLength = new JRadioButton("Compiler Queues");

        ButtonGroup groupMode = new ButtonGroup();
        groupMode.add(rbNativeSize);
        groupMode.add(rbBytecodeSize);
        groupMode.add(rbExpansions);
        groupMode.add(rbTimings);
        groupMode.add(rbQueueLength);

        rbNativeSize.setSelected(true);

        contextualControls = new JPanel();

        rbNativeSize.addActionListener(e ->
        {
            plotMode = PlotMode.NATIVE_SIZE;
            contextualControls.removeAll();
            repaint();
        });

        rbBytecodeSize.addActionListener(e ->
        {
            plotMode = PlotMode.BYTECODE_SIZE;
            contextualControls.removeAll();
            repaint();
        });

        rbExpansions.addActionListener(e ->
        {
            plotMode = PlotMode.EXPANSIONS;
            contextualControls.removeAll();
            repaint();
        });

        rbTimings.addActionListener(e ->
        {
            plotMode = PlotMode.TIMINGS;
            contextualControls.removeAll();
            contextualControls.add(getContextualControlsTimings());
            repaint();
        });

        rbQueueLength.addActionListener(e ->
        {
            plotMode = PlotMode.QUEUE_LENGTH;
            contextualControls.removeAll();
            contextualControls.add(getContextualControlsQueueLength());
            repaint();
        });

        JPanel hBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        hBox.add(rbNativeSize);
        hBox.add(rbBytecodeSize);
        hBox.add(rbExpansions);
        hBox.add(rbTimings);
        hBox.add(rbQueueLength);

        return hBox;
    }

}
