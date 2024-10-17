package org.adoptopenjdk.jitwatch.ui.code;

import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.adoptopenjdk.jitwatch.model.bytecode.BytecodeInstruction;
import org.adoptopenjdk.jitwatch.model.bytecode.MemberBytecode;
import com.intellij.openapi.util.Pair;

import java.util.*;

public class BytecodeTextBuilder
{
    private static class MemberBytecodeMap
    {
        public final int startLine;
        public final Map<BytecodeInstruction, Integer> instructionToLineMap;

        public MemberBytecodeMap(int startLine)
        {
            this.startLine = startLine;
            this.instructionToLineMap = new HashMap<>();
        }
    }

    private final StringBuilder builder;
    private int currentLine;
    private final Map<IMetaMember, MemberBytecodeMap> memberIndex;
    private final List<Object> lineIndex;

    public BytecodeTextBuilder()
    {
        this.builder = new StringBuilder();
        this.currentLine = 0;
        this.memberIndex = new HashMap<>();
        this.lineIndex = new ArrayList<>();
    }

    public void clear()
    {
        builder.setLength(0);
        currentLine = 0;
        memberIndex.clear();
        lineIndex.clear();
    }

    public void appendClass(MetaClass metaClass)
    {
        for (IMetaMember member : metaClass.getMetaMembers())
        {
            MemberBytecodeMap bytecodeMap = new MemberBytecodeMap(currentLine);
            memberIndex.put(member, bytecodeMap);

            MemberBytecode memberBytecode = member.getMemberBytecode();
            appendLine(member.toStringUnqualifiedMethodName(false, false), member);
            if (memberBytecode != null)
            {
                appendBytecode(memberBytecode, bytecodeMap);
            }
            appendLine("", null);
        }
    }

    public String getText()
    {
        return builder.toString();
    }

    private void appendLine(String text, Object associatedObject)
    {
        builder.append(text).append("\n");
        lineIndex.add(associatedObject);
        currentLine++;
    }

    private void appendBytecode(MemberBytecode memberBC, MemberBytecodeMap bytecodeMap)
    {
        OptionalInt maxOffsetOpt = memberBC.getInstructions().stream()
                .mapToInt(BytecodeInstruction::getOffset)
                .max();
        int maxOffset = maxOffsetOpt.orElse(0);

        for (BytecodeInstruction instruction : memberBC.getInstructions())
        {
            bytecodeMap.instructionToLineMap.put(instruction, currentLine);
            int labelLines = Math.max(instruction.getLabelLines(), 1);
            for (int line = 0; line < labelLines; line++)
            {
                String formattedInstruction = "    " + instruction.toString(maxOffset, line);
                appendLine(formattedInstruction, instruction);
            }
        }
    }

    public Integer findLine(IMetaMember member)
    {
        MemberBytecodeMap map = memberIndex.get(member);
        return (map != null) ? map.startLine : null;
    }

    public Integer findLine(IMetaMember member, int bytecodeOffset)
    {
        MemberBytecode memberBC = member.getMemberBytecode();
        if (memberBC == null) return null;

        BytecodeInstruction instruction = null;
        for (BytecodeInstruction instr : memberBC.getInstructions())
        {
            if (instr.getOffset() >= bytecodeOffset)
            {
                instruction = instr;
                break;
            }
        }
        if (instruction == null) return null;

        MemberBytecodeMap map = memberIndex.get(member);
        if (map == null) return null;
        return map.instructionToLineMap.get(instruction);
    }

    public Pair<IMetaMember, BytecodeInstruction> findInstruction(int line)
    {
        if (line < 0 || line >= lineIndex.size())
        {
            return null;
        }

        Object elementAtLine = lineIndex.get(line);
        if (elementAtLine instanceof IMetaMember)
        {
            return Pair.create((IMetaMember) elementAtLine, (BytecodeInstruction) null);
        }

        for (int memberLine = line - 1; memberLine >= 0; memberLine--)
        {
            Object elementAtMemberLine = lineIndex.get(memberLine);
            if (elementAtMemberLine instanceof IMetaMember)
            {
                if (elementAtLine instanceof BytecodeInstruction)
                {
                    return Pair.create((IMetaMember) elementAtMemberLine, (BytecodeInstruction) elementAtLine);
                }
                else
                {
                    return Pair.create((IMetaMember) elementAtMemberLine, null);
                }
            }
        }
        return null;
    }
}
