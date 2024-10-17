package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.project.Project;

public class AssemblyPanel extends CodePanelBase
{
    private final AssemblyViewer assemblyViewer;

    public AssemblyPanel(Project project)
    {
        super(project);
        assemblyViewer = new AssemblyViewer(this);
    }

    @Override
    protected IViewer getViewer()
    {
        return assemblyViewer;
    }
}
