package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.adoptopenjdk.jitwatch.model.MemberSignatureParts;
import org.adoptopenjdk.jitwatch.model.MetaClass;

import java.util.Collections;
import java.util.List;

public class DefaultJitLanguageSupport implements JitWatchLanguageSupport<PsiElement, PsiElement>
{
    public static final DefaultJitLanguageSupport INSTANCE = new DefaultJitLanguageSupport();

    @Override
    public List<PsiElement> getAllClasses(PsiFile file)
    {
        return Collections.emptyList();
    }

    @Override
    public PsiElement findClass(Project project, MetaClass metaClass)
    {
        return null;
    }

    @Override
    public List<PsiElement> getAllMethods(PsiElement cls)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isMethod(PsiElement element)
    {
        return false;
    }

    @Override
    public PsiElement findMethodAtOffset(PsiFile file, int offset)
    {
        return null;
    }

    @Override
    public TextRange getNameRange(PsiElement element)
    {
        return element.getTextRange();
    }

    @Override
    public String getClassVMName(PsiElement cls)
    {
        return null;
    }

    @Override
    public PsiElement getContainingClass(PsiElement method)
    {
        return null;
    }

    @Override
    public boolean matchesSignature(PsiElement method, String memberName, List<String> paramTypeNames, String returnTypeName)
    {
        return false;
    }

    @Override
    public PsiElement findCallToMember(PsiFile file, int offset, MemberSignatureParts calleeMember, int sameLineCallIndex)
    {
        return null;
    }

    @Override
    public PsiElement findAllocation(PsiFile file, int offset, String jvmName)
    {
        return null;
    }
}