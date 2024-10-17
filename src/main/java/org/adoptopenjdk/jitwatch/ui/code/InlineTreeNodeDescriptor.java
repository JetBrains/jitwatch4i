package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import org.adoptopenjdk.jitwatch.chain.CompileNode;
import org.adoptopenjdk.jitwatch.model.IMetaMember;

public class InlineTreeNodeDescriptor extends PresentableNodeDescriptor<CompileNode>
{
    private final CompileNode compileNode;
    private final boolean isRoot;

    public InlineTreeNodeDescriptor(Project project,
                                    NodeDescriptor parentDescriptor,
                                    CompileNode compileNode)
    {
        this(project, parentDescriptor, compileNode, false);
    }

    public InlineTreeNodeDescriptor(Project project,
                                    NodeDescriptor parentDescriptor,
                                    CompileNode compileNode,
                                    boolean isRoot)
    {
        super(project, parentDescriptor);
        this.compileNode = compileNode;
        this.isRoot = isRoot;
    }

    @Override
    protected void update(PresentationData presentation)
    {
        IMetaMember member = compileNode.getMember();
        String text;
        if (member.getMetaClass() != null && member.getMetaClass().getName() != null)
        {
            text = member.getMetaClass().getName();
        }
        else
        {
            text = "<unknown>";
        }

        SimpleTextAttributes attributes = (compileNode.isInlined() || isRoot)
                ? SimpleTextAttributes.REGULAR_ATTRIBUTES
                : SimpleTextAttributes.ERROR_ATTRIBUTES;

        presentation.addText(text, attributes);
        presentation.setTooltip(compileNode.getTooltipText());
    }

    @Override
    public CompileNode getElement()
    {
        return compileNode;
    }

    public CompileNode getCompileNode()
    {
        return compileNode;
    }
}
