package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.code.languages.DefaultJitLanguageSupport;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupport;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

import static org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil.LanguageSupport;

public class JitLineMarkerProvider implements LineMarkerProvider
{
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element)
    {
        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(element.getLanguage());
        if (languageSupport == DefaultJitLanguageSupport.INSTANCE)
        {
            return null;
        }

        if (!languageSupport.isMethod(element))
        {
            return null;
        }

        Project project = element.getProject();
        JitWatchModelService modelService = JitWatchModelService.getInstance(project);
        if (modelService.getModel() == null)
        {
            return null;
        }

        IMetaMember metaMember = modelService.getMetaMember(element);
        if (metaMember == null)
        {
            return notCompiledMarker(element);
        }

        if (metaMember.isCompiled())
        {
            return metaMemberMarker(element, metaMember);
        }
        else
        {
            return notCompiledMarker(element);
        }
    }

    private LineMarkerInfo<?> notCompiledMarker(@NotNull PsiElement element)
    {
        return new LineMarkerInfo<>(
                element,
                LanguageSupport.forLanguage(element.getLanguage()).getNameRange(element),
                AllIcons.Actions.Suspend,
                method -> "Not compiled",
                null,
                GutterIconRenderer.Alignment.CENTER,
                () ->  "Not compiled accessible name"
        );
    }

    private LineMarkerInfo<?> metaMemberMarker(@NotNull PsiElement method, @NotNull IMetaMember metaMember)
    {
        String decompilesStr = metaMember.getCompiledAttributes().getOrDefault("decompiles", "0");
        int decompiles = Integer.parseInt(decompilesStr);
        Icon icon = (decompiles > 0) ? AllIcons.Actions.ForceRefresh : AllIcons.Actions.Compile;

        return new LineMarkerInfo<>(
                method,
                LanguageSupport.forLanguage(method.getLanguage()).getNameRange(method),
                icon,
                methodElement -> buildCompiledTooltip(metaMember),
                (mouseEvent, elt) -> {
                    ToolWindow toolWindow = JitWatchCodeUtil.getToolWindow(elt.getProject());
                    if (toolWindow != null)
                    {
                        toolWindow.activate(() -> {
                            CodeToolWindowManager codeToolWindowManager = JitWatchCodeUtil.getCodeToolwindowManger(elt.getProject());
                            if (codeToolWindowManager != null)
                            {
                                codeToolWindowManager.navigateToMember(elt);
                            }
                        });
                    }
                },
                GutterIconRenderer.Alignment.CENTER,
                () -> {
                    String elementName = (method instanceof PsiNamedElement) ? ((PsiNamedElement) method).getName() : method.getText();
                    return "Compiled element: " + elementName;
                }
        );
    }

    private String buildCompiledTooltip(@NotNull IMetaMember metaMember)
    {
        String compiler = metaMember.getCompiledAttributes().getOrDefault("compiler", "?");
        String compileMillis = metaMember.getCompiledAttributes().getOrDefault("compileMillis", "?");
        String bytecodeSize = metaMember.getCompiledAttributes().getOrDefault("bytes", "?");
        String nativeSize = metaMember.getCompiledAttributes().getOrDefault("nmsize", "?");
        String decompiles = metaMember.getCompiledAttributes().get("decompiles");

        StringBuilder message = new StringBuilder();
        message.append("Compiled with ").append(compiler)
                .append(" in ").append(compileMillis).append(" ms")
                .append(", bytecode size ").append(bytecodeSize)
                .append(", native size ").append(nativeSize);

        if (decompiles != null)
        {
            message.append(". Decompiled ").append(decompiles).append(" times");
        }

        return message.toString();
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result)
    {
        // No implementation needed unless additional slow processing is required.
    }
}
