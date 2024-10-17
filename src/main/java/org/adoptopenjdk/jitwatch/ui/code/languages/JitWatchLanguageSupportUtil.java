package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
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
}