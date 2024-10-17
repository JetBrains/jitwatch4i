package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import org.adoptopenjdk.jitwatch.chain.CompileNode;

import java.util.ArrayList;
import java.util.List;

public class InlineTreeStructure extends AbstractTreeStructure
{
    private final Project project;
    private final CompileNode root;

    public InlineTreeStructure(Project project, CompileNode root)
    {
        this.project = project;
        this.root = root;
    }

    @Override
    public Object getRootElement()
    {
        return new InlineTreeNodeDescriptor(project, null, root, true);
    }

    @Override
    public Object[] getChildElements(Object element)
    {
        InlineTreeNodeDescriptor nodeDescriptor = (InlineTreeNodeDescriptor) element;
        List<CompileNode> children = nodeDescriptor.getCompileNode().getChildren();
        List<InlineTreeNodeDescriptor> childDescriptors = new ArrayList<>();
        for (CompileNode child : children)
        {
            childDescriptors.add(new InlineTreeNodeDescriptor(project, nodeDescriptor, child));
        }
        return childDescriptors.toArray();
    }

    @Override
    public Object getParentElement(Object element)
    {
        InlineTreeNodeDescriptor nodeDescriptor = (InlineTreeNodeDescriptor) element;
        return nodeDescriptor.getParentDescriptor();
    }

    @Override
    public NodeDescriptor createDescriptor(Object element, NodeDescriptor parentDescriptor)
    {
        return (NodeDescriptor) element;
    }

    @Override
    public void commit()
    {
        // No action needed
    }

    @Override
    public boolean hasSomethingToCommit()
    {
        return false;
    }
}
