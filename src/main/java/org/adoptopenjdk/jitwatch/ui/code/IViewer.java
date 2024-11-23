package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.psi.PsiFile;
import org.adoptopenjdk.jitwatch.model.IMetaMember;

public interface IViewer
{
    void setContentFromMember(IMetaMember member);
    void syncEditorToViewer(LogicalPosition caretPosition);
    Integer findLine(IMetaMember metaMember, int sourceLine);
    Integer findLine(IMetaMember metaMember, int bytecodeOffset, int sourceLine);
}
