/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.nmethod.codecache;

import org.adoptopenjdk.jitwatch.compilation.codecache.CodeCacheWalkerResult;
import org.adoptopenjdk.jitwatch.model.CodeCacheEvent;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;
import org.adoptopenjdk.jitwatch.ui.nmethod.AbstractNMethodPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class CodeCacheLayoutPanel extends AbstractNMethodPanel
{
    private CodeCacheWalkerResult codeCacheData;

    private long lowAddress;
    private long highAddress;
    private long addressRange;

    private double width;
    private double height;

    private JLabel lblNMethodCount;
    private JLabel lblLowAddress;
    private JLabel lblHighAddress;
    private JLabel lblAddressRange;

    private JButton btnAnimate;

    private JCheckBox checkC1;
    private JCheckBox checkC2;

    private boolean drawC1 = true;
    private boolean drawC2 = true;

    private JTextField txtAnimationSeconds;

    private Timer timer;
    private int currentEvent = 0;
    private long lastHandledAt = 0;

    private static final Color NOT_LATEST_COMPILATION = new Color(96, 0, 0);
    private static final Color LATEST_COMPILATION = COLOR_UNSELECTED_COMPILATION;

    public CodeCacheLayoutPanel(JITWatchUI parent)
    {
        super(parent);
    }

    public String getTitle()
    {
       return "CC Layout";
    }

    @Override
    protected JPanel buildControls()
    {
        JPanel vBoxControls = new JPanel();
        vBoxControls.setLayout(new BoxLayout(vBoxControls, BoxLayout.Y_AXIS)); // VBox equivalent

        vBoxControls.add(buildControlButtons());
        vBoxControls.add(buildControlInfo(), BorderLayout.SOUTH);

        return vBoxControls;
    }

    private JPanel buildControlButtons()
    {
        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
        panelButtons.setBorder(new EmptyBorder(4, 4, 0, 8));

        JButton btnZoomIn = new JButton("Zoom In");
        JButton btnZoomOut = new JButton("Zoom Out");
        JButton btnZoomReset = new JButton("Reset");
        btnAnimate = new JButton("Animate");

        checkC1 = new JCheckBox("Show C1");
        checkC1.setSelected(drawC1);
        checkC1.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                drawC1 = checkC1.isSelected();
                repaint();
            }
        });

        checkC2 = new JCheckBox("Show C2");
        checkC2.setSelected(drawC2);
        checkC2.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                drawC2 = checkC2.isSelected();
                repaint();
            }
        });

        txtAnimationSeconds = new JTextField("5", 5);
        txtAnimationSeconds.setMaximumSize(new Dimension(60, 25));
        txtAnimationSeconds.setEditable(false); // Assuming "readonly-label"

        btnZoomIn.setPreferredSize(new Dimension(80, 25));
        btnZoomOut.setPreferredSize(new Dimension(80, 25));
        btnZoomReset.setPreferredSize(new Dimension(80, 25));
        btnAnimate.setPreferredSize(new Dimension(80, 25));

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

        btnAnimate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    double animateOverSeconds = Double.parseDouble(txtAnimationSeconds.getText());
                    animate(animateOverSeconds);
                }
                catch (NumberFormatException nfe)
                {
                    // Handle exception (e.g., show an error message)
                }
            }
        });

        Component spacer = Box.createHorizontalGlue();

        panelButtons.add(checkC1);
        panelButtons.add(Box.createHorizontalStrut(10));
        panelButtons.add(checkC2);
        panelButtons.add(Box.createHorizontalStrut(20));
        panelButtons.add(btnZoomIn);
        panelButtons.add(Box.createHorizontalStrut(5));
        panelButtons.add(btnZoomOut);
        panelButtons.add(Box.createHorizontalStrut(5));
        panelButtons.add(btnZoomReset);
        panelButtons.add(Box.createHorizontalStrut(5));
        panelButtons.add(btnAnimate);
        panelButtons.add(Box.createHorizontalStrut(5));
        panelButtons.add(txtAnimationSeconds);
        panelButtons.add(spacer);

        return panelButtons;
    }

    private JPanel buildControlInfo()
    {
        JPanel panelInfo = new JPanel();
        panelInfo.setLayout(new BoxLayout(panelInfo, BoxLayout.X_AXIS));
        panelInfo.setBorder(new EmptyBorder(12, 8, 0, 8));

        int addressLabelWidth = 128;

        lblNMethodCount = new JLabel();
        lblNMethodCount.setPreferredSize(new Dimension(addressLabelWidth, 25));
        // lblNMethodCount.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // Simulating "readonly-label"

        lblLowAddress = new JLabel();
        lblLowAddress.setPreferredSize(new Dimension(addressLabelWidth, 25));
        // lblLowAddress.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        lblHighAddress = new JLabel();
        lblHighAddress.setPreferredSize(new Dimension(addressLabelWidth, 25));
        // lblHighAddress.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        lblAddressRange = new JLabel();
        lblAddressRange.setPreferredSize(new Dimension(addressLabelWidth, 25));
        //lblAddressRange.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        Component spacer = Box.createHorizontalGlue();

        panelInfo.add(new JLabel("NMethods: "));
        panelInfo.add(lblNMethodCount);
        panelInfo.add(Box.createHorizontalStrut(20));
        panelInfo.add(new JLabel("Lowest Address: "));
        panelInfo.add(lblLowAddress);
        panelInfo.add(Box.createHorizontalStrut(20));
        panelInfo.add(new JLabel("Highest Address: "));
        panelInfo.add(lblHighAddress);
        panelInfo.add(Box.createHorizontalStrut(20));
        panelInfo.add(new JLabel("Address Range Size: "));
        panelInfo.add(lblAddressRange);
        panelInfo.add(spacer);

        return panelInfo;
    }

    private boolean preDraw()
    {
        clear();

        boolean ok = false;

        lblNMethodCount.setText("");
        lblLowAddress.setText("");
        lblHighAddress.setText("");
        lblAddressRange.setText("");

        codeCacheData = parent.getCodeCacheWalkerResult();

        if (codeCacheData != null && !codeCacheData.getEvents().isEmpty())
        {
            lowAddress = codeCacheData.getLowestAddress();
            highAddress = codeCacheData.getHighestAddress();

            addressRange = highAddress - lowAddress;
            addressRange *= 1.01;

            width = scrollPane.getViewport().getWidth() * zoom;
            height = pane.getHeight();

            pane.setPreferredSize(new Dimension((int) width, (int) height));
            pane.revalidate();

            int eventCount = codeCacheData.getEvents().size();

            lblNMethodCount.setText(Integer.toString(eventCount));

            lblLowAddress.setText("0x" + Long.toHexString(lowAddress));
            lblHighAddress.setText("0x" + Long.toHexString(highAddress));
            lblAddressRange.setText(NumberFormat.getNumberInstance().format(highAddress - lowAddress));

            ok = true;
        }

        return ok;
    }

    @Override
    protected void paintGraph(Graphics g)
    {
        super.paintGraph(g);

        if (!preDraw())
        {
            return;
        }

        // TimerUtil.timerStart(getClass().getName() + ".redraw()");

        IMetaMember selectedMember = parent.getSelectedMember();

        if (selectedMember == null)
        {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Compilation selectedCompilation = selectedMember.getSelectedCompilation();

        List<CodeCacheEvent> eventsOfSelectedMember = new ArrayList<>();

        Color fillColour;

        double paneHeight = pane.getHeight();

        for (CodeCacheEvent event : codeCacheData.getEvents())
        {
            if (!showEvent(event))
            {
                continue;
            }

            final Compilation eventCompilation = event.getCompilation();

            final IMetaMember compilationMember = eventCompilation.getMember();

            if (eventCompilation != null)
            {
                if (selectedMember.equals(compilationMember))
                {
                    eventsOfSelectedMember.add(event);
                }
                else
                {
                    double scaledSize = (double) event.getNativeCodeSize() / (double) addressRange;

                    double w = scaledSize * width;

                    if (w > 0.0)
                    {
                        long addressOffset = event.getNativeAddress() - lowAddress;

                        double scaledAddress = (double) addressOffset / (double) addressRange;

                        int latestCompilationIndex = compilationMember.getCompilations().size() - 1;

                        if (eventCompilation.getIndex() == latestCompilationIndex)
                        {
                            fillColour = LATEST_COMPILATION;
                        }
                        else
                        {
                            fillColour = NOT_LATEST_COMPILATION;
                        }

                        double x = scaledAddress * width;
                        double y = 0;
                        plotCompilation(g2d, x, y, w, paneHeight, fillColour, eventCompilation, true);
                    }
                }
            }
        }

        for (CodeCacheEvent event : eventsOfSelectedMember)
        {
            long addressOffset = event.getNativeAddress() - lowAddress;

            double scaledAddress = (double) addressOffset / (double) addressRange;

            double scaledSize = (double) event.getNativeCodeSize() / (double) addressRange;

            double x = scaledAddress * width;
            double y = 0;
            double w = scaledSize * width;

            final Compilation eventCompilation = event.getCompilation();

            if (event.getCompilation().equals(selectedCompilation))
            {
                fillColour = COLOR_SELECTED_COMPILATION;
            }
            else
            {
                fillColour = COLOR_OTHER_MEMBER_COMPILATIONS;
            }

            plotCompilation(g2d, x, y, w, paneHeight, fillColour, eventCompilation, true);

            plotMarker(g, x, paneHeight, eventCompilation);
        }

        pane.repaint();
    }

    private boolean showEvent(CodeCacheEvent event)
    {
        boolean result = true;

        int level = event.getCompilationLevel();

        if (!drawC1 && level >= 1 && level <= 3)
        {
            result = false;
        }

        if (!drawC2 && level == 4)
        {
            result = false;
        }

        return result;
    }

    private void plotCompilation(Graphics2D g2d, double x, double y, double w, double h, Color fillColour, Compilation compilation,
                                 boolean clickHandler)
    {
        int ix = (int) x;
        int iy = (int) y;
        int iw = (int) w;
        int ih = (int) h;

        if (iw == 0)
        {
            iw = 1;
        }

        g2d.setColor(fillColour);
        g2d.fillRect(ix, iy, iw, ih);

        g2d.fill(new Rectangle2D.Double(x, y, w, h));

        if (clickHandler)
        {
            addMouseListenerForCompilation(ix, iy, iw, ih, compilation);
        }
    }

    private void animate(double targetSeconds)
    {
        if (!preDraw())
        {
            return;
        }

        final List<CodeCacheEvent> events = codeCacheData.getEvents();

        final int eventCount = events.size();

        double framesPerSecond = 60;
        double frameCount = targetSeconds * framesPerSecond;
        final double eventsPerFrame = eventCount / frameCount;
        final double secondsPerEvent = targetSeconds / eventCount;
        final double nanoSecondsPerEvent = 1_000_000_000 * secondsPerEvent;

        int delay = (int) (1000 / framesPerSecond);

        btnAnimate.setEnabled(false);

        timer = new Timer(delay, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                double realEventsPerFrame = eventsPerFrame;

                if (eventsPerFrame < 1.0)
                {
                    realEventsPerFrame = 1;

                    long now = System.nanoTime();
                    if (now - lastHandledAt < nanoSecondsPerEvent)
                    {
                        return;
                    }
                    lastHandledAt = now;
                }

                for (int i = 0; i < realEventsPerFrame; i++)
                {
                    if (currentEvent >= eventCount)
                    {
                        stopAnimation(); // Stop the timer when events are done
                        return;
                    }

                    CodeCacheEvent event = events.get(currentEvent++);
                    if (!showEvent(event))
                    {
                        continue;
                    }

                    Compilation eventCompilation = event.getCompilation();
                    IMetaMember compilationMember = eventCompilation.getMember();

                    if (eventCompilation != null)
                    {
                        long addressOffset = event.getNativeAddress() - lowAddress;
                        double scaledAddress = (double) addressOffset / (double) addressRange;
                        double scaledSize = (double) event.getNativeCodeSize() / (double) addressRange;

                        int latestCompilationIndex = compilationMember.getCompilations().size() - 1;
                        Color fillColour = (eventCompilation.getIndex() == latestCompilationIndex) ? LATEST_COMPILATION : NOT_LATEST_COMPILATION;

                        double x = scaledAddress * width;
                        double y = 0;
                        double w = scaledSize * width;
                        double h = height;

                        // Paint the compilation event onto the canvas
                        // plotCompilation(x, y, w, h, fillColour, eventCompilation, false);
                    }
                }

                //
                // repaint();
            }
        });

        timer.start(); // Start the animation
    }

    private void stopAnimation()
    {
        if (timer != null)
        {
            timer.stop();
        }
        btnAnimate.setEnabled(true);
        repaint();
    }
}

