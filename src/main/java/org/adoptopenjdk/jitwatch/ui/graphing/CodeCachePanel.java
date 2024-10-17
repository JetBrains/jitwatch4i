/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.graphing;

import org.adoptopenjdk.jitwatch.model.CodeCacheEvent;
import org.adoptopenjdk.jitwatch.model.Tag;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CodeCachePanel extends AbstractGraphPanel
{
    private boolean labelLeft = true;

    public CodeCachePanel(JITWatchUI parent)
    {
        super(parent, true);
    }

    public String getTitle()
    {
        return "Free CC";
    }

    @Override
    protected void paintComponent(Graphics g)
    {
       paintGraph(g);
    }

    @Override
    protected void paintGraph(Graphics g)
    {
        super.paintGraph(g);

        Graphics2D g2d = (Graphics2D) g;

        labelLeft = true;

        List<CodeCacheEvent> codeCacheEvents = mainUI.getJITDataModel().getCodeCacheEvents();

        Collections.sort(codeCacheEvents, Comparator.comparingLong(CodeCacheEvent::getStamp));

        if (!codeCacheEvents.isEmpty())
        {
            CodeCacheEvent firstEvent = codeCacheEvents.get(0);
            minX = firstEvent.getStamp();

            double firstNonZeroY = 0;

            Tag endOfLogTag = mainUI.getJITDataModel().getEndOfLogTag();

            if (endOfLogTag != null)
            {
                maxX = getStampFromTag(endOfLogTag);
            }
            else
            {
                CodeCacheEvent lastEvent = codeCacheEvents.get(codeCacheEvents.size() - 1);
                maxX = lastEvent.getStamp();
            }

            minY = firstEvent.getFreeCodeCache();
            maxY = firstEvent.getFreeCodeCache();

            if (minY != 0)
            {
                firstNonZeroY = minY;
            }

            // Find ranges
            for (CodeCacheEvent event : codeCacheEvents)
            {
                long freeCodeCache = event.getFreeCodeCache();
                if (freeCodeCache > 0)
                {
                    if (minY == 0)
                    {
                        minY = freeCodeCache;
                        firstNonZeroY = minY;
                    }
                    if (freeCodeCache > maxY)
                    {
                        maxY = freeCodeCache;
                    }
                    else if (freeCodeCache < minY)
                    {
                        minY = freeCodeCache;
                    }
                }
            }

            drawAxes(g2d);

            double lastCX = graphGapLeft + normaliseX(minX);
            double lastCY = graphGapTop + normaliseY(firstNonZeroY);

            Color colourLine = Color.BLUE;
            float lineWidth = 2.0f;

            for (CodeCacheEvent event : codeCacheEvents)
            {
                long stamp = event.getStamp();

                double x = graphGapLeft + normaliseX(stamp);
                double y = lastCY;

                switch (event.getEventType())
                {
                    case COMPILATION:
                        y = addToGraph(g2d, lastCX, lastCY, colourLine, lineWidth, event, x);
                        lastCX = x;
                        lastCY = y;
                        break;

                    case SWEEPER:
                        showLabel(g2d, "Sweep", Color.WHITE, x, y);
                        break;

                    case CACHE_FULL:
                        showLabel(g2d, "Code Cache Full", Color.RED, x, y);
                        break;
                }
            }

            continueLineToEndOfXAxis(g2d, lastCX, lastCY, colourLine, lineWidth);
        }
        else
        {
            g2d.drawString("No code cache information in log", 10, 10);
        }
    }

    private void baseRedraw(Graphics2D g2d)
    {
        // Drawing the background and chart area
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(210, 255, 255));
        g2d.fillRect((int) graphGapLeft, (int) graphGapTop, getWidth() - 40, getHeight() - 80);

        g2d.setColor(Color.BLACK);
        g2d.drawRect((int) graphGapLeft, (int) graphGapTop, getWidth() - 40, getHeight() - 80);
    }

    private double addToGraph(Graphics2D g2d, double lastCX, double lastCY, Color colourLine, float lineWidth, CodeCacheEvent event, double x)
    {
        long freeCodeCache = event.getFreeCodeCache();
        double y = graphGapTop + normaliseY(freeCodeCache);

        g2d.setColor(colourLine);
        g2d.setStroke(new BasicStroke(lineWidth));

        g2d.drawLine((int) lastCX, (int) lastCY, (int) x, (int) y);
        return y;
    }

    private void showLabel(Graphics2D g2d, String text, Color background, double x, double y)
    {
        double labelX;
        double labelY;

        if (labelLeft)
        {
            labelX = x - getApproximateStringWidth(g2d, text) - 16;
            labelY = Math.min(y - getStringHeight(g2d), graphGapTop + chartHeight - 32);
        }
        else
        {
            labelX = x + 8;
            labelY = Math.min(y, graphGapTop + chartHeight - 32);
        }

        drawLabel(g2d, text, labelX, labelY, background);

        labelLeft = !labelLeft;
    }
}
