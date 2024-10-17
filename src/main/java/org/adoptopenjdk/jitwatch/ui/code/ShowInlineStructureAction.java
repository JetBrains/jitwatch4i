package org.adoptopenjdk.jitwatch.ui.code;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseTreePopupStep;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import org.adoptopenjdk.jitwatch.chain.CompileChainWalker;
import org.adoptopenjdk.jitwatch.chain.CompileNode;
import org.adoptopenjdk.jitwatch.model.IMetaMember;
import org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupport;

import static org.adoptopenjdk.jitwatch.ui.code.languages.JitWatchLanguageSupportUtil.LanguageSupport;

public class ShowInlineStructureAction extends AnAction
{
    @Override
    public void actionPerformed(AnActionEvent e)
    {
        IMetaMember metaMember = findTargetMember(e);
        if (metaMember == null)
        {
            return;
        }
        Project project = e.getProject();
        if (project == null)
        {
            return;
        }
        JitWatchModelService modelService = JitWatchModelService.getInstance(project);
        CompileChainWalker compileChainWalker = new CompileChainWalker(modelService.getModel());
        CompileNode compileNode = compileChainWalker.buildCallTree(metaMember.getCompilation(0));
        if (compileNode == null)
        {
            return;
        }
        InlineTreeStructure treeStructure = new InlineTreeStructure(project, compileNode);
        BaseTreePopupStep<InlineTreeNodeDescriptor> popupStep = new BaseTreePopupStep<InlineTreeNodeDescriptor>(project, "Inline", treeStructure)
        {
            @Override
            public boolean isRootVisible()
            {
                return true;
            }

            @Override
            public PopupStep<?> onChosen(InlineTreeNodeDescriptor selectedValue, boolean finalChoice)
            {
                if (selectedValue != null)
                {
                    IMetaMember member = selectedValue.getCompileNode().getMember();
                    PsiElement psiMember = JitWatchModelService.getInstance(project).getPsiMember(member);
                    if (psiMember instanceof Navigatable)
                    {
                        ((Navigatable) psiMember).navigate(true);
                    }
                }
                return PopupStep.FINAL_CHOICE;
            }
        };
        JBPopupFactory.getInstance().createTree(popupStep).showInBestPositionFor(e.getDataContext());
    }

    @Override
    public void update(AnActionEvent e)
    {
        e.getPresentation().setEnabledAndVisible(findTargetMember(e) != null);
    }

    private IMetaMember findTargetMember(AnActionEvent e)
    {
        Project project = e.getProject();
        if (project == null)
        {
            return null;
        }
        JitWatchModelService modelService = JitWatchModelService.getInstance(project);
        if (modelService.getModel() == null)
        {
            return null;
        }
        PsiElement psiElement = e.getData(LangDataKeys.PSI_ELEMENT);
        if (psiElement == null)
        {
            return null;
        }
        JitWatchLanguageSupport<PsiElement, PsiElement> languageSupport = LanguageSupport.forLanguage(psiElement.getLanguage());
        if (languageSupport.isMethod(psiElement))
        {
            return modelService.getMetaMember(psiElement);
        }
        return null;
    }
}
