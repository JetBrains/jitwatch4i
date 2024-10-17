/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.compilechain;

import com.intellij.ui.components.JBScrollPane;
import org.adoptopenjdk.jitwatch.chain.CompileChainWalker;
import org.adoptopenjdk.jitwatch.chain.CompileNode;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.IReadOnlyJITDataModel;
import org.adoptopenjdk.jitwatch.ui.compilationchooser.CompilationChooser;
import org.adoptopenjdk.jitwatch.ui.main.*;
import org.adoptopenjdk.jitwatch.ui.resize.IRedrawable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

public class CompileChainPanel extends JPanel implements ICompilationChangeListener, IRedrawable
{
    private static final double X_OFFSET = 16;
    private static final double Y_OFFSET = 16;

    private static final double X_GAP = 25;
    private static final int STROKE_WIDTH = 3;
    private static final double RECT_HEIGHT = 25;
    private static final double RECT_Y_GAP = 16;

    private JBScrollPane scrollPane;
    private DrawingPane pane;
    private IStageAccessProxy stageAccess;
    private JLabel labelRootNodeMember;

    private CompilationChooser compilationChooser;

    private CompileNode rootNode;
    private double y;
    private IReadOnlyJITDataModel model;

    public CompileChainPanel(IMemberSelectedListener selectionListener, final IStageAccessProxy stageAccess, IReadOnlyJITDataModel model)
    {
        super();

        this.stageAccess = stageAccess;
        this.model = model;

        compilationChooser = new CompilationChooser(selectionListener);

        pane = new DrawingPane();
        scrollPane = new JBScrollPane(pane);

        JPanel vBoxStack = new JPanel();
        vBoxStack.setLayout(new BorderLayout());

        JPanel hBox = new JPanel();
        hBox.setLayout(new BoxLayout(hBox, BoxLayout.X_AXIS));

        labelRootNodeMember = new JLabel();

        hBox.add(labelRootNodeMember);
        hBox.add(Box.createHorizontalStrut(16));
        hBox.add(compilationChooser.getCombo());
        hBox.add(Box.createHorizontalStrut(16));
        hBox.add(Box.createHorizontalGlue()); // Spacer
        hBox.add(Box.createHorizontalStrut(16));

        hBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        vBoxStack.add(hBox, BorderLayout.NORTH);
        vBoxStack.add(scrollPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());

        add(vBoxStack, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                redraw();
            }
        });
    }
    public String getTitle()
    {
        return "Comp. Chain";
    }

    @Override
    public void compilationChanged(IMetaMember member)
    {
        compilationChooser.compilationChanged(member);

        if (member != null)
        {
            buildTree(member);
        }
        else
        {
            rootNode = null;
            redraw();
        }
    }

    private void clear()
    {
        y = Y_OFFSET;

        pane.clear();

        showKey();
    }

    @Override
    public void redraw()
    {
        if (rootNode != null)
        {
            clear();

            show(rootNode, X_OFFSET, Y_OFFSET, 0);

            if (rootNode.getChildren().isEmpty())
            {
                pane.addText("No method calls made by " + rootNode.getMemberName() + " were inlined or JIT compiled",
                        X_OFFSET, y, 1.0);
            }
            pane.repaint();
        }
    }

    private void showKey()
    {
        double keyX = scrollPane.getWidth() - 220;
        double keyY = 10;

        // Add a rectangle for the key
        pane.addRoundedRectangle(keyX - 20, keyY, 210, 180, 30, 30, Color.BLACK, Color.WHITE, 1.0f);

        keyY += 20;

        pane.addText("Key", keyX + 75, keyY, 1.0);

        keyY += 15;

        buildNode("Inlined", keyX, keyY, true, false, false);
        keyY += 35;

        buildNode("Compiled", keyX, keyY, false, true, false);
        keyY += 35;

        buildNode("Virtual Call", keyX, keyY, false, false, true);
        keyY += 35;

        buildNode("Not Compiled", keyX, keyY, false, false, false);
        keyY += 35;
    }

    private void show(CompileNode node, double x, double parentY, int depth)
    {
        double lastX = x;

        lastX = plotNode(node, x, parentY, depth);

        y += RECT_HEIGHT + STROKE_WIDTH + RECT_Y_GAP;

        parentY = y - RECT_Y_GAP;

        for (CompileNode child : node.getChildren())
        {
            show(child, lastX, parentY, depth + 1);
        }
    }

    private String getLabelText(CompileNode node)
    {
        String result = null;

        IMetaMember member = null;

        if (node == null)
        {
            result = "Unknown";
        }
        else
        {
            member = node.getMember();

            if (member == null)
            {
                result = "Unknown";
            }
            else if (member.isConstructor())
            {
                result = member.getMetaClass().getAbbreviatedFullyQualifiedName() + "()";
            }
            else
            {
                result = member.getAbbreviatedFullyQualifiedMemberName() + "()";
            }
        }

        return result;
    }

    private double plotNode(final CompileNode node, final double x, final double parentY, final int depth)
    {
        String labelText = getLabelText(node);

        NodeDrawable nodeDrawable = buildNode(labelText, x, y, node.isInlined(), node.isCompiled(), node.isVirtualCall());

        if (depth > 0)
        {
            double connectX = x - X_GAP;
            double connectY = y + RECT_HEIGHT / 2;
            double upLineY = y + RECT_HEIGHT / 2;

            pane.addLine(connectX, upLineY, connectX, parentY, Color.BLACK, STROKE_WIDTH);
            pane.addLine(connectX, connectY, x, connectY, Color.BLACK, STROKE_WIDTH);
        }

        double nextX = x + nodeDrawable.getWidth() / 2;

        nextX += X_GAP;

        pane.addDrawable(nodeDrawable);

        return nextX;
    }

    private NodeDrawable buildNode(String labelText, double x, double y, boolean inlined, boolean compiled, boolean virtualCall)
    {
        Font font = new Font("SansSerif", Font.PLAIN, 12);

        Color fillColor = getColourForCompilation(compiled, inlined, virtualCall);

        NodeDrawable nodeDrawable = new NodeDrawable(x, y, 0, RECT_HEIGHT, labelText, font, fillColor, Color.BLACK, STROKE_WIDTH);

        // Set the width based on text size
        FontMetrics fm = pane.getFontMetrics(font);
        int textWidth = fm.stringWidth(labelText);
        double rectWidth = textWidth + 20;
        nodeDrawable.setWidth(rectWidth);

        pane.addDrawable(nodeDrawable);

        return nodeDrawable;
    }

    private Color getColourForCompilation(boolean isCompiled, boolean isInlined, boolean isVirtual)
    {
        if (isInlined)
        {
            return Color.GREEN;
        }
        else if (isVirtual)
        {
            return Color.MAGENTA;
        }
        else if (isCompiled)
        {
            return Color.RED;
        }
        else
        {
            return Color.GRAY;
        }
    }

    private void buildTree(IMetaMember member)
    {
        Compilation selectedCompilation = member.getSelectedCompilation();

        String title = "Compile Chain: ";

        if (selectedCompilation != null)
        {
            CompileChainWalker walker = new CompileChainWalker(model);

            CompileNode root = walker.buildCallTree(selectedCompilation);

            this.rootNode = root;

            String rootMemberName = getLabelText(root);

            title += rootMemberName;

            if (root != null)
            {
                title += " " + root.getCompilation().getSignature();
            }

            // setTitle(title);

            labelRootNodeMember.setText(rootMemberName);
        }
        else
        {
            rootNode = null;

            labelRootNodeMember.setText("");

            clear();

            pane.addText(member.toString() + " was not JIT compiled", X_OFFSET, y, 1.0);
            pane.repaint();
        }

        redraw();
    }

    private class DrawingPane extends JPanel
    {
        private final java.util.List<Drawable> drawables = new ArrayList<>();

        public DrawingPane()
        {
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    Point2D p = e.getPoint();
                    for (Drawable d : drawables)
                    {
                        if (d instanceof NodeDrawable)
                        {
                            NodeDrawable nd = (NodeDrawable) d;
                            if (nd.contains(p))
                            {
                                // TODO:
                                // stageAccess.openTriView(nd.getNode().getMember());
                            }
                        }
                    }
                }
            });
        }

        public void clear()
        {
            drawables.clear();
        }

        public void addDrawable(Drawable d)
        {
            drawables.add(d);
        }

        public void addLine(double x1, double y1, double x2, double y2, Color color, double strokeWidth)
        {
            LineDrawable line = new LineDrawable(x1, y1, x2, y2, color, (float) strokeWidth);
            drawables.add(line);
        }

        public void addRoundedRectangle(double x, double y, double width, double height, double arcWidth, double arcHeight, Color strokeColor, Color fillColor, float strokeWidth)
        {
            RoundedRectangleDrawable rect = new RoundedRectangleDrawable(x, y, width, height, arcWidth, arcHeight, strokeColor, fillColor, strokeWidth);
            drawables.add(rect);
        }

        public void addText(String text, double x, double y, double strokeWidth)
        {
            TextDrawable td = new TextDrawable(text, x, y, new Font("SansSerif", Font.PLAIN, 12), Color.BLACK);
            drawables.add(td);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            for (Drawable d : drawables)
            {
                d.draw((Graphics2D) g);
            }
        }

        @Override
        public Dimension getPreferredSize()
        {
            Rectangle2D bounds = new Rectangle2D.Double();
            for (Drawable d : drawables)
            {
                Rectangle2D drawableBounds = d.getBoundingRect();
                if (drawableBounds != null)
                {
                    Rectangle2D.union(bounds, drawableBounds, bounds);
                }
            }

            Insets insets = getInsets();
            int width = (int) Math.ceil(bounds.getMaxX()) + insets.left + insets.right;
            int height = (int) Math.ceil(bounds.getMaxY()) + insets.top + insets.bottom;

            Container parent = getParent();
            Dimension viewportSize = parent.getSize();
            width = Math.max(width, viewportSize.width);
            height = Math.max(height, viewportSize.height);
            return new Dimension(width, height);
        }
    }

    private interface Drawable
    {
        void draw(Graphics2D g2d);
        Rectangle2D getBoundingRect();
    }

    private class LineDrawable implements Drawable
    {
        private Line2D.Double line;
        private Color color;
        private float strokeWidth;

        public LineDrawable(double x1, double y1, double x2, double y2, Color color, float strokeWidth)
        {
            this.line = new Line2D.Double(x1, y1, x2, y2);
            this.color = color;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void draw(Graphics2D g2d)
        {
            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.setColor(color);
            g2d.draw(line);
            g2d.setStroke(oldStroke);
        }

        @Override
        public Rectangle2D getBoundingRect()
        {
            return line.getBounds2D();
        }
    }

    private class RoundedRectangleDrawable implements Drawable
    {
        private RoundRectangle2D.Double rect;
        private Color strokeColor;
        private Color fillColor;
        private float strokeWidth;

        public RoundedRectangleDrawable(double x, double y, double width, double height, double arcWidth, double arcHeight, Color strokeColor, Color fillColor, float strokeWidth)
        {
            this.rect = new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight);
            this.strokeColor = strokeColor;
            this.fillColor = fillColor;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void draw(Graphics2D g2d)
        {
            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.setColor(fillColor);
            g2d.fill(rect);
            g2d.setColor(strokeColor);
            g2d.draw(rect);
            g2d.setStroke(oldStroke);
        }

        @Override
        public Rectangle2D getBoundingRect()
        {
            return rect.getBounds2D();
        }
    }

    private class TextDrawable implements Drawable
    {
        private String text;
        private double x;
        private double y;
        private Font font;
        private Color color;

        public TextDrawable(String text, double x, double y, Font font, Color color)
        {
            this.text = text;
            this.x = x;
            this.y = y;
            this.font = font;
            this.color = color;
        }

        @Override
        public void draw(Graphics2D g2d)
        {
            g2d.setFont(font);
            g2d.setColor(color);
            g2d.drawString(text, (float) x, (float) y);
        }

        @Override
        public Rectangle2D getBoundingRect()
        {
            return null;
        }
    }

    private class NodeDrawable implements Drawable
    {
        private Rectangle2D.Double rect;
        private String text;
        private Font font;
        private Color textColor;
        private Color fillColor;
        private Color strokeColor;
        private float strokeWidth;

        public NodeDrawable(double x, double y, double width, double height, String text, Font font, Color fillColor,
                            Color strokeColor, float strokeWidth)
        {
            this.rect = new Rectangle2D.Double(x, y, width, height);
            this.text = text;
            this.font = font;
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            this.strokeWidth = strokeWidth;
            this.textColor = Color.BLACK;
        }

        public void setWidth(double width)
        {
            this.rect.width = width;
        }

        public double getWidth()
        {
            return rect.width;
        }

        @Override
        public void draw(Graphics2D g2d)
        {
            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.setColor(fillColor);
            g2d.fill(rect);
            g2d.setColor(strokeColor);
            g2d.draw(rect);
            g2d.setStroke(oldStroke);

            // Draw the text
            g2d.setFont(font);
            g2d.setColor(textColor);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();

            double textX = rect.getX() + (rect.getWidth() - textWidth) / 2;
            double textY = rect.getY() + (rect.getHeight() + textHeight) / 2 - fm.getDescent();

            g2d.drawString(text, (float) textX, (float) textY);
        }

        public boolean contains(Point2D p)
        {
            return rect.contains(p);
        }

        @Override
        public Rectangle2D getBoundingRect()
        {
            return rect.getBounds2D();
        }
    }
}
