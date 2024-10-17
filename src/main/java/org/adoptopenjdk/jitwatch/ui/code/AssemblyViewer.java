package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupport;

import java.util.ArrayList;
import java.util.List;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.C_DOLLAR;
import static org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil.LanguageSupport;

public class AssemblyViewer implements IViewer
{
    private static final Logger logger = Logger.getInstance(AssemblyViewer.class);

    private final AssemblyPanel assemblyPanel;
    private final AssemblyTextBuilder assemblyTextBuilder;

    public AssemblyViewer(AssemblyPanel assemblyPanel)
    {
        this.assemblyPanel = assemblyPanel;
        this.assemblyTextBuilder = new AssemblyTextBuilder();
    }

    @Override
    public void setContentFromPsiFile(PsiFile sourceFile)
    {
        setContentFromMember(null);
    }

    @Override
    public void setContentFromMember(IMetaMember member)
    {
        assemblyTextBuilder.setCurrentMember(member);

        WriteCommandAction.runWriteCommandAction(assemblyPanel.getProject(), () ->
        {
            assemblyPanel.setMovingCaretInViewer(true);
            try
            {
                assemblyPanel.getViewerDocument().setText(assemblyTextBuilder.getText());
            }
            finally
            {
                assemblyPanel.setMovingCaretInViewer(false);
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

                    assemblyPanel.setMovingCaretInViewer(true);
                    try
                    {
                        assemblyPanel.moveSourceEditorCaretToLine(sourceLine - 1);
                    }
                    finally
                    {
                        assemblyPanel.setMovingCaretInViewer(false);
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
