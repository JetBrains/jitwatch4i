package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.adoptopenjdk.jitwatch.model.MemberSignatureParts;
import org.adoptopenjdk.jitwatch.model.MetaClass;

import java.util.List;

public interface JitWatchLanguageSupport<ClassT extends PsiElement, MethodT extends PsiElement>
{
    List<ClassT> getAllClasses(PsiFile file);

    ClassT findClass(Project project, MetaClass metaClass);

    List<MethodT> getAllMethods(ClassT cls);

    MethodT findMethodAtOffset(PsiFile file, int offset);

    String getClassVMName(ClassT cls);

    ClassT getContainingClass(MethodT method);

    boolean matchesSignature(MethodT method, String memberName, List<String> paramTypeNames, String returnTypeName);

}