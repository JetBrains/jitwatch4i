/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.graphing;

import org.adoptopenjdk.jitwatch.model.Tag;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;
import org.adoptopenjdk.jitwatch.util.ParseUtil;
import org.adoptopenjdk.jitwatch.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Map;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.ATTR_STAMP;

public abstract class AbstractGraphPanel extends JPanel
{
    protected JITWatchUI mainUI;

    protected static int stdFontSize = 12;

    protected double graphGapLeft = 20.5;
    protected final double graphGapRight = 20.5;
    protected final double graphGapTop = 20.5;

    protected static final int[] Y_SCALE = new int[21];

    protected double chartWidth;
    protected double chartHeight;

    protected long minX;
    protected long maxX;
    protected long minY;
    protected long maxY;

    protected long minXQ;
    protected long maxXQ;
    protected long minYQ;
    protected long maxYQ;

    protected double endOfXAxis;

    private boolean xAxisTime = false;

    protected static final Font STANDARD_FONT = new Font("JetBrains Mono", Font.PLAIN, stdFontSize);
    protected static final Font MEMBER_FONT = new Font("JetBrains Mono", Font.PLAIN, 24);

    static
    {
        int multiplier = 1;
        for (int i = 0; i < Y_SCALE.length; i += 3)
        {
            Y_SCALE[i + 0] = 1 * multiplier;
            Y_SCALE[i + 1] = 2 * multiplier;
            Y_SCALE[i + 2] = 5 * multiplier;
            multiplier *= 10;
        }
    }

    public AbstractGraphPanel(final JITWatchUI parent, boolean xAxisTime)
    {
        this.mainUI = parent;
        this.xAxisTime = xAxisTime;
    }

    protected void paintGraph(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setFont(STANDARD_FONT);

        chartWidth = getWidth() - graphGapLeft - graphGapRight;
        chartHeight = getHeight() - graphGapTop * 2;

        setStrokeForAxis(g2d);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(210, 255, 255));
        g2d.fillRect((int) graphGapLeft, (int) graphGapTop, (int) chartWidth, (int) chartHeight);

        g2d.setColor(Color.BLACK);
        g2d.drawRect((int) graphGapLeft, (int) graphGapTop, (int) chartWidth, (int) chartHeight);
    }

    protected void drawAxes(Graphics2D g2d)
    {
        if (xAxisTime)
        {
            drawXAxisTime(g2d);
        }
        else
        {
            drawXAxis(g2d);
        }
        drawYAxis(g2d);
    }

    protected long getStampFromTag(Tag tag)
    {
        Map<String, String> attrs = tag.getAttributes();
        return ParseUtil.parseStamp(attrs.get(ATTR_STAMP));
    }

    protected void continueLineToEndOfXAxis(Graphics2D g2d, double lastX, double lastY, Color color, float lineWidth)
    {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineWidth));
        g2d.draw(new Line2D.Double(lastX, lastY, endOfXAxis, lastY));
    }

    protected void drawLabel(Graphics2D g2d, String text, double xPos, double yPos, Color backgroundColour)
    {
        FontMetrics metrics = g2d.getFontMetrics();
        int boxWidth = metrics.stringWidth(text) + 8;
        int boxHeight = metrics.getHeight() + 8;

        g2d.setColor(backgroundColour);
        g2d.fillRect((int) xPos, (int) yPos, boxWidth, boxHeight);

        setStrokeForAxis(g2d);
        g2d.drawRect((int) xPos, (int) yPos, boxWidth, boxHeight);

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, (int) (xPos + 4), (int) (yPos + metrics.getAscent() + 4));
    }

    protected void drawXAxisTime(Graphics2D g2d)
    {
        long xInc = getXStepTime();

        minXQ = (minX / xInc) * xInc;
        maxXQ = (1 + (maxX / xInc)) * xInc;

        long gridX = minXQ;

        while (gridX <= maxX)
        {
            double x = graphGapLeft + normaliseX(gridX);

            setStrokeForAxis(g2d);
            g2d.draw(new Line2D.Double(x, graphGapTop, x, graphGapTop + chartHeight));

            boolean showMillis = maxX < 5000;
            g2d.drawString(StringUtil.formatTimestamp(gridX, showMillis), (float) x, (float) (graphGapTop + chartHeight + 15));

            gridX += xInc;
        }

        endOfXAxis = graphGapLeft + normaliseX(gridX);
    }

    private void drawXAxis(Graphics2D g2d)
    {
        long xInc = findScale(maxX - minX);

        minXQ = (minX / xInc) * xInc;
        maxXQ = (1 + (maxX / xInc)) * xInc;

        long gridX = minXQ;

        while (gridX <= maxX)
        {
            double x = graphGapLeft + normaliseX(gridX);

            setStrokeForAxis(g2d);
            g2d.draw(new Line2D.Double(x, graphGapTop, x, graphGapTop + chartHeight));

            g2d.drawString(StringUtil.formatThousands(Long.toString(gridX)), (float) x, (float) (graphGapTop + chartHeight + 15));

            gridX += xInc;
        }

        endOfXAxis = graphGapLeft + normaliseX(gridX);
    }

    private void drawYAxis(Graphics2D g2d)
    {
        long yInc = findScale(maxY - minY);

        minYQ = (minY / yInc) * yInc;
        maxYQ = (1 + (maxY / yInc)) * yInc;

        long gridY = minYQ;

        int maxYLabelWidth = StringUtil.formatThousands(Long.toString(maxYQ)).length();

        graphGapLeft = Math.max(40.5, maxYLabelWidth * 9);
        double yLabelX = graphGapLeft - (0.5 + maxYLabelWidth) * 8;

        while (gridY <= maxYQ)
        {
            if (gridY >= minYQ)
            {
                double y = graphGapTop + normaliseY(gridY);

                setStrokeForAxis(g2d);
                g2d.draw(new Line2D.Double(graphGapLeft, y, graphGapLeft + chartWidth, y));

                g2d.drawString(StringUtil.formatThousands(Long.toString(gridY)), (float) yLabelX, (float) (y + getStringHeight(g2d) / 2));
            }
            gridY += yInc;
        }
    }

    protected double getApproximateStringWidth(Graphics2D g2d, String text)
    {
        return g2d.getFontMetrics().stringWidth(text);
    }

    protected double getStringHeight(Graphics2D g2d)
    {
        return g2d.getFontMetrics().getHeight();
    }

    private long getXStepTime()
    {
        long rangeMillis = maxX - minX;
        int requiredLines = 5;

        long[] gapMillis = new long[]{
                30 * 24 * 60 * 60000L, 14 * 24 * 60 * 60000L, 7 * 24 * 60 * 60000L, 4 * 24 * 60 * 60000L,
                2 * 24 * 60 * 60000L, 24 * 60 * 60000L, 16 * 60 * 60000L, 12 * 60 * 60000L, 8 * 60 * 60000L,
                6 * 60 * 60000L, 4 * 60 * 60000L, 2 * 60 * 60000L, 60 * 60000L, 30 * 60000L, 15 * 60000L,
                10 * 60000L, 5 * 60000L, 2 * 60000L, 1 * 60000L, 30000L, 15000L, 10000L, 5000L, 2000L,
                1000L, 500L, 200L, 100L, 50L, 20L, 10L, 5L, 2L, 1L
        };

        long incrementMillis = 120 * 60000L;

        for (long gapMilli : gapMillis)
        {
            if (rangeMillis / gapMilli >= requiredLines)
            {
                incrementMillis = gapMilli;
                break;
            }
        }

        return incrementMillis;
    }

    protected long findScale(long range)
    {
        long requiredLines = 8;

        for (int scale : Y_SCALE)
        {
            if (range / scale < requiredLines)
            {
                return scale;
            }
        }

        return range / requiredLines;
    }

    protected double normaliseX(double value)
    {
        return normalise(value, minXQ, maxXQ, chartWidth, false);
    }

    protected double normaliseY(double value)
    {
        return normalise(value, minYQ, maxYQ, chartHeight, true);
    }

    protected double normalise(double value, double min, double max, double size, boolean invert)
    {
        double range = max - min;
        double result = 0;

        if (range != 0)
        {
            result = (value - min) / range;
        }

        result *= size;

        if (invert)
        {
            result = size - result;
        }

        return result;
    }

    protected void setStrokeForAxis(Graphics2D g2d)
    {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(0.5f));
    }

    protected void setStrokeForText(Graphics2D g2d)
    {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setFont(new Font("Arial", Font.PLAIN, stdFontSize));
    }
}