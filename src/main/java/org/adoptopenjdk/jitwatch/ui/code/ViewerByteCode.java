package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.JBColor;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.bytecode.BCAnnotationType;
import org.adoptopenjdk.jitwatch.model.bytecode.BytecodeInstruction;
import org.adoptopenjdk.jitwatch.model.bytecode.LineAnnotation;
import org.adoptopenjdk.jitwatch.model.bytecode.LineTable;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;

public class ViewerByteCode extends CodePanelBase
{
    private final BytecodeTextBuilder bytecodeTextBuilder;
    private RangeHighlighter lineRangeHighlighter;

    public ViewerByteCode(Project project)
    {
        super(project);
        this.bytecodeTextBuilder = new BytecodeTextBuilder();
    }

    @Override
    public void setContentFromMember(IMetaMember member)
    {
        bytecodeTextBuilder.setCurrentMember(member);

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
        {
            setMovingCaretInViewer(true);
            try
            {
                getViewerDocument().setText(bytecodeTextBuilder.getText());
            }
            finally
            {
                setMovingCaretInViewer(false);
            }
        });

        renderBytecodeAnnotations(member);
    }

    private void renderBytecodeAnnotations(IMetaMember member)
    {
        if (member == null)
        {
            return;
        }

        MarkupModel markupModel = DocumentMarkupModel.forDocument(getViewerDocument(), getProject(), true);
        markupModel.removeAllHighlighters();

        getModelService().processMemberBytecodeAnnotations(member, (method, member1, memberBytecode, instruction, annotationsForBCI) ->
        {
            Integer line = bytecodeTextBuilder.findLine(member1, instruction.getOffset());
            if (line == null)
            {
                return;
            }
            Color color = null;
            for (LineAnnotation lineAnnotation : annotationsForBCI)
            {
                color = getColorForBytecodeAnnotation(lineAnnotation.getType());
                if (color != null)
                {
                    break;
                }
            }
            String tooltip = String.join("\n", annotationsForBCI.stream().map(Object::toString).toArray(String[]::new));
            highlightBytecodeLine(line, color, tooltip, markupModel);
        });
    }

    private Color getColorForBytecodeAnnotation(BCAnnotationType type)
    {
        switch (type)
        {
            case BRANCH:
                return JBColor.BLUE;
            case ELIMINATED_ALLOCATION:
            case LOCK_COARSEN:
            case LOCK_ELISION:
                return JBColor.GRAY;
            case INLINE_FAIL:
                return JBColor.RED;
            case INLINE_SUCCESS:
                return JBColor.GREEN.darker().darker();
            case UNCOMMON_TRAP:
                return JBColor.MAGENTA;
            default:
                return null;
        }
    }

    private void highlightBytecodeLine(int line, Color color, String tooltip, MarkupModel markupModel)
    {
        Document document = getViewerEditor().getDocument();
        int lineStartOffset = document.getLineStartOffset(line);
        int lineEndOffset = document.getLineEndOffset(line);

        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setForegroundColor(color);

        RangeHighlighterEx highlighter = (RangeHighlighterEx) markupModel.addRangeHighlighter(
                Math.min(lineStartOffset + 4, lineEndOffset),
                lineEndOffset,
                HighlighterLayer.SYNTAX,
                textAttributes,
                HighlighterTargetArea.EXACT_RANGE
        );

        HighlightInfo highlightInfo = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                .range(highlighter.getStartOffset(), highlighter.getEndOffset())
                .description(tooltip)
                .textAttributes(textAttributes)
                .unescapedToolTip(tooltip)
                .createUnconditionally();

        try
        {
            Field highlighterField = HighlightInfo.class.getDeclaredField("highlighter");
            highlighterField.setAccessible(true);
            highlighterField.set(highlightInfo, highlighter);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
        }

        highlighter.setErrorStripeTooltip(highlightInfo);
    }

    @Override
    public void syncEditorToViewer(LogicalPosition caretPosition)
    {
        Pair<IMetaMember, BytecodeInstruction> instructionPair = bytecodeTextBuilder.findInstruction(caretPosition.line);
        if (instructionPair == null)
        {
            return;
        }
        IMetaMember member = instructionPair.first;
        BytecodeInstruction instruction = instructionPair.second;
        if (member.getMemberBytecode() == null)
        {
            return;
        }
        LineTable lineTable = member.getMemberBytecode().getLineTable();
        int sourceLine = lineTable.findSourceLineForBytecodeOffset(instruction.getOffset());
        if (sourceLine == -1)
        {
            return;
        }

        setMovingCaretInViewer(true);
        try
        {
            moveSourceEditorCaretToLine(sourceLine - 1);
        }
        finally
        {
            setMovingCaretInViewer(false);
        }

        if (lineRangeHighlighter != null)
        {
            getViewerEditor().getMarkupModel().removeHighlighter(lineRangeHighlighter);
            lineRangeHighlighter = null;
        }

        List<BytecodeInstruction> instructionsForLine = JitWatchCodeUtil.findInstructionsForSourceLine(member.getMemberBytecode(), sourceLine);
        if (!instructionsForLine.isEmpty())
        {
            Integer startLine = bytecodeTextBuilder.findLine(member, instructionsForLine.get(0).getOffset());
            Integer endLine = bytecodeTextBuilder.findLine(member, instructionsForLine.get(instructionsForLine.size() - 1).getOffset());
            if (startLine != null && endLine != null)
            {
                int startOffset = getViewerDocument().getLineStartOffset(startLine);
                int endOffset = getViewerDocument().getLineStartOffset(endLine);

                Color caretRowColor = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.CARET_ROW_COLOR);
                if (caretRowColor != null)
                {
                    Color rangeColor = slightlyDarker(caretRowColor);
                    lineRangeHighlighter = getViewerEditor().getMarkupModel().addRangeHighlighter(
                            startOffset,
                            endOffset,
                            HighlighterLayer.CARET_ROW - 1,
                            new TextAttributes(null, rangeColor, null, null, 0),
                            HighlighterTargetArea.LINES_IN_RANGE
                    );
                }
            }
        }
    }

    @Override
    public Integer findLine(IMetaMember metaMember, int sourceLine)
    {
        return bytecodeTextBuilder.findLine(metaMember);
    }

    @Override
    public Integer findLine(IMetaMember metaMember, int bytecodeOffset, int sourceLine)
    {
        return bytecodeTextBuilder.findLine(metaMember, bytecodeOffset);
    }

    private Color slightlyDarker(Color color)
    {
        int red = Math.max((int) (color.getRed() * 0.9), 0);
        int green = Math.max((int) (color.getGreen() * 0.9), 0);
        int blue = Math.max((int) (color.getBlue() * 0.9), 0);
        return new Color(red, green, blue, color.getAlpha());
    }
}

