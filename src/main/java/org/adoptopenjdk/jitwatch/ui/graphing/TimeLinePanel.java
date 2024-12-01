/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.graphing;

import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.JITEvent;
import org.adoptopenjdk.jitwatch.model.JITStats;
import org.adoptopenjdk.jitwatch.model.Tag;
import org.adoptopenjdk.jitwatch.util.ParseUtil;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.*;

public class TimeLinePanel extends AbstractGraphPanel
{
    private IMetaMember selectedMember = null;
    private int compilationIndex = 0;
    private static final int MARKET_DIAMETER = 10;
    private boolean labelLeft = true;
    private boolean drawnQueueEvent = false;

    public TimeLinePanel(JITWatchUI mainUI)
    {
        super(mainUI, true);
        this.mainUI = mainUI;
    }

    public String getTitle()
    {
        return "Timeline";
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        paintGraph(g);
    }

    @Override
    protected void paintGraph(Graphics g)
    {
        labelLeft = true;

        super.paintGraph(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));

        if (selectedMember != mainUI.getSelectedMember())
        {
            selectedMember = mainUI.getSelectedMember();
        }

        List<JITEvent> events = mainUI.getJITDataModel().getEventListCopy();
        compilationIndex = 0;

        if (!events.isEmpty())
        {
            events.sort(Comparator.comparingLong(JITEvent::getStamp));

            JITEvent firstEvent = events.get(0);
            minX = firstEvent.getStamp();

            JITEvent lastEvent = events.get(events.size() - 1);
            Tag endOfLogTag = mainUI.getJITDataModel().getEndOfLogTag();

            if (endOfLogTag != null)
            {
                maxX = getStampFromTag(endOfLogTag);
                maxX = Math.min(maxX, (long) (lastEvent.getStamp() * 1.1));
            }
            else
            {
                maxX = lastEvent.getStamp();
            }

            minY = 0;
            calculateMaxCompiles(events);
            drawAxes(g2d);
            drawEvents(g2d, events);
            showSelectedMemberLabel(g2d);
        }
        else
        {
            g2d.drawString("No compilation information processed", 10, 10);
        }
    }

    private void calculateMaxCompiles(List<JITEvent> events)
    {
        maxY = events.size();
    }

    private void drawMemberEvents(Graphics2D g2d, List<Compilation> compilations, long stamp, double yPos)
    {
        if (compilationIndex >= compilations.size())
        {
            return;
        }

        Compilation compilation = compilations.get(compilationIndex);

        if (!compilation.isC2N())
        {
            if (!drawnQueueEvent)
            {
                Tag tagTaskQueued = compilation.getTagTaskQueued();

                if (tagTaskQueued != null && compilation.getStampTaskQueued() == stamp)
                {
                    drawMemberEvent(g2d, compilation, tagTaskQueued, stamp, yPos);
                    drawnQueueEvent = true;
                }
            }

            Tag tagNMethod = compilation.getTagNMethod();

            if (tagNMethod != null && compilation.getStampTaskCompilationStart() == stamp)
            {
                drawMemberEvent(g2d, compilation, tagNMethod, stamp, yPos);
                compilationIndex++;
                drawnQueueEvent = false;
            }
        }
    }

    private void drawMemberEvent(Graphics2D g2d, Compilation compilation, Tag tag, long stamp, double yPos)
    {
        long journalEventTime = ParseUtil.getStamp(tag.getAttributes());

        g2d.setColor(Color.BLUE);
        double smX = graphGapLeft + normaliseX(journalEventTime);
        double blobX = smX - MARKET_DIAMETER / 2;
        double blobY = yPos - MARKET_DIAMETER / 2;

        g2d.fillOval((int) blobX, (int) blobY, MARKET_DIAMETER, MARKET_DIAMETER);

        String label = buildLabel(tag, journalEventTime, compilation);
        drawLabel(g2d, label, blobX, blobY);
    }

    private void drawLabel(Graphics2D g2d, String label, double blobX, double blobY)
    {
        FontMetrics metrics = g2d.getFontMetrics();
        int labelWidth = metrics.stringWidth(label);

        int labelX = labelLeft ? (int) (blobX - labelWidth - 16) : (int) (blobX + 16);
        int labelY = (int) Math.min(blobY, graphGapTop + chartHeight - 32);

        labelLeft = !labelLeft;

        g2d.setColor(Color.WHITE);
        g2d.drawString(label, labelX, labelY);
    }

    private void drawEvents(Graphics2D g2d, List<JITEvent> events)
    {
        Color colourTotal = Color.BLACK;
        float lineWidth = 2.0f;
        g2d.setStroke(new BasicStroke(lineWidth));

        int cumTotal = 0;
        double lastCX = graphGapLeft + normaliseX(minX);
        double lastCY = graphGapTop + normaliseY(0);

        showStatsLegend(g2d);

        for (JITEvent event : events)
        {
            long stamp = event.getStamp();
            cumTotal++;

            double x = graphGapLeft + normaliseX(stamp);
            double y = graphGapTop + normaliseY(cumTotal);

            if (selectedMember != null)
            {
                List<Compilation> compilations = selectedMember.getCompilations();
                if (!compilations.isEmpty())
                {
                    drawMemberEvents(g2d, compilations, stamp, y);
                }
            }

            g2d.setColor(colourTotal);
            g2d.drawLine((int) lastCX, (int) lastCY, (int) x, (int) y);

            lastCX = x;
            lastCY = y;
        }

        continueLineToEndOfXAxis(g2d, lastCX, lastCY, colourTotal, lineWidth);

        drawLevelGraph(g2d, 1, events, Color.BLUE, lineWidth);
        drawLevelGraph(g2d, 2, events, Color.RED, lineWidth);
        drawLevelGraph(g2d, 3, events, Color.MAGENTA, lineWidth);
        drawLevelGraph(g2d, 4, events, Color.GREEN, lineWidth);

    }

    private void drawLevelGraph(Graphics2D g2d, int level, List<JITEvent> events, Color color, float lineWidth)
    {
        int cumLevel = 0;

        double lastCX = graphGapLeft + normaliseX(minX);
        double lastCY = graphGapTop + normaliseY(0);

        g2d.setStroke(new BasicStroke(lineWidth));

        for (JITEvent event : events)
        {
            if (event.getLevel() == level)
            {
                cumLevel++;
            }

            long stamp = event.getStamp();

            double x = graphGapLeft + normaliseX(stamp);

            double y = graphGapTop + normaliseY(cumLevel);

            g2d.setColor(color);
            g2d.drawLine((int) lastCX, (int) lastCY, (int) x, (int) y);

            lastCX = x;
            lastCY = y;
        }

        continueLineToEndOfXAxis(g2d, lastCX, lastCY, color, lineWidth);
    }

    private void showSelectedMemberLabel(Graphics2D g2d)
    {
        if (selectedMember != null)
        {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.setColor(Color.WHITE);
            g2d.drawString(selectedMember.toString(), 56, 40);
        }
    }

    private String buildLabel(Tag nextJournalEvent, long journalEventTime, Compilation compilation)
    {
        StringBuilder labelBuilder = new StringBuilder();
        String tagName = nextJournalEvent.getName();

        if (TAG_TASK_QUEUED.equals(tagName))
        {
            labelBuilder.append("Queued");
        }
        else
        {
            Map<String, String> eventAttributes = nextJournalEvent.getAttributes();
            String compiler = eventAttributes.getOrDefault(ATTR_COMPILER, "Unknown");
            labelBuilder.append(compiler);

            String compileKind = eventAttributes.get(ATTR_COMPILE_KIND);
            if (compileKind != null)
            {
                labelBuilder.append(" (").append(compileKind.toUpperCase()).append(")");
            }

            String level = eventAttributes.get(ATTR_LEVEL);
            if (level != null)
            {
                labelBuilder.append(" (Level ").append(level).append(")");
            }

            if (!compilation.isC2N())
            {
                long duration = compilation.getCompilationDuration();
                if (duration != 0)
                {
                    labelBuilder.append(" in ").append(duration).append("ms");
                }
            }
        }

        return labelBuilder.toString();
    }

    private void showStatsLegend(Graphics2D g2d)
    {
        JITStats stats = mainUI.getJITDataModel().getJITStats();
        String statsText = String.format(
                "Total Compilations: %d (L1[blue]: %d) (L2(red): %d) (L3(magenta): %d) (L4(green): %d)",
                stats.getTotalCompiledMethods(),
                stats.getCountLevel1(),
                stats.getCountLevel2(),
                stats.getCountLevel3(),
                stats.getCountLevel4()
        );

        g2d.drawString(statsText, (int) graphGapLeft,  (int) (graphGapTop+stdFontSize)/2);
    }
}
