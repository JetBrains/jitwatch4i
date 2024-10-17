package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.LogParseException;
import org.adoptopenjdk.jitwatch.model.MemberSignatureParts;
import org.adoptopenjdk.jitwatch.model.bytecode.BytecodeInstruction;
import org.adoptopenjdk.jitwatch.model.bytecode.LineAnnotation;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupport;
import org.adoptopenjdk.jitwatch.model.bytecode.MemberBytecode;
import org.adoptopenjdk.jitwatch.util.ParseUtil;
import org.jetbrains.annotations.NotNull;
import static org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil.LanguageSupport;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class JitSourceAnnotator extends ExternalAnnotator<PsiFile, List<Pair<PsiElement, LineAnnotation>>>
{
    private static final Logger LOG = Logger.getInstance(JitSourceAnnotator.class);

    @Override
    public PsiFile collectInformation(@NotNull PsiFile file)
    {
        return file;
    }

    @Override
    public List<Pair<PsiElement, LineAnnotation>> doAnnotate(PsiFile file)
    {
        if (file == null)
        {
            return null;
        }

        JitWatchModelService service = JitWatchModelService.getInstance(file.getProject());
        if (service.getModel() == null)
        {
            return null;
        }

        service.loadBytecode(file);

        return ApplicationManager.getApplication().runReadAction((Computable<List<Pair<PsiElement, LineAnnotation>>>) () -> mapBytecodeAnnotationsToSource(file));
    }

    private List<Pair<PsiElement, LineAnnotation>> mapBytecodeAnnotationsToSource(PsiFile psiFile)
    {
        List<Pair<PsiElement, LineAnnotation>> result = new ArrayList<>();
        JitWatchModelService modelService = JitWatchModelService.getInstance(psiFile.getProject());

        modelService.processBytecodeAnnotations(psiFile, (method, member, memberBytecode, instruction, lineAnnotations) ->
        {
            for (LineAnnotation lineAnnotation : lineAnnotations)
            {
                PsiElement sourceElement = mapBytecodeAnnotationToSource(method, member, memberBytecode, instruction, lineAnnotation);
                if (sourceElement != null)
                {
                    result.add(Pair.create(sourceElement, lineAnnotation));
                }
            }
        });

        return result;
    }

    private PsiElement mapBytecodeAnnotationToSource(PsiElement method,
                                                     IMetaMember member,
                                                     MemberBytecode memberBytecode,
                                                     BytecodeInstruction instruction,
                                                     LineAnnotation lineAnnotation)
    {
        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(method.getLanguage());

        if (languageSupport == null)
        {
            return null;
        }

        int sourceLine = memberBytecode.getLineTable().findSourceLineForBytecodeOffset(instruction.getOffset());
        if (sourceLine == -1)
        {
            return null;
        }

        PsiFile psiFile = method.getContainingFile();
        if (psiFile == null)
        {
            return null;
        }

        Integer lineStartOffset = findLineStart(psiFile, sourceLine);
        if (lineStartOffset == null)
        {
            return null;
        }

        switch (lineAnnotation.getType())
        {
            case INLINE_SUCCESS:
            case INLINE_FAIL:
                int index = findSameLineCallIndex(memberBytecode, sourceLine, instruction);
                MemberSignatureParts calleeMember = null;
                try
                {
                    calleeMember = getMemberSignatureFromBytecodeComment(member, instruction);
                }
                catch (LogParseException e)
                {
                    throw new RuntimeException(e);
                }

                if (calleeMember == null)
                {
                    LOG.info("Can't find callee by comment: " + instruction.getComment());
                    return null;
                }
                return languageSupport.findCallToMember(psiFile, lineStartOffset, calleeMember, index);

            case ELIMINATED_ALLOCATION:
                String comment = instruction.getComment().replaceFirst("^// class ", "");
                return languageSupport.findAllocation(psiFile, lineStartOffset, comment);

            default:
                return null;
        }
    }

    private MemberSignatureParts getMemberSignatureFromBytecodeComment(IMetaMember currentMember,
                                                                       BytecodeInstruction instruction) throws LogParseException
    {
        String comment = instruction.getCommentWithMemberPrefixStripped();
        if (comment == null)
        {
            return null;
        }

        if (ParseUtil.bytecodeMethodCommentHasNoClassPrefix(comment))
        {
            String currentClass = currentMember.getMetaClass().getFullyQualifiedName().replace('.', '/');
            comment = currentClass + "." + comment;
        }

        return MemberSignatureParts.fromBytecodeComment(comment);
    }

    private int findSameLineCallIndex(MemberBytecode memberBytecode,
                                      int sourceLine,
                                      BytecodeInstruction invokeInstruction)
    {
        int result = -1;
        List<BytecodeInstruction> sameLineInstructions = JitWatchCodeUtil.findInstructionsForSourceLine(memberBytecode, sourceLine);
        for (BytecodeInstruction instruction : sameLineInstructions)
        {
            if (instruction.getOpcode().equals(invokeInstruction.getOpcode())
                    && instruction.getComment().equals(invokeInstruction.getComment()))
            {
                result++;
            }
            if (instruction.equals(invokeInstruction))
            {
                break;
            }
        }
        return Math.max(result, 0);
    }

    private Integer findLineStart(PsiFile psiFile, int sourceLine)
    {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(psiFile.getProject());
        com.intellij.openapi.editor.Document document = documentManager.getDocument(psiFile);
        if (document == null)
        {
            return null;
        }

        int adjustedLine = sourceLine - 1;
        if (adjustedLine >= document.getLineCount())
        {
            return null;
        }

        int lineStartOffset = document.getLineStartOffset(adjustedLine);
        while (lineStartOffset < document.getTextLength() && psiFile.findElementAt(lineStartOffset) instanceof com.intellij.psi.PsiWhiteSpace)
        {
            lineStartOffset++;
        }

        return lineStartOffset;
    }

    @Override
    public void apply(@NotNull PsiFile file,
                      List<Pair<PsiElement, LineAnnotation>> annotationResult,
                      @NotNull AnnotationHolder holder)
    {
        if (annotationResult == null)
        {
            return;
        }

        for (Pair<PsiElement, LineAnnotation> pair : annotationResult)
        {
            applyAnnotation(pair.getFirst(), pair.getSecond(), holder);
        }
    }

    private void applyAnnotation(PsiElement element, LineAnnotation lineAnnotation, AnnotationHolder holder)
    {
        com.intellij.lang.annotation.Annotation annotation = holder.createInfoAnnotation(element, lineAnnotation.getAnnotation());

        Color color = null;
        switch (lineAnnotation.getType())
        {
            case INLINE_SUCCESS:
            case ELIMINATED_ALLOCATION:
                color = com.intellij.ui.JBColor.GREEN;
                break;
            case INLINE_FAIL:
                color = com.intellij.ui.JBColor.RED;
                break;
            default:
                break;
        }

        if (color != null)
        {
            annotation.setEnforcedTextAttributes(underline(color));
        }
    }

    private com.intellij.openapi.editor.markup.TextAttributes underline(Color color)
    {
        com.intellij.openapi.editor.markup.TextAttributes attributes = new com.intellij.openapi.editor.markup.TextAttributes();
        attributes.setEffectType(com.intellij.openapi.editor.markup.EffectType.LINE_UNDERSCORE);
        attributes.setEffectColor(color);
        return attributes;
    }

    @FunctionalInterface
    public interface BytecodeAnnotationCallback
    {
        void process(PsiElement method,
                     IMetaMember member,
                     MemberBytecode memberBytecode,
                     BytecodeInstruction instruction,
                     List<LineAnnotation> lineAnnotations);
    }
}
