package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.adoptopenjdk.jitwatch.model.bytecode.BytecodeInstruction;
import org.adoptopenjdk.jitwatch.model.bytecode.LineTableEntry;
import org.adoptopenjdk.jitwatch.model.bytecode.MemberBytecode;
import org.adoptopenjdk.jitwatch.ui.main.JITWatchUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JitWatchCodeUtil
{
    public static final String TOOL_WINDOW_ID = "JITWatch";

    public static CodeToolWindowManager registerToolWindows(Project project, JITWatchUI jitWatchIdeaUI)
    {
        CodeToolWindowManager result = null;
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow existingToolWindow = getToolWindow(project);
        if (existingToolWindow == null)
        {
            ToolWindow toolWindow = toolWindowManager.registerToolWindow(
                    TOOL_WINDOW_ID, false, ToolWindowAnchor.RIGHT, project, true);
            ByteCodePanel byteCodePanel = new ByteCodePanel(project);
            toolWindow.getContentManager().addContent(
                    ContentFactory.getInstance().createContent(byteCodePanel, "ByteCode", false)
            );
            AssemblyPanel assemblyPanel = new AssemblyPanel(project);
            toolWindow.getContentManager().addContent(
                    ContentFactory.getInstance().createContent(assemblyPanel, "Asm", false)
            );
            result = new CodeToolWindowManager(project, toolWindow, byteCodePanel, assemblyPanel);
        }
        return result;
    }

    public static ToolWindow getToolWindow(Project project)
    {
        return ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
    }

    public static CodeToolWindowManager getCodeToolwindowManger(Project project)
    {
        ToolWindow toolWindow = getToolWindow(project);
        if (toolWindow != null)
        {
            ContentManager contentManager = toolWindow.getContentManager();
            CodePanelBase codePanelBase = (CodePanelBase) Arrays.stream(contentManager.getContents()).findFirst().orElse(null);
            return codePanelBase.getCodeToolWindowManager();
        }
        return null;
    }

    public static List<BytecodeInstruction> findInstructionsForSourceLine(MemberBytecode memberBytecode, int sourceLine)
    {
        List<LineTableEntry> entries = memberBytecode.getLineTable().getEntries();
        int lineEntryIndex = -1;

        for (int i = 0; i < entries.size(); i++)
        {
            if (entries.get(i).getSourceOffset() == sourceLine)
            {
                lineEntryIndex = i;
                break;
            }
        }

        if (lineEntryIndex >= 0)
        {
            int startBytecodeOffset = entries.get(lineEntryIndex).getBytecodeOffset();
            int nextLineBytecodeOffset;

            if (lineEntryIndex < entries.size() - 1)
            {
                nextLineBytecodeOffset = entries.get(lineEntryIndex + 1).getBytecodeOffset();
            }
            else
            {
                nextLineBytecodeOffset = -1;
            }

            List<BytecodeInstruction> result = new ArrayList<>();
            for (BytecodeInstruction instruction : memberBytecode.getInstructions())
            {
                int offset = instruction.getOffset();
                if (offset >= startBytecodeOffset && (nextLineBytecodeOffset == -1 || offset < nextLineBytecodeOffset))
                {
                    result.add(instruction);
                }
            }
            return result;
        }

        return List.of();
    }
}
