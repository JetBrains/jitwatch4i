package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.project.Project;

public class ByteCodePanel extends CodePanelBase
{
    private final BytecodeViewer bytecodeViewer;

    public ByteCodePanel(Project project)
    {
        super(project);
        bytecodeViewer = new BytecodeViewer(this);
    }

    @Override
    protected IViewer getViewer()
    {
        return bytecodeViewer;
    }
}
