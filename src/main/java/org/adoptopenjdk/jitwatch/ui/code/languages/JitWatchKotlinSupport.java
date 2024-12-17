package org.adoptopenjdk.jitwatch.ui.code.languages;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.adoptopenjdk.jitwatch.model.MetaClass;
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex;
import org.jetbrains.kotlin.psi.KtCallableDeclaration;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtTypeReference;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JitWatchKotlinSupport implements JitWatchLanguageSupport<KtClassOrObject, KtCallableDeclaration>
{
    @Override
    public List<KtClassOrObject> getAllClasses(PsiFile file)
    {
        return ApplicationManager.getApplication().runReadAction(new Computable<List<KtClassOrObject>>()
        {
            @Override
            public List<KtClassOrObject> compute()
            {
                Collection<KtClassOrObject> ktClasses = PsiTreeUtil.collectElementsOfType(file, KtClassOrObject.class);
                return new ArrayList<>(ktClasses);
            }
        });
    }

    @Override
    public KtClassOrObject findClass(Project project, MetaClass metaClass)
    {
        Collection<KtClassOrObject> classes = KotlinFullClassNameIndex.getInstance()
                .get(metaClass.getFullyQualifiedName(), project, ProjectScope.getAllScope(project));

        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Override
    public List<KtCallableDeclaration> getAllMethods(KtClassOrObject cls)
    {
        List<KtCallableDeclaration> methods = new ArrayList<>();
        methods.addAll(cls.getDeclarations().stream()
                .filter(declaration -> declaration instanceof KtCallableDeclaration)
                .map(declaration -> (KtNamedFunction) declaration)
                .toList());
        return methods;
    }

    @Override
    public KtCallableDeclaration findMethodAtOffset(PsiFile file, int offset)
    {
        return PsiTreeUtil.getParentOfType(file.findElementAt(offset), KtCallableDeclaration.class);
    }

    @Override
    public String getClassVMName(KtClassOrObject cls) {
        String fqName = cls.getFqName().asString();
        return fqName;
    }

    @Override
    public KtClassOrObject getContainingClass(KtCallableDeclaration method)
    {
        KtClassOrObject parentOfType = PsiTreeUtil.getParentOfType(method, KtClassOrObject.class);
        return parentOfType;
    }

    @Override
    public boolean matchesSignature(KtCallableDeclaration method, String memberName, List<String> paramTypeNames, String returnTypeName)
    {
        String name = method.getName();
        if (!memberName.equals(name))
        {
            return false;
        }

        if (paramTypeNames.size() != method.getValueParameters().size())
        {
            return false;
        }

        List<String> methodParamTypes = method.getValueParameters().stream()
                .map(param -> jvmText(param.getTypeReference()))
                .toList();

        for (int i = 0; i < paramTypeNames.size(); i++)
        {
            if (!paramTypeNames.get(i).equals(methodParamTypes.get(i)))
            {
                return false;
            }
        }

        String methodReturnType = jvmText(method.getTypeReference());
        return returnTypeName.equals(methodReturnType);
    }

    private String jvmText(KtTypeReference typeReference)
    {
        if (typeReference == null)
        {
            return Void.class.getName();
        }
        return typeReference.getText();
    }

}
