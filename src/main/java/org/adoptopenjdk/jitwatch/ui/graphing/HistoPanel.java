/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.graphing;

import com.intellij.openapi.ui.ComboBox;
import org.adoptopenjdk.jitwatch.histo.*;
import org.adoptopenjdk.jitwatch.model.IReadOnlyJITDataModel;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoPanel extends JPanel
{
    private Histo histo;
    private IHistoVisitable histoVisitable;
    private JComboBox<String> comboBox;
    private HistoGraphPanel graphPanel;

    public HistoPanel(JITWatchUI parent)
    {
        IReadOnlyJITDataModel model = parent.getJITDataModel();
        final Map<String, IHistoVisitable> attrMap = new HashMap<>();

        attrMap.put("JIT Compilation Times", new CompileTimeHistoWalker(model, 1));
        attrMap.put("Bytes per Compiled Method", new AttributeNameHistoWalker(model, true, "ATTR_BYTES", 1));
        attrMap.put("Native Bytes per Compiled Method", new NativeSizeHistoWalker(model, 1));
        attrMap.put("Inlined Method Sizes", new InlineSizeHistoVisitable(model, 1));

        setLayout(new BorderLayout());

        comboBox = new ComboBox<>(attrMap.keySet().toArray(new String[0]));
        comboBox.setSelectedIndex(0);

        histoVisitable = attrMap.get(comboBox.getSelectedItem());
        histo = histoVisitable.buildHistogram();

        comboBox.addActionListener(e ->
        {
            String selected = (String) comboBox.getSelectedItem();
            histoVisitable = attrMap.get(selected);
            histo = histoVisitable.buildHistogram();
            repaint();
        });

        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        //comboPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        comboPanel.add(comboBox);

        add(comboPanel, BorderLayout.NORTH);

        graphPanel = new HistoGraphPanel(parent);
        add(graphPanel, BorderLayout.CENTER);
    }

    public String getTitle()
    {
        return "Histo";
    }

    public class HistoGraphPanel extends AbstractGraphPanel
    {
        public HistoGraphPanel(JITWatchUI parent)
        {
            super(parent, false);
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

            if (histo == null)
            {
                return;
            }

            List<Map.Entry<Long, Integer>> result = histo.getSortedData();

            if (result.size() > 0)
            {
                minX = 0;
                maxX = histo.getLastTime();

                minY = 0;
                maxY = histo.getMaxCount();

                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));

                drawAxes(g2d);

                Color colourLine = Color.RED;

                for (Map.Entry<Long, Integer> entry : result)
                {
                    long key = entry.getKey();
                    int value = entry.getValue();

                    double x = graphGapLeft + normaliseX(key);
                    double y = graphGapTop + normaliseY(value);

                    g2d.setColor(colourLine);
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.drawLine((int) x, (int) (graphGapTop + chartHeight), (int) x, (int) y);
                }

                double legendWidth = 100;
                double legendHeight = 220;
                double xPos = getWidth() - graphGapRight - legendWidth - 5;
                double yPos = graphGapTop + 5;

                g2d.setColor(Color.WHITE);
                g2d.fillRect((int) xPos, (int) yPos, (int) legendWidth, (int) legendHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect((int) xPos, (int) yPos, (int) legendWidth, (int) legendHeight);

                xPos += 5;
                yPos += 5;

                for (double percent : new double[]{50, 90, 95, 99, 99.9, 99.99, 99.999, 100})
                {
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(percent + "% : " + histo.getPercentile(percent), (int) xPos, (int) yPos);
                    yPos += 20;
                }
            }
            else
            {
                g2d.setColor(Color.BLACK);
                g2d.drawString("No data for histogram.", (int) (graphGapLeft + 8), (int) (graphGapTop + 16));
            }
        }
    }
}
