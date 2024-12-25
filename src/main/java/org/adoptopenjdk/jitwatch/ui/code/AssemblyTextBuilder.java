package org.adoptopenjdk.jitwatch.ui.code;

import capstone.Capstone;
import capstone.api.Instruction;
import com.intellij.openapi.diagnostic.Logger;
import org.adoptopenjdk.jitwatch.model.Compilation;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.assembly.AssemblyBlock;
import org.adoptopenjdk.jitwatch.model.assembly.AssemblyInstruction;
import org.adoptopenjdk.jitwatch.model.assembly.AssemblyMethod;
import org.adoptopenjdk.jitwatch.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.*;

public class AssemblyTextBuilder
{
    private static final Logger logger = Logger.getInstance(AssemblyTextBuilder.class);

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

    public IMetaMember getCurrentMember()
    {
        return currentMember;
    }

    public AssemblyLine getLine(int line)
    {
        if (line >= 0 && line < lines.size())
        {
            return lines.get(line);
        }
        return null;
    }

    public void setCurrentMember(IMetaMember member, boolean reload)
    {
        if (!reload && member == currentMember)
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
                            String line = instr.toString(annoWidth, 0, true);
                            if (!instr.getHexaCode().isEmpty())
                            {
                                line = disassembly(line);
                            }
                            AssemblyLine assemblyLine = new AssemblyLine(line, instr);
                            lines.add(assemblyLine);
                        }
                        else
                        {
                            for (int i = 0; i < commentLines.size(); i++)
                            {
                                String line = instr.toString(annoWidth, i, true);
                                if (!instr.getHexaCode().isEmpty())
                                {
                                    line = disassembly(line);
                                }
                                AssemblyLine assemblyLine = new AssemblyLine(line, instr);
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
    private static final String PART_ADDRESS = "(0x[a-f0-9]+):";
    private static final String PART_INSTRUCTION = "([0-9a-fA-F]+(?:[\\s\\|]+[0-9a-fA-F]+)*)";
    private static final String PART_COMMENT1 = "([;#].*)?";
    private static final String PART_COMMENT2 = "([;#].*)";

    private static final Pattern PATTERN_HEXA_CODE_INSTRUCTION = Pattern
            .compile("^" + PART_ADDRESS + "\\s+" + PART_INSTRUCTION + PART_COMMENT1);

    private String disassembly(String line)
    {
        String result = line;
        Matcher m = PATTERN_HEXA_CODE_INSTRUCTION.matcher(line);
        if (m.find())
        {
            long address = Long.parseUnsignedLong(m.group(1).substring(2), 16);
            String codeStr = m.group(2);
            String comment = m.group(3);

            if (comment == null)
            {
                comment = "";
            }

            String[] byteGroups = codeStr.trim().split("\\s+|\\|");
            List<Byte> byteList = new ArrayList<>();

            for (String group : byteGroups)
            {
                String hex = group.toLowerCase().trim();
                for (int i = 0; i < hex.length(); i+=2)
                {
                    String byteHex = hex.substring(i, Math.min(i+2, hex.length()));
                    byteList.add((byte) Integer.parseInt(byteHex, 16));
                }
            }

            byte[] codeBytes = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++)
            {
                codeBytes[i] = byteList.get(i);
            }

            try (Capstone cs = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_64))
            {
                Instruction[] insns = cs.disasm(codeBytes, address);
                if (insns != null && insns.length > 0)
                {
                    for (Instruction insn : insns)
                    {
                        result = String.format("0x%x:\t%s\t%s %s\n", insn.getAddress(), insn.getMnemonic(), insn.getOpStr(), comment);
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Dissembly failed.", e);
                result = line;
            }
        }
        return result;
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

    public Integer getIndexForSourceLine(IMetaMember member, int sourceIndex)
    {
        Integer result = null;

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
