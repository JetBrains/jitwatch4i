package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.model.MemberSignatureParts;
import org.adoptopenjdk.jitwatch.ui.code.PsiMetaMemberWrapper;

import java.util.ArrayList;
import java.util.List;

public class JitWatchLanguageSupportUtil
{
    private static final String EP_NAME = "org.adoptopenjdk.jitwatch.ui.code.languageSupport";

    public static final LanguageExtension<JitWatchLanguageSupport<PsiElement, PsiElement>> LanguageSupport =
            new LanguageExtension<>(EP_NAME, DefaultJitLanguageSupport.INSTANCE);

    public static <T> T forElement(LanguageExtension<T> extension, PsiElement element)
    {
        return extension.forLanguage(element.getLanguage());
    }

    public static <CT extends PsiElement, MT extends PsiElement> boolean matchesSignature(
            JitWatchLanguageSupport<CT, MT> support, MT method, PsiMetaMemberWrapper metaMemberWrapper)
    {
        return support.matchesSignature(
                method,
                metaMemberWrapper.getMemberName(),
                metaMemberWrapper.getParamTypeNames(),
                metaMemberWrapper.getReturnTypeName()
        );
    }

    public static <CT extends PsiElement, MT extends PsiElement> boolean matchesSignature(
            JitWatchLanguageSupport<CT, MT> support, MT method, MemberSignatureParts signature)
    {
        return support.matchesSignature(
                method,
                signature.getMemberName(),
                signature.getParamTypes(),
                signature.getReturnType()
        );
    }

    public static boolean matchesSignature(PsiElement element, MemberSignatureParts metaMember)
    {
        JitWatchLanguageSupport<PsiElement, PsiElement> support = LanguageSupport.forLanguage(element.getLanguage());
        return matchesSignature(support, element, metaMember);
    }

    public static List<JitWatchLanguageSupport<PsiElement, PsiElement>> getAllSupportedLanguages()
    {
        ExtensionPointName<LanguageExtensionPoint<JitWatchLanguageSupport<PsiElement, PsiElement>>> epName =
                ExtensionPointName.create(EP_NAME);

        List<JitWatchLanguageSupport<PsiElement, PsiElement>> result = new ArrayList<>();
        for (LanguageExtensionPoint<JitWatchLanguageSupport<PsiElement, PsiElement>> extension : epName.getExtensionList())
        {
            String languageId = extension.language;
            Language language = Language.findLanguageByID(languageId);
            if (language != null)
            {
                JitWatchLanguageSupport<PsiElement, PsiElement> support = LanguageSupport.forLanguage(language);
                result.add(support);
            }
        }
        return result;
    }

    public static PsiElement findJavaMemberElement(Project project, PsiClass psiClass, String memberName, IMetaMember member)
    {
        PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiClassType psiClassType = elementFactory.createType(psiClass);

        PsiMethod[] methods = psiClass.findMethodsByName(memberName, false);

        for (PsiMethod method : methods)
        {
            if (method.getParameterList().getParametersCount() == member.getParamTypeNames().length)
            {
                boolean found = true;

                for (int i = 0; i < method.getParameterList().getParametersCount(); i++)
                {
                    String paramTypeA = member.getParamTypeNames()[i];
                    PsiType psiParamTypeB = method.getParameterList().getParameter(i).getType();
                    String paramTypeB = psiClassType.resolveGenerics().getSubstitutor().substitute(psiParamTypeB).getCanonicalText();
                    int genIndex = paramTypeB.indexOf('<');
                    if (genIndex > 0)
                    {
                        // metamodel has no generics
                        paramTypeB = paramTypeB.substring(0, genIndex);
                    }
                    if (!paramTypeA.equals(paramTypeB))
                    {
                        if (paramTypeA.contains("$"))
                        {
                            paramTypeA = paramTypeA.replaceAll("\\$", ".");
                            if (!paramTypeA.equals(paramTypeB))
                            {
                                found = false;
                                break;
                            }
                        }
                        else
                        {
                            found = false;
                            break;
                        }
                    }
                }

                if (found)
                {
                    return method;
                }
            }
        }

        return null;
    }

}