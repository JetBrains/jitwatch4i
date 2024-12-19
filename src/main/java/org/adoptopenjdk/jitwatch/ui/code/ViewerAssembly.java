package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.project.Project;
import com.nasmlanguage.NASMSyntaxHighlighter;
import org.adoptopenjdk.jitwatch.model.IMetaMember;

public class ViewerAssembly extends CodePanelBase
{
    private static final Logger logger = Logger.getInstance(ViewerAssembly.class);

    private final AssemblyTextBuilder assemblyTextBuilder;
    private final EditorHighlighter editorHighlighter;

    public ViewerAssembly(Project project)
    {
        super(project);
        this.assemblyTextBuilder = new AssemblyTextBuilder();
        editorHighlighter = new LexerEditorHighlighter(
                new NASMSyntaxHighlighter(),
                EditorColorsManager.getInstance().getGlobalScheme()
        );
        ((EditorEx) getViewerEditor()).setHighlighter(editorHighlighter);
    }

    public IMetaMember getCurrentMember()
    {
        return assemblyTextBuilder.getCurrentMember();
    }

    @Override
    public void setContentFromMember(IMetaMember member, boolean reload)
    {
        assemblyTextBuilder.setCurrentMember(member, reload);

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
        {
            setMovingCaretInViewer(true);
            try
            {
                getViewerDocument().setText(assemblyTextBuilder.getText());
            }
            finally
            {
                setMovingCaretInViewer(false);
            }
        });
    }

    @Override
    public void syncEditorToViewer(LogicalPosition caretPosition)
    {
        AssemblyTextBuilder.AssemblyLine assemblyLine = assemblyTextBuilder.getLine(caretPosition.line);

        if (assemblyLine != null)
        {
            String strSourceLine = assemblyTextBuilder.getSourceLineFromLine(assemblyLine);

            if (strSourceLine != null)
            {
                try
                {
                    int sourceLine = Integer.parseInt(strSourceLine) - 1;

                    setMovingCaretInViewer(true);
                    try
                    {
                        moveSourceEditorCaretToLine(sourceLine - 1);
                    }
                    finally
                    {
                        setMovingCaretInViewer(false);
                    }
                }
                catch (NumberFormatException nfe)
                {
                    logger.error("Could not parse source line number: " + strSourceLine, nfe);
                }
            }
        }
    }

    @Override
    public Integer findLine(IMetaMember metaMember, int sourceLine)
    {
        return 0;
    }

    @Override
    public Integer findLine(IMetaMember metaMember, int bytecodeOffset, int sourceLine)
    {
        return assemblyTextBuilder.getIndexForSourceLine(metaMember, sourceLine);
    }
}
