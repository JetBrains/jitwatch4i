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
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.adoptopenjdk.jitwatch.model.bytecode.BCAnnotationType;
import org.adoptopenjdk.jitwatch.model.bytecode.BytecodeInstruction;
import org.adoptopenjdk.jitwatch.model.bytecode.LineAnnotation;
import org.adoptopenjdk.jitwatch.model.bytecode.LineTable;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupport;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil.LanguageSupport;

public class BytecodeViewer implements IViewer
{
    private final ByteCodePanel byteCodePanel;
    private final BytecodeTextBuilder bytecodeTextBuilder;
    private RangeHighlighter lineRangeHighlighter;

    public BytecodeViewer(ByteCodePanel byteCodePanel)
    {
        this.byteCodePanel = byteCodePanel;
        this.bytecodeTextBuilder = new BytecodeTextBuilder();
    }

    @Override
    public void setContentFromPsiFile(PsiFile sourceFile)
    {
        bytecodeTextBuilder.clear();

        PsiFile psiFile = sourceFile;

        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(psiFile.getLanguage());

        if (languageSupport == null)
        {
            return;
        }

        List<PsiElement> classes = languageSupport.getAllClasses(psiFile);

        List<MetaClass> metaClasses = new ArrayList<>();

        for (PsiElement cls : classes)
        {
            MetaClass metaClass = byteCodePanel.getModelService().getMetaClass(cls);
            if (metaClass != null)
            {
                metaClasses.add(metaClass);
                bytecodeTextBuilder.appendClass(metaClass);
            }
        }

        WriteCommandAction.runWriteCommandAction(byteCodePanel.getProject(), () ->
        {
            byteCodePanel.setMovingCaretInViewer(true);
            try
            {
                byteCodePanel.getViewerDocument().setText(bytecodeTextBuilder.getText());
            }
            finally
            {
                byteCodePanel.setMovingCaretInViewer(false);
            }
        });

        renderBytecodeAnnotations(psiFile);
    }

    @Override
    public void setContentFromMember(IMetaMember member)
    {
    }

    private void renderBytecodeAnnotations(PsiFile psiFile)
    {
        MarkupModel markupModel = DocumentMarkupModel.forDocument(byteCodePanel.getViewerDocument(), byteCodePanel.getProject(), true);
        markupModel.removeAllHighlighters();

        byteCodePanel.getModelService().processBytecodeAnnotations(psiFile, (method, member, memberBytecode, instruction, annotationsForBCI) ->
        {
            Integer line = bytecodeTextBuilder.findLine(member, instruction.getOffset());
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
        Document document = byteCodePanel.getViewerEditor().getDocument();
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

        byteCodePanel.setMovingCaretInViewer(true);
        try
        {
            byteCodePanel.moveSourceEditorCaretToLine(sourceLine - 1);
        }
        finally
        {
            byteCodePanel.setMovingCaretInViewer(false);
        }

        if (lineRangeHighlighter != null)
        {
            byteCodePanel.getViewerEditor().getMarkupModel().removeHighlighter(lineRangeHighlighter);
            lineRangeHighlighter = null;
        }

        List<BytecodeInstruction> instructionsForLine = JitWatchCodeUtil.findInstructionsForSourceLine(member.getMemberBytecode(), sourceLine);
        if (!instructionsForLine.isEmpty())
        {
            Integer startLine = bytecodeTextBuilder.findLine(member, instructionsForLine.get(0).getOffset());
            Integer endLine = bytecodeTextBuilder.findLine(member, instructionsForLine.get(instructionsForLine.size() - 1).getOffset());
            if (startLine != null && endLine != null)
            {
                int startOffset = byteCodePanel.getViewerDocument().getLineStartOffset(startLine);
                int endOffset = byteCodePanel.getViewerDocument().getLineStartOffset(endLine);

                Color caretRowColor = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.CARET_ROW_COLOR);
                if (caretRowColor != null)
                {
                    Color rangeColor = slightlyDarker(caretRowColor);
                    lineRangeHighlighter = byteCodePanel.getViewerEditor().getMarkupModel().addRangeHighlighter(
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
