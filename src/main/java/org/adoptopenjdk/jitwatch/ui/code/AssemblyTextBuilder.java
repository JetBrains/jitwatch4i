package org.adoptopenjdk.jitwatch.ui.code;

import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.adoptopenjdk.jitwatch.model.assembly.AssemblyBlock;
import org.adoptopenjdk.jitwatch.model.assembly.AssemblyInstruction;
import org.adoptopenjdk.jitwatch.model.assembly.AssemblyMethod;
import org.adoptopenjdk.jitwatch.model.bytecode.BytecodeInstruction;
import org.adoptopenjdk.jitwatch.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.*;

public class AssemblyTextBuilder
{
    public static class AssemblyLine
    {
        public final String line;
        public final AssemblyInstruction instruction;

        public AssemblyLine(String line, AssemblyInstruction instruction)
        {
            this.line = line;
            this.instruction = instruction;
        }
    }

    private final List<AssemblyLine> lines;
    private IMetaMember currentMember;

    public AssemblyTextBuilder()
    {
        lines = new ArrayList<>();
    }

    public AssemblyLine getLine(int line)
    {
        if (line >= 0 && line < lines.size())
        {
            return lines.get(line);
        }
        return null;
    }

    public void setCurrentMember(IMetaMember member)
    {
        if (member == currentMember)
        {
            return;
        }

        currentMember = member;

        lines.clear();

        if (currentMember == null)
        {
            return;
        }

        lines.add(new AssemblyLine(member.toString(), null));

        Compilation compilation = member.getSelectedCompilation();

        if (compilation != null)
        {
            AssemblyMethod asmMethod = compilation.getAssembly();

            if (asmMethod != null)
            {
                int annoWidth = asmMethod.getMaxAnnotationWidth();
                String annoPad = StringUtil.repeat(C_SPACE, annoWidth);
                String header = asmMethod.getHeader();

                if (header != null)
                {
                    String[] headerLines = header.split(S_NEWLINE);

                    for (String headerLine : headerLines)
                    {
                        lines.add(new AssemblyLine(annoPad + headerLine, null));
                    }
                }

                for (AssemblyBlock block : asmMethod.getBlocks())
                {
                    String title = block.getTitle();

                    if (title != null)
                    {
                        lines.add(new AssemblyLine(annoPad + title, null));
                    }

                    for (final AssemblyInstruction instr : block.getInstructions())
                    {
                        List<String> commentLines = instr.getCommentLines();

                        if (commentLines.size() == 0)
                        {
                            AssemblyLine assemblyLine = new AssemblyLine(instr.toString(annoWidth, 0, true), instr);
                            lines.add(assemblyLine);
                        }
                        else
                        {
                            for (int i = 0; i < commentLines.size(); i++)
                            {
                                AssemblyLine assemblyLine = new AssemblyLine(instr.toString(annoWidth, i, true), instr);
                                lines.add(assemblyLine);
                            }
                        }
                    }
                }
            }
            else
            {
                lines.add(new AssemblyLine("No assembly available for this member.", null));
            }
        }
        else
        {
            lines.add(new AssemblyLine("No compilation selected for this member.", null));
        }
    }

    public String getText()
    {
        List<String> lineTextList = lines.stream().map(line -> line.line).collect(Collectors.toUnmodifiableList());
        return String.join("\n", lineTextList);
    }

    public String getClassNameFromLine(AssemblyLine assemblyLine)
    {
        String result = null;

        if (assemblyLine != null)
        {
            result = StringUtil.getSubstringBetween(assemblyLine.line, "; - ", "::");
        }

        return result;
    }

    public String getSourceLineFromLine(AssemblyLine assemblyLine)
    {
        String result = null;

        if (assemblyLine != null)
        {
            result = StringUtil.getSubstringBetween(assemblyLine.line, "(line ", S_CLOSE_PARENTHESES);
        }

        return result;
    }

    public String getBytecodeOffsetFromLine(AssemblyLine assemblyLine)
    {
        String result = null;

        if (assemblyLine != null)
        {
            result = StringUtil.getSubstringBetween(assemblyLine.line, S_AT, S_SPACE);
        }

        return result;
    }

    public int getIndexForSourceLine(IMetaMember member, int sourceIndex)
    {
        int result = -1;

        if (member == currentMember)
        {
            int pos = 0;

            for (AssemblyLine line : lines)
            {
                String className = getClassNameFromLine(line);

                if (className != null && className.equals(member.getMetaClass().getFullyQualifiedName()))
                {
                    String labelSourceLine = getSourceLineFromLine(line);

                    if (labelSourceLine != null && labelSourceLine.equals(Integer.toString(sourceIndex)))
                    {
                        result = pos;
                        break;
                    }
                }

                pos++;
            }
        }
        return result;
    }
}
